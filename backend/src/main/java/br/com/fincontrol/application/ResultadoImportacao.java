package br.com.fincontrol.application;

import java.math.BigDecimal;
import java.util.List;

public record ResultadoImportacao(
        int importados,
        int duplicados,
        int semCategoria,
        List<String> avisos,
        BigDecimal saldoInformado,
        String moeda,
        String banco,
        long contaId,
        String identificadorConta,
        BigDecimal rendimento) {

    public ResultadoImportacao {
        avisos = avisos == null ? List.of() : List.copyOf(avisos);
    }
}
