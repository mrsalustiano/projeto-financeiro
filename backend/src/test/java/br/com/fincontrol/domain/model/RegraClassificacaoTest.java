package br.com.fincontrol.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class RegraClassificacaoTest {

    private static final RegraClassificacao SALARIO_DIA_15 = new RegraClassificacao(
            1L, 42L, "ITAU", 15, 15, new BigDecimal("4800.00"), new BigDecimal("4800.00"), "Salário");

    private static final RegraClassificacao SALARIO_VIRADA = new RegraClassificacao(
            2L, 42L, "ITAU", 28, 2, new BigDecimal("3750.00"), new BigDecimal("3850.00"), "Salário");

    @Test
    void classificaCreditoDoItauNoDiaQuinze() {
        assertThat(SALARIO_DIA_15.classifica(movimento("2026-10-15", "4800.00", "PIX RECEBIDO ITAU UNIBANCO")))
                .isTrue();
    }

    @Test
    void naoClassificaValorOuDiaDiferente() {
        assertThat(SALARIO_DIA_15.classifica(movimento("2026-10-15", "1200.00", "PIX RECEBIDO ITAU UNIBANCO")))
                .isFalse();
        assertThat(SALARIO_DIA_15.classifica(movimento("2026-10-16", "4800.00", "PIX RECEBIDO ITAU UNIBANCO")))
                .isFalse();
        assertThat(SALARIO_DIA_15.classifica(movimento("2026-10-15", "4800.00", "PIX RECEBIDO JOSE")))
                .isFalse();
    }

    @Test
    void faixaQueViraOMesCobreFimEInicio() {
        assertThat(SALARIO_VIRADA.classifica(movimento("2026-10-29", "3800.00", "PIX ITAU"))).isTrue();
        assertThat(SALARIO_VIRADA.classifica(movimento("2026-11-01", "3800.00", "PIX ITAU"))).isTrue();
        assertThat(SALARIO_VIRADA.classifica(movimento("2026-11-02", "3800.00", "PIX ITAU"))).isTrue();
        assertThat(SALARIO_VIRADA.classifica(movimento("2026-11-03", "3800.00", "PIX ITAU"))).isFalse();
        assertThat(SALARIO_VIRADA.classifica(movimento("2026-10-29", "3900.00", "PIX ITAU"))).isFalse();
    }

    @Test
    void termoIgnoraCaixaEAcento() {
        RegraClassificacao regra = new RegraClassificacao(
                3L, 42L, "benefício", 1, 31, BigDecimal.ZERO, new BigDecimal("9999.00"), "Benefícios");

        assertThat(regra.classifica(movimento("2026-10-05", "10.00", "BENEFICIO FLASH"))).isTrue();
        assertThat(regra.classifica(movimento("2026-10-05", "10.00", "Benefício Flash"))).isTrue();
    }

    @Test
    void recusaRegraInvalida() {
        assertThatThrownBy(() -> new RegraClassificacao(1L, 42L, " ", 1, 1, BigDecimal.ZERO,
                BigDecimal.TEN, "Salário")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RegraClassificacao(1L, 42L, "X", 1, 1, BigDecimal.ZERO,
                BigDecimal.TEN, " ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RegraClassificacao(1L, 42L, "X", 0, 1, BigDecimal.ZERO,
                BigDecimal.TEN, "Salário")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RegraClassificacao(1L, 42L, "X", 1, 32, BigDecimal.ZERO,
                BigDecimal.TEN, "Salário")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RegraClassificacao(1L, 42L, "X", 1, 1, BigDecimal.TEN,
                BigDecimal.ONE, "Salário")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RegraClassificacao(1L, 42L, "X", 1, 1, null,
                BigDecimal.ONE, "Salário")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void bancoReconheceCodigoENome() {
        assertThat(BancoOfx.porCodigo("260")).contains(BancoOfx.NUBANK);
        assertThat(BancoOfx.porCodigo("0260")).contains(BancoOfx.NUBANK);
        assertThat(BancoOfx.porCodigo("001")).contains(BancoOfx.BANCO_DO_BRASIL);
        assertThat(BancoOfx.porCodigo("999")).isEmpty();
        assertThat(BancoOfx.porCodigo(null)).isEmpty();
        assertThat(BancoOfx.porCodigo(" ")).isEmpty();
        assertThat(BancoOfx.porNome("Nubank")).contains(BancoOfx.NUBANK);
        assertThat(BancoOfx.porNome("banco do brasil")).contains(BancoOfx.BANCO_DO_BRASIL);
        assertThat(BancoOfx.porNome("Itaú")).contains(BancoOfx.ITAU);
        assertThat(BancoOfx.porNome("Outro")).isEmpty();
        assertThat(BancoOfx.porNome(null)).isEmpty();
        assertThat(BancoOfx.NUBANK.codigo()).isEqualTo("260");
        assertThat(BancoOfx.NUBANK.nome()).isEqualTo("Nubank");
    }

    private static MovimentoOfx movimento(String data, String valor, String descricao) {
        return new MovimentoOfx("FIT", LocalDate.parse(data), new Dinheiro(new BigDecimal(valor)),
                Natureza.RECEITA, descricao);
    }
}
