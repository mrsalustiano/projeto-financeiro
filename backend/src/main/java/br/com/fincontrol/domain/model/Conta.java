package br.com.fincontrol.domain.model;

import java.time.LocalDate;

public record Conta(
        long id,
        long clienteId,
        String nome,
        String banco,
        TipoConta tipo,
        Saldo saldoInicial,
        LocalDate dataSaldo,
        Dinheiro limite) {

    public Conta {
        validarLimite(tipo, limite);
    }

    public static void validarLimite(TipoConta tipo, Dinheiro limite) {
        if (tipo == TipoConta.CARTAO_CREDITO) {
            exigirLimite(limite);
            return;
        }
        rejeitarLimite(limite);
    }

    private static void exigirLimite(Dinheiro limite) {
        if (limite == null) {
            throw new IllegalArgumentException("Cartão exige limite.");
        }
    }

    private static void rejeitarLimite(Dinheiro limite) {
        if (limite != null) {
            throw new IllegalArgumentException("Só cartão de crédito tem limite.");
        }
    }
}
