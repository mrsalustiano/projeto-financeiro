package br.com.fincontrol.application;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fincontrol.domain.model.Acesso;
import br.com.fincontrol.domain.model.DiaDoMes;
import br.com.fincontrol.domain.model.Dinheiro;
import br.com.fincontrol.domain.model.FaixaBeneficio;
import br.com.fincontrol.domain.model.Lancamento;
import br.com.fincontrol.domain.model.Natureza;
import br.com.fincontrol.domain.model.Recorrencia;
import br.com.fincontrol.domain.model.TipoDespesa;
import br.com.fincontrol.domain.service.CategoriaRepositorio;
import br.com.fincontrol.domain.service.ContaRepositorio;
import br.com.fincontrol.domain.service.EscopoTenant;
import br.com.fincontrol.domain.service.FechamentoMesRepositorio;
import br.com.fincontrol.domain.service.LancamentoRepositorio;
import br.com.fincontrol.domain.service.NaoEncontradoException;
import br.com.fincontrol.domain.service.NomeEmUsoException;
import br.com.fincontrol.domain.service.RecorrenciaInativaException;
import br.com.fincontrol.domain.service.RecorrenciaRepositorio;
import br.com.fincontrol.domain.service.Relogio;

@Service
public class Recorrencias {

    private final RecorrenciaRepositorio recorrencias;
    private final LancamentoRepositorio lancamentos;
    private final ContaRepositorio contas;
    private final CategoriaRepositorio categorias;
    private final FechamentoMesRepositorio fechamentos;
    private final Relogio relogio;

    public Recorrencias(
            RecorrenciaRepositorio recorrencias,
            LancamentoRepositorio lancamentos,
            ContaRepositorio contas,
            CategoriaRepositorio categorias,
            FechamentoMesRepositorio fechamentos,
            Relogio relogio) {
        this.recorrencias = recorrencias;
        this.lancamentos = lancamentos;
        this.contas = contas;
        this.categorias = categorias;
        this.fechamentos = fechamentos;
        this.relogio = relogio;
    }

    @Transactional
    public Recorrencia criar(Acesso acesso, ComandoRecorrencia comando) {
        long clienteId = EscopoTenant.escrita(acesso);
        if (contas.buscar(clienteId, comando.contaId()).isEmpty()
                || categorias.buscar(clienteId, comando.categoriaId()).isEmpty()) {
            throw new NaoEncontradoException();
        }
        if (recorrencias.existeDescricao(clienteId, comando.descricao() == null ? "" : comando.descricao().trim())) {
            throw new NomeEmUsoException();
        }
        Recorrencia nova = new Recorrencia(
                0L,
                clienteId,
                comando.contaId(),
                comando.categoriaId(),
                comando.natureza(),
                comando.tipoDespesa(),
                comando.valor(),
                comando.descricao(),
                new DiaDoMes(comando.diaDoMes()),
                true,
                null,
                relogio.agora());
        long id = recorrencias.inserir(nova);
        return comId(nova, id);
    }

    public List<Recorrencia> listar(Acesso acesso) {
        return recorrencias.listar(EscopoTenant.leitura(acesso));
    }

    @Transactional
    public Recorrencia pausar(Acesso acesso, long id) {
        long clienteId = EscopoTenant.escrita(acesso);
        Recorrencia atual = recorrencias.buscar(clienteId, id).orElseThrow(NaoEncontradoException::new);
        Recorrencia pausada = pausar(atual);
        recorrencias.atualizar(pausada);
        return pausada;
    }

    @Transactional
    public Recorrencia retomar(Acesso acesso, long id) {
        long clienteId = EscopoTenant.escrita(acesso);
        Recorrencia atual = recorrencias.buscar(clienteId, id).orElseThrow(NaoEncontradoException::new);
        Recorrencia retomada = retomar(atual);
        recorrencias.atualizar(retomada);
        return retomada;
    }

    @Transactional
    public int executarDoDia(LocalDate hoje) {
        int criadas = 0;
        for (Recorrencia recorrencia : recorrencias.listarAtivas()) {
            if (executar(recorrencia, hoje)) {
                criadas++;
            }
        }
        return criadas;
    }

    private boolean executar(Recorrencia recorrencia, LocalDate hoje) {
        YearMonth mes = YearMonth.from(hoje);
        if (!recorrencia.dia().venceEm(mes).equals(hoje)) {
            return false;
        }
        long clienteId = recorrencia.clienteId();
        if (fechamentos.buscar(clienteId, mes.getYear(), mes.getMonthValue()).isPresent()) {
            return false;
        }
        Optional<Long> existente = recorrencias.ocorrencia(clienteId, recorrencia.id(), mes.getYear(), mes.getMonthValue());
        if (existente.isPresent()) {
            ajustar(recorrencia, existente.get());
            return false;
        }
        Lancamento lancamento = lancamentos.buscar(clienteId,
                lancamentos.inserir(lancamentoDe(recorrencia, hoje))).orElseThrow(NaoEncontradoException::new);
        recorrencias.registrarOcorrencia(clienteId, recorrencia.id(), mes.getYear(), mes.getMonthValue(), lancamento.id());
        return true;
    }

    private void ajustar(Recorrencia recorrencia, long lancamentoId) {
        Lancamento atual = lancamentos.buscar(recorrencia.clienteId(), lancamentoId).orElse(null);
        if (atual == null || atual.estornado() || atual.valor().valor().compareTo(recorrencia.valor().valor()) == 0) {
            return;
        }
        if (FaixaBeneficio.FLASH.aceita(recorrencia.valor()) && FaixaBeneficio.FLASH.aceita(atual.valor())) {
            lancamentos.atualizar(atual.comValor(recorrencia.valor()));
        }
    }

    private static Lancamento lancamentoDe(Recorrencia recorrencia, LocalDate data) {
        return new Lancamento(
                0L,
                recorrencia.clienteId(),
                recorrencia.contaId(),
                recorrencia.categoriaId(),
                recorrencia.natureza(),
                recorrencia.tipoDespesa(),
                recorrencia.valor(),
                data,
                recorrencia.descricao(),
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    private Recorrencia pausar(Recorrencia atual) {
        try {
            return atual.pausar(relogio.agora());
        } catch (IllegalArgumentException ex) {
            throw new RecorrenciaInativaException();
        }
    }

    private static Recorrencia retomar(Recorrencia atual) {
        try {
            return atual.retomar();
        } catch (IllegalArgumentException ex) {
            throw new RecorrenciaInativaException();
        }
    }

    private static Recorrencia comId(Recorrencia recorrencia, long id) {
        return new Recorrencia(id, recorrencia.clienteId(), recorrencia.contaId(), recorrencia.categoriaId(),
                recorrencia.natureza(), recorrencia.tipoDespesa(), recorrencia.valor(), recorrencia.descricao(),
                recorrencia.dia(), recorrencia.ativa(), recorrencia.pausadaEm(), recorrencia.criadaEm());
    }
}
