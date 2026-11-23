package br.com.fincontrol.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public record Meta(long id, long clienteId, String nome, Dinheiro valorAlvo, LocalDate dataAlvo, java.time.Instant criadaEm) {

    public static final int NOME_MAXIMO = 80;

    public Meta {
        if (valorAlvo == null || criadaEm == null) {
            throw new IllegalArgumentException("Valor alvo e criação são obrigatórios.");
        }
        nome = exigirNome(nome);
    }

    public ProgressoMeta progresso(BigDecimal aportado) {
        BigDecimal total = aportado == null ? BigDecimal.ZERO : aportado.setScale(2, RoundingMode.HALF_UP);
        BigDecimal percentual = total
                .multiply(new BigDecimal("100"))
                .divide(valorAlvo.valor(), 2, RoundingMode.HALF_UP);
        return new ProgressoMeta(valorAlvo, total, percentual, total.compareTo(valorAlvo.valor()) >= 0);
    }

    private static String exigirNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome da meta obrigatório.");
        }
        String limpo = nome.trim();
        if (limpo.length() > NOME_MAXIMO) {
            throw new IllegalArgumentException("Nome da meta acima do limite.");
        }
        return limpo;
    }
}
