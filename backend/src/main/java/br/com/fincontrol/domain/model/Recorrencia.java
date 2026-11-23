package br.com.fincontrol.domain.model;

import java.time.Instant;

public record Recorrencia(
        long id,
        long clienteId,
        long contaId,
        long categoriaId,
        Natureza natureza,
        TipoDespesa tipoDespesa,
        Dinheiro valor,
        String descricao,
        DiaDoMes dia,
        boolean ativa,
        Instant pausadaEm,
        Instant criadaEm) {

    public static final int DESCRICAO_MAXIMA = 255;

    public Recorrencia {
        if (natureza == null || valor == null || dia == null || criadaEm == null) {
            throw new IllegalArgumentException("Natureza, valor, dia e criação são obrigatórios.");
        }
        descricao = exigirDescricao(descricao);
        tipoDespesa = tipo(natureza, tipoDespesa);
        if (ativa && pausadaEm != null) {
            throw new IllegalArgumentException("Recorrência ativa não tem pausa.");
        }
    }

    public Recorrencia pausar(Instant quando) {
        if (!ativa) {
            throw new IllegalArgumentException("Recorrência já pausada.");
        }
        return new Recorrencia(id, clienteId, contaId, categoriaId, natureza, tipoDespesa, valor, descricao, dia,
                false, quando, criadaEm);
    }

    public Recorrencia retomar() {
        if (ativa) {
            throw new IllegalArgumentException("Recorrência já ativa.");
        }
        return new Recorrencia(id, clienteId, contaId, categoriaId, natureza, tipoDespesa, valor, descricao, dia,
                true, null, criadaEm);
    }

    private static String exigirDescricao(String descricao) {
        if (descricao == null || descricao.isBlank()) {
            throw new IllegalArgumentException("Descrição obrigatória.");
        }
        String limpa = descricao.trim();
        if (limpa.length() > DESCRICAO_MAXIMA) {
            throw new IllegalArgumentException("Descrição acima do limite.");
        }
        return limpa;
    }

    private static TipoDespesa tipo(Natureza natureza, TipoDespesa tipoDespesa) {
        if (natureza == Natureza.DESPESA && tipoDespesa == null) {
            throw new IllegalArgumentException("Tipo de despesa obrigatório.");
        }
        return natureza == Natureza.RECEITA ? null : tipoDespesa;
    }
}
