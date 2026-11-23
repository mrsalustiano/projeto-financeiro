package br.com.fincontrol.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class LancamentosDominioTest {

    private static final Instant AGORA = Instant.parse("2026-11-22T03:00:00Z");

    @Test
    void parcelaDivideECentavosVaoParaPrimeira() {
        Parcelamento parcelamento = Parcelamento.planejar(
                "grupo", new Dinheiro(new BigDecimal("100.00")), 3, LocalDate.of(2026, 1, 31));

        assertThat(parcelamento.parcelas()).extracting(ParcelaPlanejada::valor)
                .extracting(Dinheiro::valor)
                .containsExactly(new BigDecimal("33.34"), new BigDecimal("33.33"), new BigDecimal("33.33"));
        assertThat(parcelamento.parcelas()).extracting(ParcelaPlanejada::data)
                .containsExactly(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 2, 28), LocalDate.of(2026, 3, 31));
        assertThat(parcelamento.parcelas()).extracting(ParcelaPlanejada::numero).containsExactly(1, 2, 3);
    }

    @Test
    void parcelaRecusaNumeroOuValorImpossivel() {
        Dinheiro dez = new Dinheiro(new BigDecimal("10.00"));
        assertThatThrownBy(() -> Parcelamento.planejar("g", dez, 0, LocalDate.of(2026, 1, 1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Parcelamento.planejar("g", dez, 61, LocalDate.of(2026, 1, 1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Parcelamento.planejar("g", new Dinheiro(new BigDecimal("0.01")), 60, LocalDate.of(2026, 1, 1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Parcelamento(" ", 1, dez, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Parcelamento("g", 2, dez, List.of(new ParcelaPlanejada(1, dez, LocalDate.now()))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ParcelaPlanejada(0, dez, LocalDate.now()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ParcelaPlanejada(1, null, LocalDate.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void estornoAconteceUmaVezENaoEdita() {
        Lancamento lancamento = despesa();
        Lancamento estornado = lancamento.estornar("engano", "7", AGORA);

        assertThat(estornado.estornado()).isTrue();
        assertThat(estornado.estornoMotivo()).isEqualTo("engano");
        assertThat(estornado.estornadoPor()).isEqualTo("7");
        assertThatThrownBy(() -> estornado.estornar("de novo", "7", AGORA))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> estornado.editar(1L, 2L, Natureza.DESPESA, TipoDespesa.FIXA,
                new Dinheiro(new BigDecimal("5")), LocalDate.of(2026, 11, 1), "x"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void lancamentoValidaCampos() {
        assertThatThrownBy(() -> new Lancamento(0L, 42L, 1L, 2L, Natureza.DESPESA, null,
                new Dinheiro(new BigDecimal("1")), LocalDate.of(2026, 11, 1), "x", null, null, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Lancamento(0L, 42L, 1L, 2L, Natureza.DESPESA, TipoDespesa.FIXA,
                new Dinheiro(new BigDecimal("1")), LocalDate.of(2026, 11, 1), " ", null, null, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Lancamento(0L, 42L, 1L, 2L, Natureza.DESPESA, TipoDespesa.FIXA,
                new Dinheiro(new BigDecimal("1")), LocalDate.of(2026, 11, 1), "x".repeat(256), null, null, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Lancamento(0L, 42L, 1L, 2L, Natureza.RECEITA, null,
                new Dinheiro(new BigDecimal("1")), LocalDate.of(2026, 11, 1), "x", null, null, null, null, AGORA, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Lancamento(0L, 42L, 1L, 2L, Natureza.RECEITA, null,
                new Dinheiro(new BigDecimal("1")), LocalDate.of(2026, 11, 1), "x", null, null, null, null, AGORA, "m", " "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Lancamento(0L, 42L, 1L, 2L, null, null,
                new Dinheiro(new BigDecimal("1")), LocalDate.of(2026, 11, 1), "x", null, null, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void receitaDescartaTipoDespesa() {
        Lancamento receita = new Lancamento(0L, 42L, 1L, 2L, Natureza.RECEITA, TipoDespesa.FIXA,
                new Dinheiro(new BigDecimal("10")), LocalDate.of(2026, 11, 1), " salario ", null, null, null, null, null, null, null);

        assertThat(receita.tipoDespesa()).isNull();
        assertThat(receita.descricao()).isEqualTo("salario");
    }

    @Test
    void fechamentoSaldoEFiltro() {
        FechamentoMes fechamento = new FechamentoMes(1L, 42L, 2026, 11, AGORA, "7");

        assertThat(fechamento.cobre(LocalDate.of(2026, 11, 30))).isTrue();
        assertThat(fechamento.cobre(LocalDate.of(2026, 12, 1))).isFalse();
        assertThat(fechamento.cobre(null)).isFalse();
        assertThatThrownBy(() -> new FechamentoMes(1L, 42L, 1999, 11, AGORA, "7")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FechamentoMes(1L, 42L, 2026, 13, AGORA, "7")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FechamentoMes(1L, 42L, 2026, 11, null, "7")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FechamentoMes(1L, 42L, 2026, 11, AGORA, " ")).isInstanceOf(IllegalArgumentException.class);

        SaldoConta saldo = new SaldoConta(new BigDecimal("100"), new BigDecimal("50.5"), new BigDecimal("30.25"));
        assertThat(saldo.saldo()).isEqualByComparingTo("120.25");
        assertThatThrownBy(() -> new SaldoConta(null, BigDecimal.ZERO, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(FiltroLancamento.vazio().contaId()).isNull();
        assertThatThrownBy(() -> new FiltroLancamento(null, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RegistroIdempotencia(" ", 1L)).isInstanceOf(IllegalArgumentException.class);
    }

    private static Lancamento despesa() {
        return new Lancamento(1L, 42L, 1L, 2L, Natureza.DESPESA, TipoDespesa.VARIAVEL,
                new Dinheiro(new BigDecimal("10")), LocalDate.of(2026, 11, 1), "mercado",
                null, null, null, null, null, null, null);
    }
}
