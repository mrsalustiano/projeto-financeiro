package br.com.fincontrol.application;

import java.math.BigDecimal;

import br.com.fincontrol.domain.model.Dinheiro;
import br.com.fincontrol.domain.model.Saldo;
import br.com.fincontrol.domain.service.LimiteInvalidoException;

public final class ValoresTexto {

    private ValoresTexto() {
    }

    public static Saldo saldo(String texto) {
        return new Saldo(new BigDecimal(texto));
    }

    public static Dinheiro limite(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return new Dinheiro(new BigDecimal(texto));
        } catch (IllegalArgumentException ex) {
            throw new LimiteInvalidoException();
        }
    }

    public static String texto(Saldo saldo) {
        return saldo.valor().toPlainString();
    }

    public static String texto(Dinheiro limite) {
        if (limite == null) {
            return null;
        }
        return limite.valor().toPlainString();
    }
}
