package br.com.fincontrol.domain.service;

public class IaDesabilitadaException extends RuntimeException {

    public IaDesabilitadaException() {
        super("Análise por IA desabilitada.");
    }
}
