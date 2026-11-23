package br.com.fincontrol.domain.model;

public record RegistroIdempotencia(String hashCorpo, long recursoId) {

    public RegistroIdempotencia {
        if (hashCorpo == null || hashCorpo.isBlank()) {
            throw new IllegalArgumentException("Hash do corpo obrigatório.");
        }
    }
}
