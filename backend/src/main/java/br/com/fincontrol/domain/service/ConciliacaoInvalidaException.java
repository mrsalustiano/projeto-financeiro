package br.com.fincontrol.domain.service;

public class ConciliacaoInvalidaException extends RuntimeException {

    public ConciliacaoInvalidaException() {
        super("Par de conciliação inválido.");
    }
}
