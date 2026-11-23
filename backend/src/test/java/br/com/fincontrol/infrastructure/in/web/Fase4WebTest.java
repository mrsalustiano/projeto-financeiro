package br.com.fincontrol.infrastructure.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
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

import br.com.fincontrol.application.MetaComProgresso;
import br.com.fincontrol.application.Metas;
import br.com.fincontrol.application.OrcamentoComAlerta;
import br.com.fincontrol.application.Orcamentos;
import br.com.fincontrol.application.Recorrencias;
import br.com.fincontrol.application.Transferencias;
import br.com.fincontrol.domain.model.Acesso;
import br.com.fincontrol.domain.model.Aporte;
import br.com.fincontrol.domain.model.DiaDoMes;
import br.com.fincontrol.domain.model.Dinheiro;
import br.com.fincontrol.domain.model.Meta;
import br.com.fincontrol.domain.model.Natureza;
import br.com.fincontrol.domain.model.Orcamento;
import br.com.fincontrol.domain.model.Papel;
import br.com.fincontrol.domain.model.Recorrencia;
import br.com.fincontrol.domain.model.Transferencia;
import br.com.fincontrol.domain.service.NaoEncontradoException;
import br.com.fincontrol.domain.service.NomeEmUsoException;
import br.com.fincontrol.domain.service.RecorrenciaInativaException;
import br.com.fincontrol.domain.service.TransferenciaInvalidaException;

@ExtendWith(MockitoExtension.class)
class Fase4WebTest {

    private static final Instant AGORA = Instant.parse("2026-11-22T03:00:00Z");

    @Mock private Transferencias transferencias;
    @Mock private Orcamentos orcamentos;
    @Mock private Metas metas;
    @Mock private Recorrencias recorrencias;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        Acesso acesso = new Acesso("7", Papel.ADMIN, 42L, "jti", AGORA);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(acesso, null));
        mvc = MockMvcBuilders.standaloneSetup(
                        new TransferenciaController(transferencias),
                        new OrcamentoController(orcamentos),
                        new MetaController(metas),
                        new RecorrenciaController(recorrencias))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void transferenciasListaECria() throws Exception {
        when(transferencias.listar(any())).thenReturn(List.of(transferencia()));
        when(transferencias.criar(any(), any())).thenReturn(transferencia());

        mvc.perform(get("/api/v1/transferencias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].valor").value("100.00"))
                .andExpect(jsonPath("$[0].descricao").value("Reserva"));
        mvc.perform(post("/api/v1/transferencias").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contaOrigemId\":\"1\",\"contaDestinoId\":\"2\",\"valor\":\"100.00\","
                                + "\"data\":\"2026-11-10\",\"descricao\":\"Reserva\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contaDestinoId").value("2"));
    }

    @Test
    void transferenciaInvalida() throws Exception {
        when(transferencias.criar(any(), any())).thenThrow(new TransferenciaInvalidaException());
        mvc.perform(post("/api/v1/transferencias").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contaOrigemId\":\"1\",\"contaDestinoId\":\"1\",\"valor\":\"100.00\","
                                + "\"data\":\"2026-11-10\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigoErro").value("TRANSFERENCIA_INVALIDA"));
        mvc.perform(post("/api/v1/transferencias").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void orcamentosListaDefineEExclui() throws Exception {
        when(orcamentos.listar(any(), eq(2026), eq(11))).thenReturn(List.of(orcamentoComAlerta()));
        when(orcamentos.definir(any(), eq(3L), eq(2026), eq(11), any())).thenReturn(orcamentoComAlerta());
        doAnswer(inv -> {
            if (Long.valueOf(9L).equals(inv.getArgument(1))) {
                throw new NaoEncontradoException();
            }
            return null;
        }).when(orcamentos).excluir(any(), anyLong(), anyInt(), anyInt());

        mvc.perform(get("/api/v1/orcamentos").param("ano", "2026").param("mes", "11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].percentual").value("85.00"))
                .andExpect(jsonPath("$[0].alerta").value(true))
                .andExpect(jsonPath("$[0].estourou").value(false));
        mvc.perform(put("/api/v1/orcamentos/2026/11/3").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"limite\":\"1000.00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limite").value("1000.00"));
        mvc.perform(delete("/api/v1/orcamentos/2026/11/3")).andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/orcamentos/2026/11/9")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/orcamentos").param("ano", "2026").param("mes", "13"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void metasListaCriaAportaEObtem() throws Exception {
        when(metas.listar(any())).thenReturn(List.of(metaComProgresso()));
        when(metas.criar(any(), any(), any(), any())).thenReturn(metaComProgresso());
        when(metas.obter(any(), eq(7L))).thenReturn(metaComProgresso());
        when(metas.listarAportes(any(), eq(7L))).thenReturn(List.of(aporte()));
        when(metas.aportar(any(), eq(7L), any(), any())).thenReturn(aporte());
        when(metas.criar(any(), eq("Repetida"), any(), any())).thenThrow(new NomeEmUsoException());

        mvc.perform(get("/api/v1/metas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].percentual").value("25.00"))
                .andExpect(jsonPath("$[0].alcancada").value(false));
        mvc.perform(post("/api/v1/metas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Viagem\",\"valorAlvo\":\"1000.00\",\"dataAlvo\":\"2027-01-01\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Viagem"));
        mvc.perform(post("/api/v1/metas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Repetida\",\"valorAlvo\":\"1000.00\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigoErro").value("NOME_EM_USO"));
        mvc.perform(get("/api/v1/metas/7")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/metas/7/aportes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].valor").value("250.00"));
        mvc.perform(post("/api/v1/metas/7/aportes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valor\":\"250.00\",\"data\":\"2026-11-10\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.metaId").value("7"));
    }

    @Test
    void recorrenciasListaCriaPausaERetoma() throws Exception {
        when(recorrencias.listar(any())).thenReturn(List.of(recorrencia()));
        when(recorrencias.criar(any(), any())).thenReturn(recorrencia());
        when(recorrencias.pausar(any(), eq(4L))).thenReturn(recorrencia().pausar(AGORA));
        when(recorrencias.retomar(any(), eq(4L))).thenReturn(recorrencia());
        when(recorrencias.pausar(any(), eq(5L))).thenThrow(new RecorrenciaInativaException());

        mvc.perform(get("/api/v1/recorrencias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].diaDoMes").value(0))
                .andExpect(jsonPath("$[0].ativa").value(true));
        mvc.perform(post("/api/v1/recorrencias").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contaId\":\"2\",\"categoriaId\":\"3\",\"natureza\":\"RECEITA\","
                                + "\"valor\":\"1570.00\",\"descricao\":\"Benefício Flash\",\"diaDoMes\":0}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.valor").value("1570.00"));
        mvc.perform(post("/api/v1/recorrencias/4/pausa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativa").value(false))
                .andExpect(jsonPath("$.pausadaEm").exists());
        mvc.perform(post("/api/v1/recorrencias/4/retomada"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativa").value(true));
        mvc.perform(post("/api/v1/recorrencias/5/pausa"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigoErro").value("RECORRENCIA_INATIVA"));
    }

    @Test
    void semSessaoNega() throws Exception {
        SecurityContextHolder.clearContext();
        mvc.perform(get("/api/v1/transferencias")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/metas")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/recorrencias")).andExpect(status().isUnauthorized());
    }

    private static Transferencia transferencia() {
        return new Transferencia(1L, 42L, 1L, 2L, new Dinheiro(new BigDecimal("100.00")),
                LocalDate.of(2026, 11, 10), "Reserva");
    }

    private static OrcamentoComAlerta orcamentoComAlerta() {
        Orcamento orcamento = new Orcamento(5L, 42L, 3L, 2026, 11, new Dinheiro(new BigDecimal("1000.00")));
        return new OrcamentoComAlerta(orcamento, orcamento.alertar(new BigDecimal("850.00")));
    }

    private static MetaComProgresso metaComProgresso() {
        Meta meta = new Meta(7L, 42L, "Viagem", new Dinheiro(new BigDecimal("1000.00")),
                LocalDate.of(2027, 1, 1), AGORA);
        return new MetaComProgresso(meta, meta.progresso(new BigDecimal("250.00")));
    }

    private static Aporte aporte() {
        return new Aporte(11L, 42L, 7L, new Dinheiro(new BigDecimal("250.00")), LocalDate.of(2026, 11, 10));
    }

    private static Recorrencia recorrencia() {
        return new Recorrencia(4L, 42L, 2L, 3L, Natureza.RECEITA, null,
                new Dinheiro(new BigDecimal("1570.00")), "Benefício Flash", DiaDoMes.ultimo(), true, null, AGORA);
    }
}
