package br.com.fincontrol.application;

import java.time.LocalDate;

import br.com.fincontrol.domain.model.Dinheiro;

public record ComandoTransferencia(
        long contaOrigemId,
        long contaDestinoId,
        Dinheiro valor,
        LocalDate data,
        String descricao) {
}
