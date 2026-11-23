package br.com.fincontrol.domain.service;

public class ComprovanteGrandeException extends RuntimeException {

    public ComprovanteGrandeException() {
        super("Comprovante acima de 1 MB.");
    }
}
