package br.com.fincontrol.domain.service;

public class ComprovanteTipoInvalidoException extends RuntimeException {

    public ComprovanteTipoInvalidoException() {
        super("Comprovante deve ser PNG, JPEG ou PDF.");
    }
}
