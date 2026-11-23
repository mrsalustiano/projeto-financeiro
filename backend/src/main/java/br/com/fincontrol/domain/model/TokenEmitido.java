package br.com.fincontrol.domain.model;

import java.time.Instant;

public record TokenEmitido(String valor, String jti, Instant emitidoEm, Instant expiraEm) {
}
