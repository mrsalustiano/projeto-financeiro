package br.com.fincontrol.domain.model;

import java.time.Instant;

public record Acesso(String usuarioId, Papel papel, Long clienteId, String jti, Instant expiraEm) {
}
