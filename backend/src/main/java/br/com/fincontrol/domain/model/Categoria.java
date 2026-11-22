package br.com.fincontrol.domain.model;

public record Categoria(long id, long clienteId, String nome, Natureza natureza) {
}
