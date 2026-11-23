package br.com.fincontrol.infrastructure.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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

import br.com.fincontrol.application.FechamentosMes;
import br.com.fincontrol.application.LancamentosConsulta;
import br.com.fincontrol.application.LancamentosEscrita;
import br.com.fincontrol.domain.model.Acesso;
import br.com.fincontrol.domain.model.Dinheiro;
import br.com.fincontrol.domain.model.FechamentoMes;
import br.com.fincontrol.domain.model.Lancamento;
import br.com.fincontrol.domain.model.Natureza;
import br.com.fincontrol.domain.model.Papel;
import br.com.fincontrol.domain.model.SaldoConta;
import br.com.fincontrol.domain.model.TipoDespesa;
import br.com.fincontrol.domain.service.AcessoNegadoException;
import br.com.fincontrol.domain.service.EstornoRepetidoException;
import br.com.fincontrol.domain.service.IdempotenciaConflitoException;
import br.com.fincontrol.domain.service.LancamentoEstornadoException;
import br.com.fincontrol.domain.service.MesFechadoException;
import br.com.fincontrol.domain.service.NaoEncontradoException;
import br.com.fincontrol.domain.service.ParcelaInvalidaException;

@ExtendWith(MockitoExtension.class)
class LancamentosWebTest {

    private static final Instant AGORA = Instant.parse("2026-11-22T03:00:00Z");

    @Mock private LancamentosConsulta consulta;
    @Mock private LancamentosEscrita escrita;
    @Mock private FechamentosMes fechamentos;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        Acesso acesso = new Acesso("7", Papel.ADMIN, 42L, "jti", AGORA);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(acesso, null));
        mvc = MockMvcBuilders.standaloneSetup(new LancamentoController(consulta, escrita), new MesController(fechamentos))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listaCriaEObtem() throws Exception {
        when(consulta.listar(any(), any())).thenReturn(List.of(lancamento(1L, null, null)));
        when(consulta.obter(any(), eq(1L))).thenReturn(lancamento(1L, null, null));
        when(escrita.criar(any(), eq("k-1"), any())).thenReturn(List.of(
                lancamento(11L, 9L, "grupo"), lancamento(12L, 9L, "grupo")));

        mvc.perform(get("/api/v1/lancamentos").param("contaId", "1").param("de", "2026-11-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].valor").value("100.00"))
                .andExpect(jsonPath("$[0].estornado").value(false));
        mvc.perform(get("/api/v1/lancamentos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.natureza").value("DESPESA"))
                .andExpect(jsonPath("$.tipoDespesa").value("VARIAVEL"));
        mvc.perform(post("/api/v1/lancamentos").header("Idempotency-Key", "k-1")
                .contentType(MediaType.APPLICATION_JSON).content(corpo(2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.grupoParcela").value("grupo"))
                .andExpect(jsonPath("$.parcelas.length()").value(2));
    }

    @Test
    void atualizaEstornaEExclui() throws Exception {
        when(escrita.editar(any(), eq(1L), any())).thenReturn(lancamento(1L, null, null));
        when(escrita.estornar(any(), eq(1L), eq("engano")))
                .thenReturn(lancamento(1L, null, null).estornar("engano", "7", AGORA));

        mvc.perform(put("/api/v1/lancamentos/1").contentType(MediaType.APPLICATION_JSON).content(corpo(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"));
        mvc.perform(post("/api/v1/lancamentos/1/estorno").contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\":\"engano\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estornado").value(true))
                .andExpect(jsonPath("$.estornoMotivo").value("engano"))
                .andExpect(jsonPath("$.estornadoEm").exists());
        mvc.perform(delete("/api/v1/lancamentos/1")).andExpect(status().isNoContent());
    }

    @Test
    void saldoDaConta() throws Exception {
        when(consulta.saldo(any(), eq(1L))).thenReturn(
                new SaldoConta(new BigDecimal("100"), new BigDecimal("50.50"), new BigDecimal("30.25")));

        mvc.perform(get("/api/v1/contas/1/saldo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contaId").value("1"))
                .andExpect(jsonPath("$.saldo").value("120.25"));
    }

    @Test
    void errosDeLancamento() throws Exception {
        when(consulta.obter(any(), eq(2L))).thenThrow(new NaoEncontradoException());
        doAnswer(inv -> {
            String chave = inv.getArgument(1);
            if ("k-2".equals(chave)) {
                throw new IdempotenciaConflitoException();
            }
            if ("k-3".equals(chave)) {
                throw new ParcelaInvalidaException();
            }
            throw new MesFechadoException();
        }).when(escrita).criar(any(), any(), any());
        when(escrita.editar(any(), eq(3L), any())).thenThrow(new LancamentoEstornadoException());
        when(escrita.estornar(any(), eq(4L), any())).thenThrow(new EstornoRepetidoException());
        doThrow(new AcessoNegadoException()).when(escrita).excluir(any(), eq(5L));

        mvc.perform(get("/api/v1/lancamentos/2")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/lancamentos/abc")).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/lancamentos").contentType(MediaType.APPLICATION_JSON).content(corpo(1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigoErro").value("MES_FECHADO"));
        mvc.perform(put("/api/v1/lancamentos/3").contentType(MediaType.APPLICATION_JSON).content(corpo(1)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigoErro").value("LANCAMENTO_ESTORNADO"));
        mvc.perform(post("/api/v1/lancamentos/4/estorno").contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\":\"x\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigoErro").value("ESTORNO_REPETIDO"));
        mvc.perform(post("/api/v1/lancamentos").header("Idempotency-Key", "k-2")
                .contentType(MediaType.APPLICATION_JSON).content(corpo(1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigoErro").value("IDEMPOTENCIA_CONFLITO"));
        mvc.perform(post("/api/v1/lancamentos").header("Idempotency-Key", "k-3")
                .contentType(MediaType.APPLICATION_JSON).content(corpo(1)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigoErro").value("PARCELA_INVALIDA"));
        mvc.perform(delete("/api/v1/lancamentos/5")).andExpect(status().isForbidden());
    }

    @Test
    void errosDeValidacao() throws Exception {
        mvc.perform(post("/api/v1/lancamentos").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigoErro").value("VALIDACAO"));
        mvc.perform(post("/api/v1/lancamentos").contentType(MediaType.APPLICATION_JSON)
                .content(corpo(1).replace("\"100.00\"", "\"abc\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigoErro").value("VALIDACAO"));
        mvc.perform(post("/api/v1/lancamentos/1/estorno").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void mesesFechaReabreELista() throws Exception {
        when(fechamentos.listar(any())).thenReturn(List.of(new FechamentoMes(1L, 42L, 2026, 11, AGORA, "7")));
        when(fechamentos.fechar(any(), eq(2026), eq(11))).thenReturn(new FechamentoMes(1L, 42L, 2026, 11, AGORA, "7"));
        doAnswer(inv -> {
            if (Integer.valueOf(1).equals(inv.getArgument(2))) {
                throw new NaoEncontradoException();
            }
            return null;
        }).when(fechamentos).reabrir(any(), anyInt(), anyInt());

        mvc.perform(get("/api/v1/meses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].mes").value(11))
                .andExpect(jsonPath("$[0].fechadoPor").value("7"));
        mvc.perform(post("/api/v1/meses/2026/11/fechamento"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ano").value(2026));
        mvc.perform(delete("/api/v1/meses/2026/11/fechamento")).andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/meses/2026/1/fechamento")).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/meses/2026/13/fechamento"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigoErro").value("VALIDACAO"));
        mvc.perform(post("/api/v1/meses/abc/11/fechamento")).andExpect(status().isBadRequest());
    }

    @Test
    void semSessaoNega() throws Exception {
        SecurityContextHolder.clearContext();
        mvc.perform(get("/api/v1/lancamentos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/meses")).andExpect(status().isUnauthorized());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("texto", null));
        mvc.perform(get("/api/v1/lancamentos/1")).andExpect(status().isUnauthorized());
    }

    private static String corpo(int parcelas) {
        return """
                {"contaId":"1","categoriaId":"2","natureza":"DESPESA","tipoDespesa":"VARIAVEL",
                 "valor":"100.00","data":"2026-11-10","descricao":"Compra","parcelas":%d}
                """.formatted(parcelas);
    }

    private static Lancamento lancamento(long id, Long parcelaId, String grupo) {
        return new Lancamento(id, 42L, 1L, 2L, Natureza.DESPESA, TipoDespesa.VARIAVEL,
                new Dinheiro(new BigDecimal("100.00")), LocalDate.of(2026, 11, 10), "Compra",
                parcelaId, grupo, parcelaId == null ? null : 1, parcelaId == null ? null : 2, null, null, null);
    }
}
