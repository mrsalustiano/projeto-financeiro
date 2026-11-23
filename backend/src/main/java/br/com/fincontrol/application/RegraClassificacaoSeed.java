package br.com.fincontrol.application;

import java.math.BigDecimal;

public record RegraClassificacaoSeed(
        String termo,
        int diaInicio,
        int diaFim,
        BigDecimal valorMinimo,
        BigDecimal valorMaximo,
        String categoria) {
}
