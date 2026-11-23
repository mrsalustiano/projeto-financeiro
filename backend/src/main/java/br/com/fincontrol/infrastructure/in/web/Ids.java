package br.com.fincontrol.infrastructure.in.web;

import br.com.fincontrol.domain.service.NaoEncontradoException;

public final class Ids {

    private Ids() {
    }

    public static long ler(String texto) {
        try {
            return Long.parseLong(texto);
        } catch (NumberFormatException ex) {
            throw new NaoEncontradoException();
        }
    }
}
