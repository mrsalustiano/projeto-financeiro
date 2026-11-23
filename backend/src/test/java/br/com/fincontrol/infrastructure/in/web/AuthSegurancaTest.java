package br.com.fincontrol.infrastructure.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import br.com.fincontrol.application.AutenticarUsuario;
import br.com.fincontrol.application.ConsultarUsuarioAutenticado;
import br.com.fincontrol.application.EncerrarSessao;
import br.com.fincontrol.application.RenovarSessao;
import br.com.fincontrol.domain.model.Acesso;
import br.com.fincontrol.domain.model.Papel;
import br.com.fincontrol.domain.service.NaoAutenticadoException;
import br.com.fincontrol.domain.service.RevogacaoRepositorio;
import br.com.fincontrol.domain.service.TokenAcessoPort;
import jakarta.servlet.http.Cookie;

class AuthSegurancaTest {

    private final AutenticarUsuario autenticar = mock(AutenticarUsuario.class);
    private final RenovarSessao renovar = mock(RenovarSessao.class);
    private final EncerrarSessao encerrar = mock(EncerrarSessao.class);
    private final ConsultarUsuarioAutenticado consultar = mock(ConsultarUsuarioAutenticado.class);
    private final TokenAcessoPort tokens = mock(TokenAcessoPort.class);
    private final RevogacaoRepositorio revogacao = mock(RevogacaoRepositorio.class);

    private MockMvc mvc;

    @BeforeEach
    void setUp() throws Exception {
        ObjectPostProcessor<Object> postProcessor = ObjectPostProcessor.identity();
        AnnotationConfigApplicationContext contexto = new AnnotationConfigApplicationContext();
        contexto.refresh();
        Map<Class<?>, Object> compartilhados = new HashMap<>();
        compartilhados.put(ApplicationContext.class, contexto);
        HttpSecurity http = new HttpSecurity(
                postProcessor, new AuthenticationManagerBuilder(postProcessor), compartilhados);
        SecurityFilterChain cadeia = new SegurancaHttp().cadeia(
                http,
                new FiltroDeAcesso(tokens, revogacao),
                new ProblemaAutenticacao(),
                new ProblemaAcesso());
        AuthController controller = new AuthController(autenticar, renovar, encerrar, consultar, new CookieRefresh(true));
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .addFilters(new FilterChainProxy(cadeia))
                .build();
    }

    @Test
    void logoutSoComCookieResponde401() throws Exception {
        mvc.perform(post("/api/v1/auth/logout").cookie(new Cookie("refresh_token", "opaco")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigoErro").value("NAO_AUTENTICADO"));

        verify(encerrar, never()).executar(any());
    }

    @Test
    void logoutSemBearerResponde401() throws Exception {
        mvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigoErro").value("NAO_AUTENTICADO"));

        verify(encerrar, never()).executar(any());
    }

    @Test
    void logoutComBearerInvalidoResponde401AntesDoController() throws Exception {
        when(tokens.ler("ruim")).thenReturn(Optional.empty());

        mvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer ruim")
                        .cookie(new Cookie("refresh_token", "opaco")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigoErro").value("NAO_AUTENTICADO"));

        verify(encerrar, never()).executar(any());
    }

    @Test
    void logoutComBearerValidoChegaAoControllerComAAcesso() throws Exception {
        Acesso acesso = new Acesso("7", Papel.ADMIN, 42L, "jti", Instant.parse("2026-11-22T03:15:00Z"));
        when(tokens.ler("jwt")).thenReturn(Optional.of(acesso));
        when(revogacao.revogado("jti")).thenReturn(false);

        mvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer jwt")
                        .cookie(new Cookie("refresh_token", "opaco")))
                .andExpect(status().isNoContent());

        verify(encerrar).executar(acesso);
    }
}
