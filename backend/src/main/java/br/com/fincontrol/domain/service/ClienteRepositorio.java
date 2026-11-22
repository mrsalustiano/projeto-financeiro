package br.com.fincontrol.domain.service;

import java.util.Optional;

import br.com.fincontrol.domain.model.Cliente;

public interface ClienteRepositorio {

    Optional<Cliente> buscarPorId(long id);

    long inserir(String nome, boolean ativo);
}
