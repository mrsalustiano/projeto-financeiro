package br.com.fincontrol.infrastructure.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import br.com.fincontrol.application.AutenticarUsuario;
import br.com.fincontrol.application.ConsultarUsuarioAutenticado;
import br.com.fincontrol.application.EncerrarSessao;
import br.com.fincontrol.application.RenovarSessao;
import br.com.fincontrol.application.SessaoEmitida;
import br.com.fincontrol.application.UsuarioVisao;
import br.com.fincontrol.domain.model.Acesso;
import br.com.fincontrol.domain.model.Papel;
import br.com.fincontrol.domain.service.LoginBloqueadoException;
import br.com.fincontrol.domain.service.NaoAutenticadoException;
import br.com.fincontrol.domain.service.RevogacaoRepositorio;
import br.com.fincontrol.domain.service.TokenAcessoPort;
import jakarta.servlet.FilterChain;

@ExtendWith(MockitoExtension.class)
class AuthWebTest {

    @Mock private AutenticarUsuario autenticar;
    @Mock private RenovarSessao renovar;
    @Mock private EncerrarSessao encerrar;
    @Mock private ConsultarUsuarioAutenticado consultar;
    @Mock private TokenAcessoPort tokens;
    @Mock private RevogacaoRepositorio revogacao;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        AuthController controller = new AuthController(autenticar, renovar, encerrar, consultar, new CookieRefresh(true));
        mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new ApiExceptionHandler()).build();
    }

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void loginDevolveTokenECookie() throws Exception {
        when(autenticar.executar("voce@fincontrol.local", "segredo")).thenReturn(new SessaoEmitida("jwt", "opaco"));

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"voce@fincontrol.local\",\"senha\":\"segredo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.refreshToken").value("opaco"))
                .andExpect(header().exists("Set-Cookie"));
    }

    @Test
    void loginBloqueadoENaoAutenticado() throws Exception {
        when(autenticar.executar("voce@fincontrol.local", "errada"))
                .thenThrow(new NaoAutenticadoException())
                .thenThrow(new LoginBloqueadoException());

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"voce@fincontrol.local\",\"senha\":\"errada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigoErro").value("NAO_AUTENTICADO"));
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"voce@fincontrol.local\",\"senha\":\"errada\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.codigoErro").value("LOGIN_BLOQUEADO"));
    }

    @Test
    void refreshLogoutEMe() throws Exception {
        when(renovar.executar("opaco")).thenReturn(new SessaoEmitida("novo", "novo-opaco"));
        when(consultar.executar(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new UsuarioVisao("7", "Voce", "voce@fincontrol.local", Papel.ADMIN, "42"));
        Acesso acesso = new Acesso("7", Papel.ADMIN, 42L, "jti", Instant.parse("2026-11-22T03:15:00Z"));
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(acesso, null, java.util.List.of()));

        mvc.perform(post("/api/v1/auth/refresh_token").cookie(new jakarta.servlet.http.Cookie("refresh_token", "opaco")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("novo"))
                .andExpect(jsonPath("$.refreshToken").value("novo-opaco"));
        mvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isNoContent());
        verify(encerrar).executar(acesso);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clienteId").value("42"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.senha").doesNotExist());
    }

    @Test
    void logoutComBearerResponde204() throws Exception {
        Acesso acesso = new Acesso("7", Papel.ADMIN, 42L, "jti", Instant.parse("2026-11-22T03:15:00Z"));
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(acesso, null, java.util.List.of()));

        mvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));
        verify(encerrar).executar(acesso);
    }

    @Test
    void logoutSemBearerResponde401() throws Exception {
        mvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isUnauthorized());
        verify(encerrar, org.mockito.Mockito.never()).executar(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void refreshComHeaderXRefreshTokenTemPrecedenciaSobreOCookie() throws Exception {
        when(renovar.executar("do-header")).thenReturn(new SessaoEmitida("novo", "novo-opaco"));

        mvc.perform(post("/api/v1/auth/refresh_token")
                        .header("X-Refresh-Token", "do-header")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "do-cookie")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").value("novo-opaco"));
        verify(renovar).executar("do-header");
    }

    @Test
    void meSemAcessoResponde401() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void filtroAceitaRecusaEIgnoraSemBearer() throws Exception {
        FiltroDeAcesso filtro = new FiltroDeAcesso(tokens, revogacao);
        Acesso acesso = new Acesso("7", Papel.ADMIN, 42L, "jti", Instant.now());
        when(tokens.ler("jwt")).thenReturn(Optional.of(acesso));
        when(revogacao.revogado("jti")).thenReturn(false);
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/me");
        request.addHeader("Authorization", "Bearer jwt");

        filtro.doFilter(request, new MockHttpServletResponse(), chain);

        verify(chain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void filtroRecusaTokenRuimESegueSemCabecalho() throws Exception {
        FiltroDeAcesso filtro = new FiltroDeAcesso(tokens, revogacao);
        when(tokens.ler("ruim")).thenReturn(Optional.empty());
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest ruim = new MockHttpServletRequest("GET", "/api/v1/auth/me");
        ruim.addHeader("Authorization", "Bearer ruim");
        MockHttpServletResponse resposta = new MockHttpServletResponse();

        filtro.doFilter(ruim, resposta, chain);
        MockHttpServletRequest login = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        login.addHeader("Authorization", "Bearer ruim");
        filtro.doFilter(login, new MockHttpServletResponse(), chain);
        verify(chain).doFilter(org.mockito.ArgumentMatchers.eq(login), org.mockito.ArgumentMatchers.any());

        assertThat(resposta.getStatus()).isEqualTo(401);
        assertThat(resposta.getContentType()).contains("application/problem+json");
        assertThat(resposta.getContentAsString()).contains("NAO_AUTENTICADO");

        MockHttpServletRequest livre = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        filtro.doFilter(livre, new MockHttpServletResponse(), chain);
        verify(chain).doFilter(org.mockito.ArgumentMatchers.eq(livre), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void pontosDeEntradaEscrevemProblema() throws Exception {
        ProblemaAutenticacao entrada = new ProblemaAutenticacao();
        ProblemaAcesso negado = new ProblemaAcesso();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/me");
        MockHttpServletResponse semToken = new MockHttpServletResponse();
        MockHttpServletResponse semPermissao = new MockHttpServletResponse();

        entrada.commence(request, semToken, new BadCredentialsException("x"));
        negado.handle(request, semPermissao, new AccessDeniedException("x"));

        assertThat(semToken.getStatus()).isEqualTo(401);
        assertThat(semPermissao.getStatus()).isEqualTo(403);
        assertThat(semPermissao.getContentAsString()).contains("ACESSO_NEGADO");
    }

    @Test
    void cookieSeguroEApagavel() {
        String criado = new CookieRefresh(true).criar("opaco").toString();
        String apagado = new CookieRefresh(false).apagar().toString();

        assertThat(criado).contains("HttpOnly").contains("Secure").contains("SameSite=Strict").contains("Path=/api/v1/auth");
        assertThat(apagado).contains("Max-Age=0").doesNotContain("Secure");
    }
}
