package br.com.fincontrol.domain.service;

import java.util.Optional;

import br.com.fincontrol.domain.model.NovoUsuario;
import br.com.fincontrol.domain.model.Usuario;

public interface UsuarioRepositorio {

    Optional<Usuario> buscarPorEmail(String email);

    Optional<Usuario> buscarPorId(long id);

    boolean emailExiste(String email);

    void salvarEstado(Usuario usuario);

    long inserir(NovoUsuario usuario);
}
