package br.com.fincontrol.application;

import java.time.LocalDate;
import java.util.List;

import br.com.fincontrol.domain.model.Conta;

public record SugestaoOfx(
        String banco,
        String identificador,
        String moeda,
        LocalDate inicio,
        LocalDate fim,
        int movimentos,
        List<Conta> contasSugeridas) {

    public SugestaoOfx {
        contasSugeridas = contasSugeridas == null ? List.of() : List.copyOf(contasSugeridas);
    }
}
