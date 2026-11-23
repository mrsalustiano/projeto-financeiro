package br.com.fincontrol.domain.model;

import java.util.Locale;
import java.util.Optional;

public enum BancoOfx {
    NUBANK("260", "Nubank"),
    BANCO_DO_BRASIL("001", "Banco do Brasil"),
    ITAU("341", "Itaú"),
    BRADESCO("237", "Bradesco"),
    CAIXA("104", "Caixa"),
    SANTANDER("033", "Santander"),
    INTER("077", "Inter"),
    C6("336", "C6");

    private final String codigo;
    private final String nome;

    BancoOfx(String codigo, String nome) {
        this.codigo = codigo;
        this.nome = nome;
    }

    public String codigo() {
        return codigo;
    }

    public String nome() {
        return nome;
    }

    public static Optional<BancoOfx> porCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return Optional.empty();
        }
        String limpo = codigo.trim();
        for (BancoOfx banco : values()) {
            if (mesmoCodigo(banco.codigo, limpo)) {
                return Optional.of(banco);
            }
        }
        return Optional.empty();
    }

    public static Optional<BancoOfx> porNome(String nome) {
        if (nome == null || nome.isBlank()) {
            return Optional.empty();
        }
        String alvo = nome.trim().toUpperCase(Locale.ROOT);
        for (BancoOfx banco : values()) {
            if (banco.nome.toUpperCase(Locale.ROOT).equals(alvo)) {
                return Optional.of(banco);
            }
        }
        return Optional.empty();
    }

    private static boolean mesmoCodigo(String esperado, String informado) {
        try {
            return Integer.parseInt(esperado) == Integer.parseInt(informado);
        } catch (NumberFormatException ex) {
            return esperado.equals(informado);
        }
    }
}
