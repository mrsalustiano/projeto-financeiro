package br.com.fincontrol.domain.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import br.com.fincontrol.domain.model.FiltroLancamento;
import br.com.fincontrol.domain.model.Lancamento;
import br.com.fincontrol.domain.model.Parcelamento;
import br.com.fincontrol.domain.model.SaldoConta;

public interface LancamentoRepositorio {

    long inserir(Lancamento lancamento);

    long inserirParcela(long clienteId, Parcelamento parcelamento);

    Optional<Lancamento> buscar(long clienteId, long id);

    List<Lancamento> listar(long clienteId, FiltroLancamento filtro);

    List<Lancamento> listarPorParcela(long clienteId, long parcelaId);

    boolean existeFitid(long clienteId, long contaId, String fitid);

    void atualizar(Lancamento lancamento);

    boolean excluir(long clienteId, long id);

    Optional<SaldoConta> saldo(long clienteId, long contaId);

    BigDecimal gastoPorCategoria(long clienteId, long categoriaId, LocalDate de, LocalDate ate);
}
