package br.com.fincontrol.application;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fincontrol.domain.model.Categoria;
import br.com.fincontrol.domain.model.Conta;
import br.com.fincontrol.domain.model.DiaDoMes;
import br.com.fincontrol.domain.model.Recorrencia;
import br.com.fincontrol.domain.service.CategoriaRepositorio;
import br.com.fincontrol.domain.service.ContaRepositorio;
import br.com.fincontrol.domain.service.RecorrenciaRepositorio;
import br.com.fincontrol.domain.service.Relogio;

@Service
public class GarantirRecorrencias {

    private final ContaRepositorio contas;
    private final CategoriaRepositorio categorias;
    private final RecorrenciaRepositorio recorrencias;
    private final Relogio relogio;

    public GarantirRecorrencias(
            ContaRepositorio contas,
            CategoriaRepositorio categorias,
            RecorrenciaRepositorio recorrencias,
            Relogio relogio) {
        this.contas = contas;
        this.categorias = categorias;
        this.recorrencias = recorrencias;
        this.relogio = relogio;
    }

    @Transactional
    public void executar(long clienteId, List<RecorrenciaSeed> previstas) {
        for (RecorrenciaSeed prevista : previstas) {
            if (recorrencias.existeDescricao(clienteId, prevista.descricao())) {
                continue;
            }
            Optional<Conta> conta = contas.listar(clienteId).stream()
                    .filter(item -> item.nome().equals(prevista.conta()))
                    .findFirst();
            Optional<Categoria> categoria = categorias.listar(clienteId).stream()
                    .filter(item -> item.nome().equals(prevista.categoria()))
                    .findFirst();
            if (conta.isEmpty() || categoria.isEmpty()) {
                continue;
            }
            recorrencias.inserir(new Recorrencia(
                    0L,
                    clienteId,
                    conta.get().id(),
                    categoria.get().id(),
                    prevista.natureza(),
                    prevista.tipoDespesa(),
                    prevista.valor(),
                    prevista.descricao(),
                    new DiaDoMes(prevista.diaDoMes()),
                    true,
                    null,
                    relogio.agora()));
        }
    }
}
