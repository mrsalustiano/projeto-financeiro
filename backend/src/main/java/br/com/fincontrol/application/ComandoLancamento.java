package br.com.fincontrol.application;

import java.time.LocalDate;

import br.com.fincontrol.domain.model.Dinheiro;
import br.com.fincontrol.domain.model.Natureza;
import br.com.fincontrol.domain.model.Parcelamento;
import br.com.fincontrol.domain.model.TipoDespesa;
import br.com.fincontrol.domain.service.ParcelaInvalidaException;

public record ComandoLancamento(
        long contaId,
        long categoriaId,
        Natureza natureza,
        TipoDespesa tipoDespesa,
        Dinheiro valor,
        LocalDate data,
        String descricao,
        int parcelas) {

    public ComandoLancamento {
        if (parcelas < Parcelamento.MINIMO || parcelas > Parcelamento.MAXIMO) {
            throw new ParcelaInvalidaException();
        }
    }
}
