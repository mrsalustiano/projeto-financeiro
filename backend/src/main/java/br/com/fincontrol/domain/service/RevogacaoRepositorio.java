package br.com.fincontrol.domain.service;

import java.time.Instant;

public interface RevogacaoRepositorio {

    void revogar(String jti, Instant expiraEm);

    boolean revogado(String jti);
}
