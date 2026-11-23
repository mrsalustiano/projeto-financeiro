package br.com.fincontrol.domain.model;

import java.time.LocalDate;

public record FiltroLancamento(Long contaId, LocalDate de, LocalDate ate) {

    public FiltroLancamento {
        if (de != null && ate != null && de.isAfter(ate)) {
            throw new IllegalArgumentException("Período invertido.");
        }
    }

    public static FiltroLancamento vazio() {
        return new FiltroLancamento(null, null, null);
    }
}
