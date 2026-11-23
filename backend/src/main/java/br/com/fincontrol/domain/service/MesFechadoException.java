package br.com.fincontrol.domain.service;

public class MesFechadoException extends RuntimeException {

    public MesFechadoException() {
        super("Mês fechado para escrita.");
    }
}
