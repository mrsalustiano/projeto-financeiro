package br.com.fincontrol.infrastructure.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import br.com.fincontrol.application.ComprovanteBaixado;
import br.com.fincontrol.application.Comprovantes;
import br.com.fincontrol.application.Conciliacoes;
import br.com.fincontrol.domain.model.Acesso;
import br.com.fincontrol.domain.model.Comprovante;
import br.com.fincontrol.domain.model.Conciliacao;
import br.com.fincontrol.domain.model.Dinheiro;
import br.com.fincontrol.domain.model.Lancamento;
import br.com.fincontrol.domain.model.Natureza;
import br.com.fincontrol.domain.model.Papel;
import br.com.fincontrol.domain.model.TipoComprovante;
import br.com.fincontrol.domain.model.TipoDespesa;
import br.com.fincontrol.domain.service.ComprovanteGrandeException;
import br.com.fincontrol.domain.service.ComprovanteTipoInvalidoException;
import br.com.fincontrol.domain.service.ConciliacaoInvalidaException;
import br.com.fincontrol.domain.service.ConciliacaoRepetidaException;
import br.com.fincontrol.domain.service.MesFechadoException;
import br.com.fincontrol.domain.service.NaoEncontradoException;

@ExtendWith(MockitoExtension.class)
class ConciliacaoWebTest {

    private static final Instant AGORA = Instant.parse("2026-11-23T12:00:00Z");

    @Mock private Conciliacoes conciliacoes;
    @Mock private Comprovantes comprovantes;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        Acesso acesso = new Acesso("7", Papel.ADMIN, 42L, "jti", AGORA);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(acesso, null));
        mvc = MockMvcBuilders.standaloneSetup(new ConciliacaoController(conciliacoes),
                        new ComprovanteController(comprovantes))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void sugereCandidatos() throws Exception {
        when(conciliacoes.sugerir(any(), eq(1L))).thenReturn(List.of(lancamento(2L, "FIT-1")));

        mvc.perform(get("/api/v1/conciliacoes/sugestoes").param("lancamentoId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("2"))
                .andExpect(jsonPath("$[0].valor").value("100.00"))
                .andExpect(jsonPath("$[0].natureza").value("DESPESA"))
                .andExpect(jsonPath("$[0].fitid").value("FIT-1"))
                .andExpect(jsonPath("$[0].data").value("2026-11-10"));
    }

    @Test
    void conciliaEDevolveCriado() throws Exception {
        when(conciliacoes.conciliar(any(), eq(1L), eq(2L)))
                .thenReturn(new Conciliacao(5L, 42L, 10L, 1L, 2L, AGORA, "7"));

        mvc.perform(post("/api/v1/conciliacoes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lancamentoId\":\"1\",\"extratoId\":\"2\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("5"))
                .andExpect(jsonPath("$.lancamentoId").value("1"))
                .andExpect(jsonPath("$.extratoId").value("2"))
                .andExpect(jsonPath("$.conciliadoPor").value("7"));
    }

    @Test
    void errosDeConciliacao() throws Exception {
        when(conciliacoes.conciliar(any(), eq(1L), eq(2L))).thenThrow(new ConciliacaoRepetidaException());
        when(conciliacoes.conciliar(any(), eq(1L), eq(3L))).thenThrow(new ConciliacaoInvalidaException());
        when(conciliacoes.conciliar(any(), eq(1L), eq(4L))).thenThrow(new MesFechadoException());
        when(conciliacoes.conciliar(any(), eq(1L), eq(5L))).thenThrow(new NaoEncontradoException());

        mvc.perform(post("/api/v1/conciliacoes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lancamentoId\":\"1\",\"extratoId\":\"2\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigoErro").value("CONCILIACAO_REPETIDA"));
        mvc.perform(post("/api/v1/conciliacoes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lancamentoId\":\"1\",\"extratoId\":\"3\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigoErro").value("CONCILIACAO_INVALIDA"));
        mvc.perform(post("/api/v1/conciliacoes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lancamentoId\":\"1\",\"extratoId\":\"4\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigoErro").value("MES_FECHADO"));
        mvc.perform(post("/api/v1/conciliacoes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lancamentoId\":\"1\",\"extratoId\":\"5\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void desfazConciliacao() throws Exception {
        mvc.perform(delete("/api/v1/conciliacoes/5")).andExpect(status().isNoContent());

        doThrow(new NaoEncontradoException()).when(conciliacoes).desfazer(any(), eq(6L));
        mvc.perform(delete("/api/v1/conciliacoes/6")).andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/conciliacoes/abc")).andExpect(status().isNotFound());
    }

    @Test
    void anexaComprovante() throws Exception {
        when(comprovantes.anexar(any(), eq(1L), eq("recibo.png"), any()))
                .thenReturn(new Comprovante(3L, 42L, 1L, "recibo.png", TipoComprovante.PNG, 4L, "chave", AGORA, "7"));

        mvc.perform(multipart("/api/v1/lancamentos/1/comprovantes").file(arquivo()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("3"))
                .andExpect(jsonPath("$.tipo").value("PNG"))
                .andExpect(jsonPath("$.tamanho").value(4))
                .andExpect(jsonPath("$.nomeArquivo").value("recibo.png"));
    }

    @Test
    void errosDeAnexo() throws Exception {
        when(comprovantes.anexar(any(), eq(1L), any(), any())).thenThrow(new ComprovanteGrandeException());
        when(comprovantes.anexar(any(), eq(2L), any(), any())).thenThrow(new ComprovanteTipoInvalidoException());
        when(comprovantes.anexar(any(), eq(3L), any(), any())).thenThrow(new MesFechadoException());
        when(comprovantes.anexar(any(), eq(4L), any(), any())).thenThrow(new NaoEncontradoException());

        mvc.perform(multipart("/api/v1/lancamentos/1/comprovantes").file(arquivo()))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.codigoErro").value("COMPROVANTE_GRANDE"));
        mvc.perform(multipart("/api/v1/lancamentos/2/comprovantes").file(arquivo()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigoErro").value("COMPROVANTE_TIPO_INVALIDO"));
        mvc.perform(multipart("/api/v1/lancamentos/3/comprovantes").file(arquivo()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigoErro").value("MES_FECHADO"));
        mvc.perform(multipart("/api/v1/lancamentos/4/comprovantes").file(arquivo()))
                .andExpect(status().isNotFound());
    }

    @Test
    void anexoSemArquivoRespondeQuatrocentos() throws Exception {
        mvc.perform(multipart("/api/v1/lancamentos/1/comprovantes"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigoErro").value("VALIDACAO"));
    }

    @Test
    void listaEBaixaComprovante() throws Exception {
        Comprovante comprovante = new Comprovante(3L, 42L, 1L, "recibo.png", TipoComprovante.PNG, 4L, "chave", AGORA,
                "7");
        when(comprovantes.listar(any(), eq(1L))).thenReturn(List.of(comprovante));
        when(comprovantes.baixar(any(), eq(3L)))
                .thenReturn(new ComprovanteBaixado(comprovante, new byte[] {(byte) 0x89, 'P', 'N', 'G'}));

        mvc.perform(get("/api/v1/lancamentos/1/comprovantes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("3"))
                .andExpect(jsonPath("$[0].tipo").value("PNG"));
        mvc.perform(get("/api/v1/comprovantes/3"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"recibo.png\""))
                .andExpect(content().bytes(new byte[] {(byte) 0x89, 'P', 'N', 'G'}));
    }

    @Test
    void excluiComprovante() throws Exception {
        mvc.perform(delete("/api/v1/comprovantes/3")).andExpect(status().isNoContent());

        doThrow(new NaoEncontradoException()).when(comprovantes).remover(any(), eq(4L));
        mvc.perform(delete("/api/v1/comprovantes/4")).andExpect(status().isNotFound());
    }

    @Test
    void semSessaoNega() throws Exception {
        SecurityContextHolder.clearContext();

        mvc.perform(get("/api/v1/conciliacoes/sugestoes").param("lancamentoId", "1"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/conciliacoes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lancamentoId\":\"1\",\"extratoId\":\"2\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/comprovantes/3")).andExpect(status().isUnauthorized());
    }

    private static MockMultipartFile arquivo() {
        return new MockMultipartFile("arquivo", "recibo.png", "image/png",
                new byte[] {(byte) 0x89, 'P', 'N', 'G'});
    }

    private static Lancamento lancamento(long id, String fitid) {
        return new Lancamento(id, 42L, 10L, null, Natureza.DESPESA, TipoDespesa.VARIAVEL, new Dinheiro(new BigDecimal("100.00")),
                LocalDate.of(2026, 11, 10), "Compra", null, null, null, null, null, null, null, fitid);
    }
}
