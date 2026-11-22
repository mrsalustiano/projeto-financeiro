package br.com.fincontrol.domain.service;

import java.util.List;
import java.util.Optional;

import br.com.fincontrol.domain.model.Categoria;
import br.com.fincontrol.domain.model.Natureza;

public interface CategoriaRepositorio {

    List<Categoria> listar(long clienteId);

    Optional<Categoria> buscar(long clienteId, long id);

    boolean existeNome(long clienteId, String nome);

    long inserir(long clienteId, String nome, Natureza natureza);

    void atualizar(long clienteId, long id, String nome, Natureza natureza);

    boolean excluir(long clienteId, long id);
}
