package br.com.fincontrol.domain.model;

import java.time.LocalDate;

public record Transferencia(
        long id,
        long clienteId,
        long contaOrigemId,
        long contaDestinoId,
        Dinheiro valor,
        LocalDate data,
        String descricao) {

    public Transferencia {
        if (valor == null || data == null) {
            throw new IllegalArgumentException("Valor e data são obrigatórios.");
        }
        if (contaOrigemId == contaDestinoId) {
            throw new IllegalArgumentException("Conta de origem e destino iguais.");
        }
        descricao = descricao == null || descricao.isBlank() ? null : descricao.trim();
    }
}
