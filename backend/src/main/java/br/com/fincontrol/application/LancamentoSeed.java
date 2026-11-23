package br.com.fincontrol.application;

import java.time.LocalDate;

import br.com.fincontrol.domain.model.Dinheiro;
import br.com.fincontrol.domain.model.Natureza;
import br.com.fincontrol.domain.model.TipoDespesa;

public record LancamentoSeed(
        String conta,
        String categoria,
        Natureza natureza,
        TipoDespesa tipoDespesa,
        Dinheiro valor,
        LocalDate data,
        String descricao) {
}
