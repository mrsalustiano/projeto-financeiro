package br.com.fincontrol.application;

import br.com.fincontrol.domain.model.Papel;

public record UsuarioVisao(String id, String nome, String email, Papel papel, String clienteId) {
}
