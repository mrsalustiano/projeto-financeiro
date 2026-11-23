package br.com.fincontrol.domain.model;

import java.time.Instant;

public record Conciliacao(
        long id,
        long clienteId,
        long contaId,
        long lancamentoId,
        long extratoId,
        Instant conciliadoEm,
        String conciliadoPor) {

    public static final int JANELA_DIAS = 5;

    public Conciliacao {
        if (clienteId <= 0 || contaId <= 0 || lancamentoId <= 0 || extratoId <= 0) {
            throw new IllegalArgumentException("Identificadores inválidos.");
        }
        if (lancamentoId == extratoId) {
            throw new IllegalArgumentException("Lançamento e extrato devem ser diferentes.");
        }
        if (conciliadoEm == null) {
            throw new IllegalArgumentException("Data da conciliação obrigatória.");
        }
        if (conciliadoPor == null || conciliadoPor.isBlank()) {
            throw new IllegalArgumentException("Autor da conciliação obrigatório.");
        }
        conciliadoPor = conciliadoPor.trim();
    }

    public boolean envolve(long lancamento) {
        return lancamentoId == lancamento || extratoId == lancamento;
    }
}
