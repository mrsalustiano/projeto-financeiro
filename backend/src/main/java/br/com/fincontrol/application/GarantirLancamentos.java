package br.com.fincontrol.application;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fincontrol.domain.model.Categoria;
import br.com.fincontrol.domain.model.Conta;
import br.com.fincontrol.domain.model.Lancamento;
import br.com.fincontrol.domain.service.CategoriaRepositorio;
import br.com.fincontrol.domain.service.ContaRepositorio;
import br.com.fincontrol.domain.service.IdempotenciaRepositorio;
import br.com.fincontrol.domain.service.LancamentoRepositorio;

@Service
public class GarantirLancamentos {

    private final ContaRepositorio contas;
    private final CategoriaRepositorio categorias;
    private final LancamentoRepositorio lancamentos;
    private final IdempotenciaRepositorio idempotencia;

    public GarantirLancamentos(
            ContaRepositorio contas,
            CategoriaRepositorio categorias,
            LancamentoRepositorio lancamentos,
            IdempotenciaRepositorio idempotencia) {
        this.contas = contas;
        this.categorias = categorias;
        this.lancamentos = lancamentos;
        this.idempotencia = idempotencia;
    }

    @Transactional
    public void executar(long clienteId, List<LancamentoSeed> previstos) {
        for (LancamentoSeed previsto : previstos) {
            String chave = chave(previsto);
            if (idempotencia.buscar(clienteId, chave).isPresent()) {
                continue;
            }
            Optional<Conta> conta = contas.listar(clienteId).stream()
                    .filter(item -> item.nome().equals(previsto.conta()))
                    .findFirst();
            Optional<Categoria> categoria = categorias.listar(clienteId).stream()
                    .filter(item -> item.nome().equals(previsto.categoria()))
                    .findFirst();
            if (conta.isEmpty() || categoria.isEmpty()) {
                continue;
            }
            Lancamento novo = new Lancamento(
                    0L,
                    clienteId,
                    conta.get().id(),
                    categoria.get().id(),
                    previsto.natureza(),
                    previsto.tipoDespesa(),
                    previsto.valor(),
                    previsto.data(),
                    previsto.descricao(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null);
            long id = lancamentos.inserir(novo);
            idempotencia.guardar(clienteId, chave, Idempotencia.sha256(chave), id);
        }
    }

    private static String chave(LancamentoSeed previsto) {
        return "seed:lancamento:" + previsto.conta() + ":" + previsto.data() + ":" + previsto.descricao();
    }
}
