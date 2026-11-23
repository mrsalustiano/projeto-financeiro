package br.com.fincontrol.domain.service;

import java.util.List;
import java.util.Optional;

import br.com.fincontrol.domain.model.Recorrencia;

public interface RecorrenciaRepositorio {

    long inserir(Recorrencia recorrencia);

    Optional<Recorrencia> buscar(long clienteId, long id);

    List<Recorrencia> listar(long clienteId);

    List<Recorrencia> listarAtivas();

    boolean existeDescricao(long clienteId, String descricao);

    void atualizar(Recorrencia recorrencia);

    Optional<Long> ocorrencia(long clienteId, long recorrenciaId, int ano, int mes);

    void registrarOcorrencia(long clienteId, long recorrenciaId, int ano, int mes, long lancamentoId);
}
