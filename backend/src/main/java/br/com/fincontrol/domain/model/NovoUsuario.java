package br.com.fincontrol.domain.model;

public record NovoUsuario(
        Long clienteId,
        String nome,
        String email,
        String senhaHash,
        Papel papel,
        boolean ativo) {
}
