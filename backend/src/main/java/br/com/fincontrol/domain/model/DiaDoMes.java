package br.com.fincontrol.domain.model;

import java.time.LocalDate;
import java.time.YearMonth;

public record DiaDoMes(int dia) {

    public static final int ULTIMO = 0;
    public static final int MAXIMO = 31;

    public DiaDoMes {
        if (dia < ULTIMO || dia > MAXIMO) {
            throw new IllegalArgumentException("Dia do mês inválido.");
        }
    }

    public static DiaDoMes ultimo() {
        return new DiaDoMes(ULTIMO);
    }

    public LocalDate venceEm(YearMonth mes) {
        if (dia == ULTIMO) {
            return mes.atEndOfMonth();
        }
        return mes.atDay(Math.min(dia, mes.lengthOfMonth()));
    }
}
