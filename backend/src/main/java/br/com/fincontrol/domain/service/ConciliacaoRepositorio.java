package br.com.fincontrol.domain.service;

import java.util.List;
import java.util.Optional;

import br.com.fincontrol.domain.model.Conciliacao;

public interface ConciliacaoRepositorio {

    List<Conciliacao> listar(long clienteId);

    Optional<Conciliacao> buscar(long clienteId, long id);

    Optional<Conciliacao> buscarPorLancamento(long clienteId, long lancamentoId);

    long inserir(Conciliacao conciliacao);

    boolean excluir(long clienteId, long id);
}
