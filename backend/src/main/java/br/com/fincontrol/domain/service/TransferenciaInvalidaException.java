package br.com.fincontrol.domain.service;

public class TransferenciaInvalidaException extends RuntimeException {

    public TransferenciaInvalidaException() {
        super("Transferência inválida.");
    }
}
