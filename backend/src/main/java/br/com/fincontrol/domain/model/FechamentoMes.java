package br.com.fincontrol.domain.model;

import java.time.Instant;
import java.time.LocalDate;

public record FechamentoMes(long id, long clienteId, int ano, int mes, Instant fechadoEm, String fechadoPor) {

    public FechamentoMes {
        if (ano < 2000 || ano > 2100) {
            throw new IllegalArgumentException("Ano inválido.");
        }
        if (mes < 1 || mes > 12) {
            throw new IllegalArgumentException("Mês inválido.");
        }
        if (fechadoEm == null) {
            throw new IllegalArgumentException("Data do fechamento obrigatória.");
        }
        if (fechadoPor == null || fechadoPor.isBlank()) {
            throw new IllegalArgumentException("Responsável pelo fechamento obrigatório.");
        }
    }

    public boolean cobre(LocalDate data) {
        return data != null && data.getYear() == ano && data.getMonthValue() == mes;
    }
}
