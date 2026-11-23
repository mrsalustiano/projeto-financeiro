package br.com.fincontrol.domain.service;

import java.util.List;
import java.util.Optional;

import br.com.fincontrol.domain.model.Orcamento;

public interface OrcamentoRepositorio {

    Optional<Orcamento> buscar(long clienteId, long categoriaId, int ano, int mes);

    List<Orcamento> listar(long clienteId, int ano, int mes);

    long salvar(Orcamento orcamento);

    boolean excluir(long clienteId, long categoriaId, int ano, int mes);
}
