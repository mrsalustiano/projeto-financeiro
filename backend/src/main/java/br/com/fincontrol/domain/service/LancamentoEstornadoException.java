package br.com.fincontrol.domain.service;

public class LancamentoEstornadoException extends RuntimeException {

    public LancamentoEstornadoException() {
        super("Lançamento estornado não pode ser alterado.");
    }
}
