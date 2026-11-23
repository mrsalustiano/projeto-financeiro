package br.com.fincontrol.domain.service;

public class EstornoRepetidoException extends RuntimeException {

    public EstornoRepetidoException() {
        super("Lançamento já estornado.");
    }
}
