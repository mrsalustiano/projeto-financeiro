package br.com.fincontrol.domain.model;

import java.math.BigDecimal;

public record ProgressoMeta(Dinheiro valorAlvo, BigDecimal aportado, BigDecimal percentual, boolean alcancada) {

    public ProgressoMeta {
        if (valorAlvo == null || aportado == null || percentual == null) {
            throw new IllegalArgumentException("Progresso incompleto.");
        }
    }
}
