package br.com.fincontrol.domain.service;

import java.util.List;
import java.util.Optional;

import br.com.fincontrol.domain.model.FechamentoMes;

public interface FechamentoMesRepositorio {

    Optional<FechamentoMes> buscar(long clienteId, int ano, int mes);

    List<FechamentoMes> listar(long clienteId);

    long inserir(FechamentoMes fechamento);

    boolean excluir(long clienteId, int ano, int mes);
}
