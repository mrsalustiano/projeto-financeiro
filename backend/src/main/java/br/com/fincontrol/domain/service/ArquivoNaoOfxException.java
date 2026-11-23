package br.com.fincontrol.domain.service;

public class ArquivoNaoOfxException extends RuntimeException {

    public ArquivoNaoOfxException() {
        super("Arquivo não é um OFX válido.");
    }
}
