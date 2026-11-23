package br.com.fincontrol.domain.model;

import java.math.BigDecimal;

public record FaixaBeneficio(BigDecimal minimo, BigDecimal maximo) {

    public static final FaixaBeneficio FLASH = new FaixaBeneficio(
            new BigDecimal("1520.00"), new BigDecimal("1620.00"));

    public FaixaBeneficio {
        if (minimo == null || maximo == null || minimo.signum() <= 0 || minimo.compareTo(maximo) > 0) {
            throw new IllegalArgumentException("Faixa inválida.");
        }
    }

    public boolean aceita(Dinheiro valor) {
        return valor != null
                && valor.valor().compareTo(minimo) >= 0
                && valor.valor().compareTo(maximo) <= 0;
    }
}
