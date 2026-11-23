package br.com.fincontrol.domain.model;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Locale;

public record RegraClassificacao(
        long id,
        long clienteId,
        String termo,
        int diaInicio,
        int diaFim,
        BigDecimal valorMinimo,
        BigDecimal valorMaximo,
        String categoria) {

    public RegraClassificacao {
        if (termo == null || termo.isBlank()) {
            throw new IllegalArgumentException("Termo da regra obrigatório.");
        }
        if (categoria == null || categoria.isBlank()) {
            throw new IllegalArgumentException("Categoria da regra obrigatória.");
        }
        if (diaInicio < 1 || diaInicio > 31 || diaFim < 1 || diaFim > 31) {
            throw new IllegalArgumentException("Dia da regra inválido.");
        }
        if (valorMinimo == null || valorMaximo == null || valorMinimo.signum() < 0
                || valorMaximo.compareTo(valorMinimo) < 0) {
            throw new IllegalArgumentException("Faixa de valor da regra inválida.");
        }
        termo = termo.trim();
        categoria = categoria.trim();
    }

    public boolean classifica(MovimentoOfx movimento) {
        return contemTermo(movimento.descricao())
                && diaValido(movimento.data().getDayOfMonth())
                && valorValido(movimento.valor().valor());
    }

    private boolean contemTermo(String descricao) {
        return normalizar(descricao).contains(normalizar(termo));
    }

    private boolean diaValido(int dia) {
        if (diaInicio <= diaFim) {
            return dia >= diaInicio && dia <= diaFim;
        }
        return dia >= diaInicio || dia <= diaFim;
    }

    private boolean valorValido(BigDecimal valor) {
        return valor.compareTo(valorMinimo) >= 0 && valor.compareTo(valorMaximo) <= 0;
    }

    static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.toUpperCase(Locale.ROOT).trim();
    }
}
