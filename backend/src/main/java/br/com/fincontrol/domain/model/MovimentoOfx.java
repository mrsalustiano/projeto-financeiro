package br.com.fincontrol.domain.model;

import java.time.LocalDate;

public record MovimentoOfx(String fitid, LocalDate data, Dinheiro valor, Natureza natureza, String descricao) {

    public MovimentoOfx {
        if (data == null || valor == null || natureza == null) {
            throw new IllegalArgumentException("Data, valor e natureza do movimento são obrigatórios.");
        }
        descricao = descricao == null ? "" : descricao.trim();
    }

    public boolean temFitid() {
        return fitid != null && !fitid.isBlank();
    }
}
