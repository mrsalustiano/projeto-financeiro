package br.com.fincontrol.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record SaldoConta(BigDecimal saldoInicial, BigDecimal creditos, BigDecimal debitos) {

    public SaldoConta {
        saldoInicial = exigir(saldoInicial);
        creditos = exigir(creditos);
        debitos = exigir(debitos);
    }

    public BigDecimal saldo() {
        return saldoInicial.add(creditos).subtract(debitos);
    }

    private static BigDecimal exigir(BigDecimal valor) {
        if (valor == null) {
            throw new IllegalArgumentException("Valor obrigatório.");
        }
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
