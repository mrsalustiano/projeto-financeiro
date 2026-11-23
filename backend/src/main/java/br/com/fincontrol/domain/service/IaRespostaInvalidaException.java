package br.com.fincontrol.domain.service;

public class IaRespostaInvalidaException extends RuntimeException {

    public IaRespostaInvalidaException() {
        super("Resposta do provedor fora do schema.");
    }

    public IaRespostaInvalidaException(Throwable causa) {
        super("Resposta do provedor fora do schema.", causa);
    }
}
