package br.com.fincontrol.domain.service;

public class IdempotenciaConflitoException extends RuntimeException {

    public IdempotenciaConflitoException() {
        super("Chave de idempotência já usada com outro corpo.");
    }
}
