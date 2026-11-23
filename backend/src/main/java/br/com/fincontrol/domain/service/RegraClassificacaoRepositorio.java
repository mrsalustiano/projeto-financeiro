package br.com.fincontrol.domain.service;

import java.util.List;

import br.com.fincontrol.domain.model.RegraClassificacao;

public interface RegraClassificacaoRepositorio {

    List<RegraClassificacao> listar(long clienteId);

    boolean existe(long clienteId, String termo, int diaInicio, int diaFim);

    long inserir(RegraClassificacao regra);
}
