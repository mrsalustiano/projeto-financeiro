package br.com.fincontrol.domain.service;

public class IaIndisponivelException extends RuntimeException {

    public IaIndisponivelException(Throwable causa) {
        super("Provedor de IA indisponível.", causa);
    }
}
