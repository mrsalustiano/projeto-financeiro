package br.com.fincontrol.domain.model;

import java.time.Instant;
import java.time.LocalDate;

public record Lancamento(
        long id,
        long clienteId,
        long contaId,
        long categoriaId,
        Natureza natureza,
        TipoDespesa tipoDespesa,
        Dinheiro valor,
        LocalDate data,
        String descricao,
        Long parcelaId,
        String grupoParcela,
        Integer numeroParcela,
        Integer totalParcelas,
        Instant estornadoEm,
        String estornoMotivo,
        String estornadoPor) {

    public static final int DESCRICAO_MAXIMA = 255;

    public Lancamento {
        if (natureza == null || valor == null || data == null) {
            throw new IllegalArgumentException("Natureza, valor e data são obrigatórios.");
        }
        descricao = exigirDescricao(descricao);
        tipoDespesa = tipo(natureza, tipoDespesa);
        exigirEstorno(estornadoEm, estornoMotivo, estornadoPor);
    }

    public boolean estornado() {
        return estornadoEm != null;
    }

    public Lancamento estornar(String motivo, String por, Instant quando) {
        if (estornado()) {
            throw new IllegalArgumentException("Lançamento já estornado.");
        }
        return new Lancamento(id, clienteId, contaId, categoriaId, natureza, tipoDespesa, valor, data, descricao,
                parcelaId, grupoParcela, numeroParcela, totalParcelas, quando, motivo, por);
    }

    public Lancamento editar(
            long novaContaId,
            long novaCategoriaId,
            Natureza novaNatureza,
            TipoDespesa novoTipoDespesa,
            Dinheiro novoValor,
            LocalDate novaData,
            String novaDescricao) {
        if (estornado()) {
            throw new IllegalArgumentException("Lançamento estornado não pode ser alterado.");
        }
        return new Lancamento(id, clienteId, novaContaId, novaCategoriaId, novaNatureza, novoTipoDespesa, novoValor,
                novaData, novaDescricao, parcelaId, grupoParcela, numeroParcela, totalParcelas, null, null, null);
    }

    public Lancamento comValor(Dinheiro novoValor) {
        if (estornado()) {
            throw new IllegalArgumentException("Lançamento estornado não pode ser alterado.");
        }
        return new Lancamento(id, clienteId, contaId, categoriaId, natureza, tipoDespesa, novoValor, data, descricao,
                parcelaId, grupoParcela, numeroParcela, totalParcelas, null, null, null);
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

    private static void exigirEstorno(Instant estornadoEm, String motivo, String por) {
        if (estornadoEm == null) {
            exigirSemEstorno(motivo, por);
            return;
        }
        exigirComEstorno(motivo, por);
    }

    private static void exigirSemEstorno(String motivo, String por) {
        if (motivo != null || por != null) {
            throw new IllegalArgumentException("Estorno sem data.");
        }
    }

    private static void exigirComEstorno(String motivo, String por) {
        if (vazio(motivo) || vazio(por)) {
            throw new IllegalArgumentException("Motivo e responsável do estorno são obrigatórios.");
        }
    }

    private static boolean vazio(String texto) {
        return texto == null || texto.isBlank();
    }
}
