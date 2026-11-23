package br.com.fincontrol.application;

import br.com.fincontrol.domain.model.Dinheiro;
import br.com.fincontrol.domain.model.Natureza;
import br.com.fincontrol.domain.model.TipoDespesa;

public record RecorrenciaSeed(
        String conta,
        String categoria,
        Natureza natureza,
        TipoDespesa tipoDespesa,
        Dinheiro valor,
        String descricao,
        int diaDoMes) {
}
