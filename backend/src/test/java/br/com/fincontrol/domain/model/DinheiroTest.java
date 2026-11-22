package br.com.fincontrol.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class DinheiroTest {

    @Test
    void arredondaParaDuasCasas() {
        Dinheiro dinheiro = new Dinheiro(new BigDecimal("10.005"));

        assertThat(dinheiro.valor()).isEqualByComparingTo("10.01");
    }

    @Test
    void recusaNuloZeroENegativo() {
        assertThatThrownBy(() -> new Dinheiro(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Dinheiro(BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Dinheiro(new BigDecimal("-1"))).isInstanceOf(IllegalArgumentException.class);
    }
}
