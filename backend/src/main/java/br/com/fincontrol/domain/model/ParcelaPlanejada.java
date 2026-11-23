package br.com.fincontrol.domain.model;

import java.time.LocalDate;

public record ParcelaPlanejada(int numero, Dinheiro valor, LocalDate data) {

    public ParcelaPlanejada {
        if (numero < 1) {
            throw new IllegalArgumentException("Número da parcela inválido.");
        }
        if (valor == null || data == null) {
            throw new IllegalArgumentException("Valor e data da parcela são obrigatórios.");
        }
    }
}
