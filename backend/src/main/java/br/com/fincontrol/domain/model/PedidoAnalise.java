package br.com.fincontrol.domain.model;

import java.time.YearMonth;

public record PedidoAnalise(YearMonth mes, TipoAnalise tipo, String pergunta) {

    public static final int PERGUNTA_MAXIMA = 500;

    public PedidoAnalise {
        if (mes == null) {
            throw new IllegalArgumentException("Mês obrigatório.");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("Tipo de análise obrigatório.");
        }
        pergunta = normalizar(pergunta);
    }

    private static String normalizar(String pergunta) {
        if (pergunta == null || pergunta.isBlank()) {
            return null;
        }
        String limpa = pergunta.trim();
        if (limpa.length() > PERGUNTA_MAXIMA) {
            throw new IllegalArgumentException("Pergunta acima do limite.");
        }
        return limpa;
    }
}
