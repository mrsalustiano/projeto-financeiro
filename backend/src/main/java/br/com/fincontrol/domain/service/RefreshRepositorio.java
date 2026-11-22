package br.com.fincontrol.domain.service;

import java.time.Instant;
import java.util.Optional;

import br.com.fincontrol.domain.model.RefreshGuardado;

public interface RefreshRepositorio {

    Optional<RefreshGuardado> buscarPorHash(String hash);

    void inserir(long usuarioId, String hash, Instant expiraEm, String jtiAccess, Instant accessExpiraEm);

    void revogar(long id);
}
