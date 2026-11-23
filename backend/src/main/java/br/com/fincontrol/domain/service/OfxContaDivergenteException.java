package br.com.fincontrol.domain.service;

public class OfxContaDivergenteException extends RuntimeException {

    public OfxContaDivergenteException() {
        super("Arquivo de outro banco para a conta escolhida.");
    }
}
