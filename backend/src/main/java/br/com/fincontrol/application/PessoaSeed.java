package br.com.fincontrol.application;

import br.com.fincontrol.domain.model.Papel;

public record PessoaSeed(
        String nome,
        String email,
        String nomeCliente,
        Papel papel,
        boolean usuarioAtivo,
        boolean clienteAtivo) {
}
