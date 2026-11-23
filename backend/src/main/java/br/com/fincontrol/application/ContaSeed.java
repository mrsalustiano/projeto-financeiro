package br.com.fincontrol.application;

import java.time.LocalDate;

import br.com.fincontrol.domain.model.Saldo;
import br.com.fincontrol.domain.model.TipoConta;

public record ContaSeed(String nome, String banco, TipoConta tipo, Saldo saldoInicial, LocalDate dataSaldo) {
}
