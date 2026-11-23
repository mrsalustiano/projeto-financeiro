package br.com.fincontrol.application;

import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fincontrol.domain.model.Acesso;
import br.com.fincontrol.domain.model.Conciliacao;
import br.com.fincontrol.domain.model.Lancamento;
import br.com.fincontrol.domain.service.ConciliacaoInvalidaException;
import br.com.fincontrol.domain.service.ConciliacaoRepetidaException;
import br.com.fincontrol.domain.service.ConciliacaoRepositorio;
import br.com.fincontrol.domain.service.EscopoTenant;
import br.com.fincontrol.domain.service.FechamentoMesRepositorio;
import br.com.fincontrol.domain.service.LancamentoRepositorio;
import br.com.fincontrol.domain.service.MesFechadoException;
import br.com.fincontrol.domain.service.NaoEncontradoException;
import br.com.fincontrol.domain.service.Relogio;

@Service
public class Conciliacoes {

    private final ConciliacaoRepositorio conciliacoes;
    private final LancamentoRepositorio lancamentos;
    private final FechamentoMesRepositorio fechamentos;
    private final Relogio relogio;

    public Conciliacoes(
            ConciliacaoRepositorio conciliacoes,
            LancamentoRepositorio lancamentos,
            FechamentoMesRepositorio fechamentos,
            Relogio relogio) {
        this.conciliacoes = conciliacoes;
        this.lancamentos = lancamentos;
        this.fechamentos = fechamentos;
        this.relogio = relogio;
    }

    public List<Lancamento> sugerir(Acesso acesso, long lancamentoId) {
        long clienteId = EscopoTenant.leitura(acesso);
        Lancamento alvo = lancamentos.buscar(clienteId, lancamentoId).orElseThrow(NaoEncontradoException::new);
        return lancamentos.sugerirPar(clienteId, alvo);
    }

    @Transactional
    public Conciliacao conciliar(Acesso acesso, long lancamentoId, long extratoId) {
        long clienteId = EscopoTenant.escrita(acesso);
        Lancamento lancamento = lancamentos.buscar(clienteId, lancamentoId).orElseThrow(NaoEncontradoException::new);
        Lancamento extrato = lancamentos.buscar(clienteId, extratoId).orElseThrow(NaoEncontradoException::new);
        exigirMesAberto(clienteId, lancamento);
        exigirMesAberto(clienteId, extrato);
        exigirPar(lancamento, extrato);
        exigirNaoConciliado(clienteId, lancamentoId);
        exigirNaoConciliado(clienteId, extratoId);
        Conciliacao nova = new Conciliacao(0L, clienteId, lancamento.contaId(), lancamentoId, extratoId,
                relogio.agora(), acesso.usuarioId());
        long id = conciliacoes.inserir(nova);
        return new Conciliacao(id, nova.clienteId(), nova.contaId(), nova.lancamentoId(), nova.extratoId(),
                nova.conciliadoEm(), nova.conciliadoPor());
    }

    @Transactional
    public void desfazer(Acesso acesso, long id) {
        long clienteId = EscopoTenant.escrita(acesso);
        Conciliacao atual = conciliacoes.buscar(clienteId, id).orElseThrow(NaoEncontradoException::new);
        exigirMesAberto(clienteId, atual);
        if (!conciliacoes.excluir(clienteId, id)) {
            throw new NaoEncontradoException();
        }
    }

    private static void exigirPar(Lancamento lancamento, Lancamento extrato) {
        if (lancamento == null || extrato == null || lancamento.id() == extrato.id()) {
            throw new ConciliacaoInvalidaException();
        }
        exigirMesmoPar(lancamento, extrato);
        exigirLadosDistintos(lancamento, extrato);
    }

    private static void exigirMesmoPar(Lancamento lancamento, Lancamento extrato) {
        boolean mesmoPar = lancamento.clienteId() == extrato.clienteId()
                && lancamento.contaId() == extrato.contaId()
                && lancamento.valor().valor().compareTo(extrato.valor().valor()) == 0
                && lancamento.natureza() == extrato.natureza()
                && Math.abs(ChronoUnit.DAYS.between(lancamento.data(), extrato.data())) <= Conciliacao.JANELA_DIAS;
        if (!mesmoPar) {
            throw new ConciliacaoInvalidaException();
        }
    }

    private static void exigirLadosDistintos(Lancamento lancamento, Lancamento extrato) {
        boolean umLadoComFitid = (lancamento.fitid() == null) != (extrato.fitid() == null);
        if (!umLadoComFitid || lancamento.estornado() || extrato.estornado()) {
            throw new ConciliacaoInvalidaException();
        }
    }

    private void exigirNaoConciliado(long clienteId, long lancamentoId) {
        if (conciliacoes.buscarPorLancamento(clienteId, lancamentoId).isPresent()) {
            throw new ConciliacaoRepetidaException();
        }
    }

    private void exigirMesAberto(long clienteId, Lancamento lancamento) {
        if (fechamentos.buscar(clienteId, lancamento.data().getYear(), lancamento.data().getMonthValue()).isPresent()) {
            throw new MesFechadoException();
        }
    }

    private void exigirMesAberto(long clienteId, Conciliacao conciliacao) {
        lancamentos.buscar(clienteId, conciliacao.lancamentoId())
                .ifPresent(lancamento -> exigirMesAberto(clienteId, lancamento));
    }
}
