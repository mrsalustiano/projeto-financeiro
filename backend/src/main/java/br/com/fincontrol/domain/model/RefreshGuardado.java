package br.com.fincontrol.domain.model;

import java.time.Instant;

public record RefreshGuardado(
        long id,
        long usuarioId,
        Instant expiraEm,
        boolean revogado,
        String jtiAccess,
        Instant accessExpiraEm) {
}
