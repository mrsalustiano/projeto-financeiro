package br.com.fincontrol.infrastructure.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import br.com.fincontrol.application.ImportacaoOfx;
import br.com.fincontrol.application.ResultadoImportacao;
import br.com.fincontrol.application.SugestaoOfx;
import br.com.fincontrol.domain.model.Acesso;
import br.com.fincontrol.domain.model.Conta;
import br.com.fincontrol.domain.model.Papel;
import br.com.fincontrol.domain.model.Saldo;
import br.com.fincontrol.domain.model.TipoConta;
import br.com.fincontrol.domain.service.AcessoNegadoException;
import br.com.fincontrol.domain.service.ArquivoNaoOfxException;
import br.com.fincontrol.domain.service.NaoEncontradoException;
import br.com.fincontrol.domain.service.OfxContaDivergenteException;
import br.com.fincontrol.domain.service.OfxSemTransacaoException;

@ExtendWith(MockitoExtension.class)
class OfxWebTest {

    private static final Instant AGORA = Instant.parse("2026-11-22T03:00:00Z");

    @Mock private ImportacaoOfx importacao;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        Acesso acesso = new Acesso("7", Papel.ADMIN, 42L, "jti", AGORA);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(acesso, null));
        mvc = MockMvcBuilders.standaloneSetup(new OfxController(importacao))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void importaEDevolveContagem() throws Exception {
        when(importacao.importar(any(), eq(1L), any())).thenReturn(new ResultadoImportacao(
                38, 0, 37, List.of(), new BigDecimal("1234.56"), "BRL", "Nubank", 1L, "7224833363",
                new BigDecimal("12.34")));

        mvc.perform(multipart("/api/v1/ofx/importacoes").file(arquivo()).param("contaId", "1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.importados").value(38))
                .andExpect(jsonPath("$.duplicados").value(0))
                .andExpect(jsonPath("$.semCategoria").value(37))
                .andExpect(jsonPath("$.saldoInformado").value("1234.56"))
                .andExpect(jsonPath("$.rendimento").value("12.34"))
                .andExpect(jsonPath("$.banco").value("Nubank"))
                .andExpect(jsonPath("$.contaId").value("1"))
                .andExpect(jsonPath("$.identificadorConta").value("7224833363"));
    }

    @Test
    void sugereContaDoMesmoBanco() throws Exception {
        when(importacao.sugerir(any(), any())).thenReturn(new SugestaoOfx(
                "Nubank", "7224833363", "BRL", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 22), 38,
                List.of(new Conta(1L, 42L, "Nubank", "Nubank", TipoConta.CORRENTE, Saldo.zero(),
                        LocalDate.of(2026, 1, 1), null))));

        mvc.perform(multipart("/api/v1/ofx/sugestoes").file(arquivo()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.banco").value("Nubank"))
                .andExpect(jsonPath("$.movimentos").value(38))
                .andExpect(jsonPath("$.contas[0].id").value("1"))
                .andExpect(jsonPath("$.contas[0].tipo").value("CORRENTE"));
    }

    @Test
    void errosDeImportacao() throws Exception {
        when(importacao.importar(any(), eq(1L), any())).thenThrow(new ArquivoNaoOfxException());
        when(importacao.importar(any(), eq(2L), any())).thenThrow(new OfxSemTransacaoException());
        when(importacao.importar(any(), eq(3L), any())).thenThrow(new OfxContaDivergenteException());
        when(importacao.importar(any(), eq(4L), any())).thenThrow(new NaoEncontradoException());
        when(importacao.importar(any(), eq(5L), any())).thenThrow(new AcessoNegadoException());

        mvc.perform(multipart("/api/v1/ofx/importacoes").file(arquivo()).param("contaId", "1"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigoErro").value("OFX_INVALIDO"));
        mvc.perform(multipart("/api/v1/ofx/importacoes").file(arquivo()).param("contaId", "2"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigoErro").value("OFX_SEM_TRANSACAO"));
        mvc.perform(multipart("/api/v1/ofx/importacoes").file(arquivo()).param("contaId", "3"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigoErro").value("OFX_CONTA_DIVERGENTE"));
        mvc.perform(multipart("/api/v1/ofx/importacoes").file(arquivo()).param("contaId", "4"))
                .andExpect(status().isNotFound());
        mvc.perform(multipart("/api/v1/ofx/importacoes").file(arquivo()).param("contaId", "5"))
                .andExpect(status().isForbidden());
        mvc.perform(multipart("/api/v1/ofx/importacoes").file(arquivo()).param("contaId", "abc"))
                .andExpect(status().isNotFound());
    }

    @Test
    void arquivoAusenteRespondeQuatrocentos() throws Exception {
        mvc.perform(multipart("/api/v1/ofx/importacoes").param("contaId", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigoErro").value("VALIDACAO"));
        mvc.perform(multipart("/api/v1/ofx/sugestoes")
                .file(new MockMultipartFile("arquivo", "vazio.ofx", "application/octet-stream", new byte[0])))
                .andExpect(status().isBadRequest());
    }

    @Test
    void semSessaoNega() throws Exception {
        SecurityContextHolder.clearContext();

        mvc.perform(multipart("/api/v1/ofx/importacoes").file(arquivo()).param("contaId", "1"))
                .andExpect(status().isUnauthorized());
        mvc.perform(multipart("/api/v1/ofx/sugestoes").file(arquivo()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sugestaoComErroDeOfx() throws Exception {
        when(importacao.sugerir(any(), any())).thenThrow(new ArquivoNaoOfxException());

        mvc.perform(multipart("/api/v1/ofx/sugestoes").file(arquivo()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigoErro").value("OFX_INVALIDO"));
    }

    private static MockMultipartFile arquivo() {
        return new MockMultipartFile("arquivo", "extrato.ofx", "application/octet-stream",
                "<OFX></OFX>".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
