package br.com.fincontrol.domain.service;

public class OfxSemTransacaoException extends RuntimeException {

    public OfxSemTransacaoException() {
        super("OFX sem transação para importar.");
    }
}
