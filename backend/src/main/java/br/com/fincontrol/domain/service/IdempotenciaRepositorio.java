package br.com.fincontrol.domain.service;

import java.util.Optional;

import br.com.fincontrol.domain.model.RegistroIdempotencia;

public interface IdempotenciaRepositorio {

    Optional<RegistroIdempotencia> buscar(long clienteId, String chave);

    void guardar(long clienteId, String chave, String hashCorpo, long recursoId);
}
