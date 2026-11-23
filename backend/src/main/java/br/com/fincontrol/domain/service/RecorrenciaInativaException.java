package br.com.fincontrol.domain.service;

public class RecorrenciaInativaException extends RuntimeException {

    public RecorrenciaInativaException() {
        super("Recorrência já está no estado pedido.");
    }
}
