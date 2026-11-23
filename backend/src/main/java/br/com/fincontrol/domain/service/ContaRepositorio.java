package br.com.fincontrol.domain.service;

import java.util.List;
import java.util.Optional;

import br.com.fincontrol.domain.model.Conta;

public interface ContaRepositorio {

    List<Conta> listar(long clienteId);

    Optional<Conta> buscar(long clienteId, long id);

    boolean existeNome(long clienteId, String nome);

    long inserir(Conta conta);

    void atualizar(Conta conta);

    void atualizarIdentificador(long clienteId, long id, String identificador);

    boolean excluir(long clienteId, long id);
}
