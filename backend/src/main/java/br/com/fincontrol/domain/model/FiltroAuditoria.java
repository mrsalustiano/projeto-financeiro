package br.com.fincontrol.domain.model;

import java.time.LocalDate;

public record FiltroAuditoria(Long clienteId, String recurso, AcaoAuditoria acao, LocalDate de, LocalDate ate) {

    public FiltroAuditoria {
        recurso = recurso == null || recurso.isBlank() ? null : recurso.trim();
        if (de != null && ate != null && de.isAfter(ate)) {
            throw new IllegalArgumentException("Período inválido.");
        }
    }

    public static FiltroAuditoria vazio() {
        return new FiltroAuditoria(null, null, null, null, null);
    }
}
