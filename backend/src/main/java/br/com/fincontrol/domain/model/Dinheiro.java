package br.com.fincontrol.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Dinheiro(BigDecimal valor) {

    public Dinheiro {
        if (valor == null) {
            throw new IllegalArgumentException("Valor obrigatório.");
        }
        valor = valor.setScale(2, RoundingMode.HALF_UP);
        if (valor.signum() <= 0) {
            throw new IllegalArgumentException("Valor deve ser positivo.");
        }
    }
}
