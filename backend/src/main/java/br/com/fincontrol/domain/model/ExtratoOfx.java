package br.com.fincontrol.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ExtratoOfx(
        String org,
        String fid,
        String bankId,
        String acctId,
        String moeda,
        LocalDate inicio,
        LocalDate fim,
        List<MovimentoOfx> movimentos,
        BigDecimal rendimento,
        BigDecimal saldoInformado,
        List<String> avisos) {

    public ExtratoOfx {
        movimentos = movimentos == null ? List.of() : List.copyOf(movimentos);
        avisos = avisos == null ? List.of() : List.copyOf(avisos);
    }

    public boolean vazio() {
        return movimentos.isEmpty() && rendimento == null;
    }
}
