package br.com.fincontrol.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class CatalogoTest {

    @Test
    void saldoZeroAceitoEDespesaNaoMexeEmOutraConta() {
        Saldo nubank = Saldo.zero();
        Saldo flash = new Saldo(new BigDecimal("1570"));
        Saldo depois = flash.menos(new Dinheiro(new BigDecimal("40.00")));

        assertThat(depois.valor()).isEqualByComparingTo("1530.00");
        assertThat(nubank.valor()).isEqualByComparingTo("0.00");
        assertThat(flash.valor()).isEqualByComparingTo("1570.00");
    }

    @Test
    void saldoNegativoRecusado() {
        assertThatThrownBy(() -> new Saldo(new BigDecimal("-0.01"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Saldo.zero().menos(new Dinheiro(new BigDecimal("1")))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void beneficioNaoTemLimiteECartaoExige() {
        LocalDate hoje = LocalDate.of(2026, 11, 22);
        new Conta(1L, 1L, "Flash", "Flash", TipoConta.BENEFICIO, Saldo.zero(), hoje, null);
        Conta.validarLimite(TipoConta.CARTAO_CREDITO, new Dinheiro(new BigDecimal("100")));
        Conta.validarLimite(TipoConta.CORRENTE, null);
        assertThatThrownBy(() -> Conta.validarLimite(TipoConta.BENEFICIO, new Dinheiro(new BigDecimal("10"))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Conta.validarLimite(TipoConta.CARTAO_CREDITO, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Conta.validarLimite(TipoConta.CORRENTE, new Dinheiro(new BigDecimal("10"))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void categoriasPadraoIncluemBeneficios() {
        assertThat(CategoriaPadrao.BENEFICIOS.natureza()).isEqualTo(Natureza.RECEITA);
        assertThat(CategoriaPadrao.ALIMENTACAO.nome()).isEqualTo("Alimentação");
    }
}
