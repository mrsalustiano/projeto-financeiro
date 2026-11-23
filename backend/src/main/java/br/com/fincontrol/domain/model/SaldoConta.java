package br.com.fincontrol.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record SaldoConta(
        BigDecimal saldoInicial,
        BigDecimal creditos,
        BigDecimal debitos,
        BigDecimal transferenciasEntrada,
        BigDecimal transferenciasSaida) {

    public SaldoConta(BigDecimal saldoInicial, BigDecimal creditos, BigDecimal debitos) {
        this(saldoInicial, creditos, debitos, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public SaldoConta {
        saldoInicial = exigir(saldoInicial);
        creditos = exigir(creditos);
        debitos = exigir(debitos);
        transferenciasEntrada = exigir(transferenciasEntrada);
        transferenciasSaida = exigir(transferenciasSaida);
    }

    public BigDecimal saldo() {
        return saldoInicial.add(creditos).subtract(debitos)
                .add(transferenciasEntrada).subtract(transferenciasSaida);
    }

    private static BigDecimal exigir(BigDecimal valor) {
        if (valor == null) {
            throw new IllegalArgumentException("Valor obrigatório.");
        }
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
