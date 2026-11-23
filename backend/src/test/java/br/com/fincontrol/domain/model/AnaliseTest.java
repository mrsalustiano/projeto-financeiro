package br.com.fincontrol.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.Test;

class AnaliseTest {

    @Test
    void validaAnalise() {
        AnaliseFinanceira analise = new AnaliseFinanceira("  Resumo  ", List.of(" um ", "dois"), List.of("acao"));

        assertThat(analise.resumo()).isEqualTo("Resumo");
        assertThat(analise.insights()).containsExactly("um", "dois");
        assertThat(analise.acoes()).containsExactly("acao");
    }

    @Test
    void recusaAnaliseInvalida() {
        assertThatThrownBy(() -> new AnaliseFinanceira(" ", List.of("a"), List.of("b")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AnaliseFinanceira("r", List.of(), List.of("b")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AnaliseFinanceira("r", List.of("a"), null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AnaliseFinanceira("r", List.of(" "), List.of("b")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AnaliseFinanceira("r", List.of("a"), List.of("b", "c", "d", "e", "f", "g", "h",
                "i", "j", "k", "l")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AnaliseFinanceira("x".repeat(2001), List.of("a"), List.of("b")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validaPedido() {
        PedidoAnalise pedido = new PedidoAnalise(YearMonth.of(2026, 11), TipoAnalise.PRESUNTIVA, "  e agora?  ");

        assertThat(pedido.pergunta()).isEqualTo("e agora?");
        assertThat(new PedidoAnalise(YearMonth.of(2026, 11), TipoAnalise.COMPARATIVA, "  ").pergunta()).isNull();
        assertThat(new PedidoAnalise(YearMonth.of(2026, 11), TipoAnalise.COMPORTAMENTAL, null).pergunta()).isNull();
        assertThatThrownBy(() -> new PedidoAnalise(null, TipoAnalise.PRESUNTIVA, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PedidoAnalise(YearMonth.of(2026, 11), null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PedidoAnalise(YearMonth.of(2026, 11), TipoAnalise.PRESUNTIVA, "x".repeat(501)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validaAgregado() {
        AgregadoMes agregado = new AgregadoMes(
                YearMonth.of(2026, 11),
                new BigDecimal("5000.00"),
                new BigDecimal("1500.00"),
                10,
                List.of(new AgregadoMes.LinhaCategoria("Alimentação", new BigDecimal("320.00"))),
                List.of(new AgregadoMes.LinhaConta("Flash", TipoConta.BENEFICIO, new BigDecimal("-390.00"))));

        assertThat(agregado.saldo()).isEqualByComparingTo("3500.00");
        assertThat(agregado.categorias()).hasSize(1);
        assertThat(agregado.contas()).hasSize(1);
        assertThat(new AgregadoMes(YearMonth.of(2026, 11), BigDecimal.ZERO, BigDecimal.ZERO, 0, null, null)
                .categorias()).isEmpty();
    }

    @Test
    void recusaAgregadoInvalido() {
        assertThatThrownBy(() -> new AgregadoMes(null, BigDecimal.ZERO, BigDecimal.ZERO, 0, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AgregadoMes(YearMonth.of(2026, 11), null, BigDecimal.ZERO, 0, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AgregadoMes.LinhaCategoria("x", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AgregadoMes.LinhaConta(" ", TipoConta.CORRENTE, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AgregadoMes.LinhaConta("Nubank", null, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(new AgregadoMes.LinhaCategoria("  ", BigDecimal.ONE).nome()).isEqualTo("Sem categoria");
    }
}
