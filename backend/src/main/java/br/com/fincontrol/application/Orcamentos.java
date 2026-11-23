package br.com.fincontrol.application;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fincontrol.domain.model.Acesso;
import br.com.fincontrol.domain.model.Dinheiro;
import br.com.fincontrol.domain.model.Orcamento;
import br.com.fincontrol.domain.service.CategoriaRepositorio;
import br.com.fincontrol.domain.service.EscopoTenant;
import br.com.fincontrol.domain.service.LancamentoRepositorio;
import br.com.fincontrol.domain.service.NaoEncontradoException;
import br.com.fincontrol.domain.service.OrcamentoRepositorio;

@Service
public class Orcamentos {

    private final OrcamentoRepositorio orcamentos;
    private final CategoriaRepositorio categorias;
    private final LancamentoRepositorio lancamentos;

    public Orcamentos(
            OrcamentoRepositorio orcamentos,
            CategoriaRepositorio categorias,
            LancamentoRepositorio lancamentos) {
        this.orcamentos = orcamentos;
        this.categorias = categorias;
        this.lancamentos = lancamentos;
    }

    @Transactional
    public OrcamentoComAlerta definir(Acesso acesso, long categoriaId, int ano, int mes, Dinheiro limite) {
        long clienteId = EscopoTenant.escrita(acesso);
        if (categorias.buscar(clienteId, categoriaId).isEmpty()) {
            throw new NaoEncontradoException();
        }
        Orcamento orcamento = new Orcamento(0L, clienteId, categoriaId, ano, mes, limite);
        long id = orcamentos.salvar(orcamento);
        return comAlerta(new Orcamento(id, clienteId, categoriaId, ano, mes, limite));
    }

    public List<OrcamentoComAlerta> listar(Acesso acesso, int ano, int mes) {
        long clienteId = EscopoTenant.leitura(acesso);
        List<OrcamentoComAlerta> lista = new ArrayList<>();
        for (Orcamento orcamento : orcamentos.listar(clienteId, ano, mes)) {
            lista.add(comAlerta(orcamento));
        }
        return lista;
    }

    @Transactional
    public void excluir(Acesso acesso, long categoriaId, int ano, int mes) {
        long clienteId = EscopoTenant.escrita(acesso);
        if (!orcamentos.excluir(clienteId, categoriaId, ano, mes)) {
            throw new NaoEncontradoException();
        }
    }

    private OrcamentoComAlerta comAlerta(Orcamento orcamento) {
        YearMonth referencia = YearMonth.of(orcamento.ano(), orcamento.mes());
        LocalDate de = referencia.atDay(1);
        LocalDate ate = referencia.atEndOfMonth();
        return new OrcamentoComAlerta(
                orcamento,
                orcamento.alertar(lancamentos.gastoPorCategoria(
                        orcamento.clienteId(), orcamento.categoriaId(), de, ate)));
    }
}
