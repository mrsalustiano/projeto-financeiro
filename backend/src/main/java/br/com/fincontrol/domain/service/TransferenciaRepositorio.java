package br.com.fincontrol.domain.service;

import java.util.List;
import java.util.Optional;

import br.com.fincontrol.domain.model.Transferencia;

public interface TransferenciaRepositorio {

    long inserir(Transferencia transferencia);

    Optional<Transferencia> buscar(long clienteId, long id);

    List<Transferencia> listar(long clienteId);
}
