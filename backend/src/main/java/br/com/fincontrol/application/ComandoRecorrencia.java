package br.com.fincontrol.application;

import br.com.fincontrol.domain.model.Dinheiro;
import br.com.fincontrol.domain.model.Natureza;
import br.com.fincontrol.domain.model.TipoDespesa;

public record ComandoRecorrencia(
        long contaId,
        long categoriaId,
        Natureza natureza,
        TipoDespesa tipoDespesa,
        Dinheiro valor,
        String descricao,
        int diaDoMes) {
}
