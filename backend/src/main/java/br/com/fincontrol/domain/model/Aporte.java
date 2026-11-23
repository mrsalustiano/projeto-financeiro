package br.com.fincontrol.domain.model;

import java.time.LocalDate;

public record Aporte(long id, long clienteId, long metaId, Dinheiro valor, LocalDate data) {

    public Aporte {
        if (valor == null || data == null) {
            throw new IllegalArgumentException("Valor e data do aporte são obrigatórios.");
        }
    }
}
