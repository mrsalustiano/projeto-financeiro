package br.com.fincontrol.domain.model;

public record DecisaoLogin(Tipo tipo, Usuario usuario, boolean persistir) {

    public enum Tipo {
        OK,
        RECUSADO,
        BLOQUEADO
    }
}
