package br.com.fincontrol.application;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fincontrol.domain.model.Acesso;
import br.com.fincontrol.domain.model.AcaoAuditoria;
import br.com.fincontrol.domain.model.Conta;
import br.com.fincontrol.domain.model.Dinheiro;
import br.com.fincontrol.domain.model.Saldo;
import br.com.fincontrol.domain.model.TipoConta;
import br.com.fincontrol.domain.service.ContaRepositorio;
import br.com.fincontrol.domain.service.EscopoTenant;
import br.com.fincontrol.domain.service.LimiteInvalidoException;
import br.com.fincontrol.domain.service.NaoEncontradoException;
import br.com.fincontrol.domain.service.NomeEmUsoException;

@Service
public class ContasEscrita {

    private final ContaRepositorio contas;

    public ContasEscrita(ContaRepositorio contas) {
        this.contas = contas;
    }

    @Transactional
    @Auditavel(acao = AcaoAuditoria.CREATE, recurso = "conta")
    public Conta criar(Acesso acesso, String nome, String banco, TipoConta tipo, Saldo saldo, LocalDate data, Dinheiro limite) {
        long clienteId = EscopoTenant.escrita(acesso);
        Conta nova = montar(0L, clienteId, nome, banco, tipo, saldo, data, limite);
        exigirNomeLivre(clienteId, nova.nome());
        long id = contas.inserir(nova);
        return comId(nova, id);
    }

    @Transactional
    @Auditavel(acao = AcaoAuditoria.UPDATE, recurso = "conta")
    public Conta atualizar(Acesso acesso, long id, String nome, String banco, TipoConta tipo, Saldo saldo, LocalDate data, Dinheiro limite) {
        long clienteId = EscopoTenant.escrita(acesso);
        Conta atual = contas.buscar(clienteId, id).orElseThrow(NaoEncontradoException::new);
        Conta nova = montar(id, clienteId, nome, banco, tipo, saldo, data, limite);
        if (!atual.nome().equals(nova.nome())) {
            exigirNomeLivre(clienteId, nova.nome());
        }
        ContextoAuditoria.antes(atual);
        contas.atualizar(nova);
        return nova;
    }

    @Transactional
    @Auditavel(acao = AcaoAuditoria.DELETE, recurso = "conta")
    public void excluir(Acesso acesso, long id) {
        long clienteId = EscopoTenant.escrita(acesso);
        Conta atual = contas.buscar(clienteId, id).orElseThrow(NaoEncontradoException::new);
        ContextoAuditoria.antes(atual);
        if (!contas.excluir(clienteId, id)) {
            throw new NaoEncontradoException();
        }
    }

    private static Conta montar(long id, long clienteId, String nome, String banco, TipoConta tipo, Saldo saldo, LocalDate data, Dinheiro limite) {
        try {
            return new Conta(id, clienteId, nome.trim(), banco.trim(), tipo, saldo, data, limite);
        } catch (IllegalArgumentException ex) {
            throw new LimiteInvalidoException();
        }
    }

    private static Conta comId(Conta conta, long id) {
        return new Conta(id, conta.clienteId(), conta.nome(), conta.banco(), conta.tipo(), conta.saldoInicial(), conta.dataSaldo(), conta.limite());
    }

    private void exigirNomeLivre(long clienteId, String nome) {
        if (contas.existeNome(clienteId, nome)) {
            throw new NomeEmUsoException();
        }
    }
}
