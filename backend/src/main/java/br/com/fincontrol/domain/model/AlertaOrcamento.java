package br.com.fincontrol.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record AlertaOrcamento(
        Dinheiro limite,
        BigDecimal gasto,
        BigDecimal percentual,
        boolean alerta,
        boolean estourou) {

    public static final BigDecimal LIMIAR = new BigDecimal("80.00");

    public AlertaOrcamento {
        if (limite == null || gasto == null) {
            throw new IllegalArgumentException("Limite e gasto são obrigatórios.");
        }
        gasto = gasto.setScale(2, RoundingMode.HALF_UP);
        percentual = percentual.setScale(2, RoundingMode.HALF_UP);
    }
}
