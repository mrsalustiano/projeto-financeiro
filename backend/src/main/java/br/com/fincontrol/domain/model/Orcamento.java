package br.com.fincontrol.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Orcamento(long id, long clienteId, long categoriaId, int ano, int mes, Dinheiro limite) {

    public static final int ANO_MINIMO = 2000;
    public static final int ANO_MAXIMO = 2100;

    public Orcamento {
        if (ano < ANO_MINIMO || ano > ANO_MAXIMO) {
            throw new IllegalArgumentException("Ano inválido.");
        }
        if (mes < 1 || mes > 12) {
            throw new IllegalArgumentException("Mês inválido.");
        }
        if (limite == null) {
            throw new IllegalArgumentException("Limite obrigatório.");
        }
    }

    public AlertaOrcamento alertar(BigDecimal gasto) {
        BigDecimal consumido = gasto == null ? BigDecimal.ZERO : gasto;
        BigDecimal percentual = consumido
                .multiply(new BigDecimal("100"))
                .divide(limite.valor(), 2, RoundingMode.HALF_UP);
        return new AlertaOrcamento(
                limite,
                consumido,
                percentual,
                percentual.compareTo(AlertaOrcamento.LIMIAR) >= 0,
                consumido.compareTo(limite.valor()) > 0);
    }
}
