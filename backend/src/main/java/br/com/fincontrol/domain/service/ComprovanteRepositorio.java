package br.com.fincontrol.domain.service;

import java.util.List;
import java.util.Optional;

import br.com.fincontrol.domain.model.Comprovante;

public interface ComprovanteRepositorio {

    List<Comprovante> listar(long clienteId, long lancamentoId);

    Optional<Comprovante> buscar(long clienteId, long id);

    long inserir(Comprovante comprovante);

    boolean excluir(long clienteId, long id);
}
