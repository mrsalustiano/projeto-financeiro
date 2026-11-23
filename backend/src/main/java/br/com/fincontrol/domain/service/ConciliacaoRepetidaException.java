package br.com.fincontrol.domain.service;

public class ConciliacaoRepetidaException extends RuntimeException {

    public ConciliacaoRepetidaException() {
        super("Lançamento ou item de extrato já conciliado.");
    }
}
