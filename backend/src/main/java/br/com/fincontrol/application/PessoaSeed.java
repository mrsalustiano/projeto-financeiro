package br.com.fincontrol.application;

import java.util.List;

import br.com.fincontrol.domain.model.Papel;

public record PessoaSeed(
        String nome,
        String email,
        String nomeCliente,
        Papel papel,
        boolean usuarioAtivo,
        boolean clienteAtivo,
        String emailDoCliente,
        List<ContaSeed> contas,
        List<LancamentoSeed> lancamentos,
        List<RecorrenciaSeed> recorrencias) {

    public PessoaSeed(
            String nome,
            String email,
            String nomeCliente,
            Papel papel,
            boolean usuarioAtivo,
            boolean clienteAtivo,
            String emailDoCliente,
            List<ContaSeed> contas) {
        this(nome, email, nomeCliente, papel, usuarioAtivo, clienteAtivo, emailDoCliente, contas, List.of(), List.of());
    }

    public PessoaSeed(
            String nome,
            String email,
            String nomeCliente,
            Papel papel,
            boolean usuarioAtivo,
            boolean clienteAtivo) {
        this(nome, email, nomeCliente, papel, usuarioAtivo, clienteAtivo, null, List.of(), List.of(), List.of());
    }

    public PessoaSeed {
        contas = contas == null ? List.of() : List.copyOf(contas);
        lancamentos = lancamentos == null ? List.of() : List.copyOf(lancamentos);
        recorrencias = recorrencias == null ? List.of() : List.copyOf(recorrencias);
    }
}
