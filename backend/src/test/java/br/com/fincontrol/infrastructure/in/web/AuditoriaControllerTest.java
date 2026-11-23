package br.com.fincontrol.infrastructure.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import br.com.fincontrol.application.Auditorias;
import br.com.fincontrol.domain.model.Acesso;
import br.com.fincontrol.domain.model.AcaoAuditoria;
import br.com.fincontrol.domain.model.PaginaAuditoria;
import br.com.fincontrol.domain.model.Papel;
import br.com.fincontrol.domain.model.RegistroAuditoria;
import br.com.fincontrol.domain.service.AcessoNegadoException;

@ExtendWith(MockitoExtension.class)
class AuditoriaControllerTest {

    private static final Instant AGORA = Instant.parse("2026-11-23T12:00:00Z");

    @Mock private Auditorias auditorias;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        Acesso master = new Acesso("1", Papel.MASTER, null, "jti", AGORA);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(master, null));
        mvc = MockMvcBuilders.standaloneSetup(new AuditoriaController(auditorias))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listaAuditoriaComCursor() throws Exception {
        RegistroAuditoria registro = new RegistroAuditoria(7L, 42L, 3L, "voce@fincontrol.local", "ADMIN",
                AcaoAuditoria.CREATE, "lancamento", 44L, null, "{\"id\":44}", AGORA);
        when(auditorias.consultar(any(), any(), isNull(), eq(20)))
                .thenReturn(new PaginaAuditoria(List.of(registro), 7L));

        mvc.perform(get("/api/v1/master/auditoria"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].id").value("7"))
                .andExpect(jsonPath("$.itens[0].clienteId").value("42"))
                .andExpect(jsonPath("$.itens[0].usuarioId").value("3"))
                .andExpect(jsonPath("$.itens[0].usuarioEmail").value("voce@fincontrol.local"))
                .andExpect(jsonPath("$.itens[0].usuarioPerfil").value("ADMIN"))
                .andExpect(jsonPath("$.itens[0].acao").value("CREATE"))
                .andExpect(jsonPath("$.itens[0].recurso").value("lancamento"))
                .andExpect(jsonPath("$.itens[0].recursoId").value("44"))
                .andExpect(jsonPath("$.itens[0].dadosDepois").value("{\"id\":44}"))
                .andExpect(jsonPath("$.proximoCursor").value("7"));
    }

    @Test
    void recusaFiltroInvalido() throws Exception {
        when(auditorias.consultar(any(), any(), eq("x"), any()))
                .thenThrow(new IllegalArgumentException("Cursor inválido."));

        mvc.perform(get("/api/v1/master/auditoria").param("clienteId", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigoErro").value("VALIDACAO"));
        mvc.perform(get("/api/v1/master/auditoria").param("acao", "APAGAR"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigoErro").value("VALIDACAO"));
        mvc.perform(get("/api/v1/master/auditoria").param("cursor", "x"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigoErro").value("VALIDACAO"));
    }

    @Test
    void recusaNaoMaster() throws Exception {
        when(auditorias.consultar(any(), any(), any(), any())).thenThrow(new AcessoNegadoException());
        Acesso admin = new Acesso("7", Papel.ADMIN, 42L, "jti", AGORA);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(admin, null));

        mvc.perform(get("/api/v1/master/auditoria"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigoErro").value("ACESSO_NEGADO"));
    }

    @Test
    void semSessaoNega() throws Exception {
        SecurityContextHolder.clearContext();

        mvc.perform(get("/api/v1/master/auditoria")).andExpect(status().isUnauthorized());
    }

    @Test
    void filtroComAcaoValida() throws Exception {
        when(auditorias.consultar(any(), any(), eq("9"), eq(50)))
                .thenReturn(PaginaAuditoria.vazia());

        mvc.perform(get("/api/v1/master/auditoria")
                        .param("acao", "delete")
                        .param("cursor", "9")
                        .param("limite", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens").isEmpty())
                .andExpect(jsonPath("$.proximoCursor").doesNotExist());
    }
}
