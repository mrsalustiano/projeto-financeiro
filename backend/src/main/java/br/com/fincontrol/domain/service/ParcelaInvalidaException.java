package br.com.fincontrol.domain.service;

public class ParcelaInvalidaException extends RuntimeException {

    public ParcelaInvalidaException() {
        super("Número de parcelas inválido para o valor informado.");
    }
}
