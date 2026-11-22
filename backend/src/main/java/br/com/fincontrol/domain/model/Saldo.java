package br.com.fincontrol.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Saldo(BigDecimal valor) {

    public Saldo {
        if (valor == null) {
            throw new IllegalArgumentException("Valor obrigatório.");
        }
        valor = valor.setScale(2, RoundingMode.HALF_UP);
        if (valor.signum() < 0) {
            throw new IllegalArgumentException("Saldo não pode ser negativo.");
        }
    }

    public static Saldo zero() {
        return new Saldo(BigDecimal.ZERO);
    }

    public Saldo menos(Dinheiro despesa) {
        return new Saldo(valor.subtract(despesa.valor()));
    }
}
