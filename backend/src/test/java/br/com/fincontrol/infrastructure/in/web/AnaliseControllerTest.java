package br.com.fincontrol.infrastructure.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import br.com.fincontrol.application.AnalisesFinanceiras;
import br.com.fincontrol.domain.model.Acesso;
import br.com.fincontrol.domain.model.AnaliseFinanceira;
import br.com.fincontrol.domain.model.Papel;
import br.com.fincontrol.domain.service.AcessoNegadoException;
import br.com.fincontrol.domain.service.IaDesabilitadaException;
import br.com.fincontrol.domain.service.IaIndisponivelException;
import br.com.fincontrol.domain.service.IaRespostaInvalidaException;

@ExtendWith(MockitoExtension.class)
class AnaliseControllerTest {

    private static final Instant AGORA = Instant.parse("2026-11-23T12:00:00Z");
    private static final String CORPO = """
            {"mes":"2026-11","tipo":"PRESUNTIVA","pergunta":"onde cortar?"}
            """;

    @Mock private AnalisesFinanceiras casos;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        Acesso admin = new Acesso("7", Papel.ADMIN, 42L, "jti", AGORA);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(admin, null));
        mvc = MockMvcBuilders.standaloneSetup(new AnaliseController(casos))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void devolveAnaliseValidada() throws Exception {
        when(casos.analisar(any(), any())).thenReturn(new AnaliseFinanceira("Resumo", List.of("i1"), List.of("a1")));
        when(casos.provedor()).thenReturn("ollama");
        when(casos.modelo()).thenReturn("qwen3.5:9b");

        mvc.perform(post("/api/v1/analise").contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mes").value("2026-11"))
                .andExpect(jsonPath("$.tipo").value("PRESUNTIVA"))
                .andExpect(jsonPath("$.provedor").value("ollama"))
                .andExpect(jsonPath("$.modelo").value("qwen3.5:9b"))
                .andExpect(jsonPath("$.resumo").value("Resumo"))
                .andExpect(jsonPath("$.insights[0]").value("i1"))
                .andExpect(jsonPath("$.acoes[0]").value("a1"));
    }

    @Test
    void recusaMesInvalido() throws Exception {
        mvc.perform(post("/api/v1/analise").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mes\":\"2026-13\",\"tipo\":\"PRESUNTIVA\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigoErro").value("VALIDACAO"));
        mvc.perform(post("/api/v1/analise").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mes\":\"novembro\",\"tipo\":\"PRESUNTIVA\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigoErro").value("VALIDACAO"));
    }

    @Test
    void recusaTipoInvalido() throws Exception {
        mvc.perform(post("/api/v1/analise").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mes\":\"2026-11\",\"tipo\":\"OUTRA\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void responde503QuandoDesabilitada() throws Exception {
        when(casos.analisar(any(), any())).thenThrow(new IaDesabilitadaException());

        mvc.perform(post("/api/v1/analise").contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.codigoErro").value("IA_DESABILITADA"));
    }

    @Test
    void responde503QuandoProvedorFora() throws Exception {
        when(casos.analisar(any(), any())).thenThrow(new IaIndisponivelException(new IllegalStateException("fora")));

        mvc.perform(post("/api/v1/analise").contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.codigoErro").value("IA_INDISPONIVEL"));
    }

    @Test
    void responde422QuandoSchemaInvalido() throws Exception {
        when(casos.analisar(any(), any())).thenThrow(new IaRespostaInvalidaException());

        mvc.perform(post("/api/v1/analise").contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigoErro").value("IA_RESPOSTA_INVALIDA"));
    }

    @Test
    void responde403ParaMaster() throws Exception {
        when(casos.analisar(any(), any())).thenThrow(new AcessoNegadoException());

        mvc.perform(post("/api/v1/analise").contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigoErro").value("ACESSO_NEGADO"));
    }

    @Test
    void responde401SemSessao() throws Exception {
        SecurityContextHolder.clearContext();

        mvc.perform(post("/api/v1/analise").contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isUnauthorized());
    }
}
