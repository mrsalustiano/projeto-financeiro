package br.com.fincontrol.application;

import java.util.List;

import org.springframework.stereotype.Service;

import br.com.fincontrol.domain.model.Acesso;
import br.com.fincontrol.domain.model.FiltroLancamento;
import br.com.fincontrol.domain.model.Lancamento;
import br.com.fincontrol.domain.model.SaldoConta;
import br.com.fincontrol.domain.service.ContaRepositorio;
import br.com.fincontrol.domain.service.EscopoTenant;
import br.com.fincontrol.domain.service.LancamentoRepositorio;
import br.com.fincontrol.domain.service.NaoEncontradoException;

@Service
public class LancamentosConsulta {

    private final LancamentoRepositorio lancamentos;
    private final ContaRepositorio contas;

    public LancamentosConsulta(LancamentoRepositorio lancamentos, ContaRepositorio contas) {
        this.lancamentos = lancamentos;
        this.contas = contas;
    }

    public List<Lancamento> listar(Acesso acesso, FiltroLancamento filtro) {
        return lancamentos.listar(EscopoTenant.leitura(acesso), filtro);
    }

    public Lancamento obter(Acesso acesso, long id) {
        long clienteId = EscopoTenant.leitura(acesso);
        return lancamentos.buscar(clienteId, id).orElseThrow(NaoEncontradoException::new);
    }

    public SaldoConta saldo(Acesso acesso, long contaId) {
        long clienteId = EscopoTenant.leitura(acesso);
        if (contas.buscar(clienteId, contaId).isEmpty()) {
            throw new NaoEncontradoException();
        }
        return lancamentos.saldo(clienteId, contaId).orElseThrow(NaoEncontradoException::new);
    }
}
