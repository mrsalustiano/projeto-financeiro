package br.com.fincontrol.domain.model;

import java.util.List;

public record AnaliseFinanceira(String resumo, List<String> insights, List<String> acoes) {

    public static final int RESUMO_MAXIMO = 2000;
    public static final int ITENS_MAXIMOS = 10;

    public AnaliseFinanceira {
        resumo = exigirResumo(resumo);
        insights = exigirItens(insights, "Insight");
        acoes = exigirItens(acoes, "Ação");
    }

    private static String exigirResumo(String resumo) {
        if (resumo == null || resumo.isBlank()) {
            throw new IllegalArgumentException("Resumo obrigatório.");
        }
        String limpo = resumo.trim();
        if (limpo.length() > RESUMO_MAXIMO) {
            throw new IllegalArgumentException("Resumo acima do limite.");
        }
        return limpo;
    }

    private static List<String> exigirItens(List<String> itens, String rotulo) {
        if (itens == null || itens.isEmpty()) {
            throw new IllegalArgumentException(rotulo + " obrigatório.");
        }
        if (itens.size() > ITENS_MAXIMOS) {
            throw new IllegalArgumentException(rotulo + " acima do limite.");
        }
        List<String> limpos = itens.stream()
                .filter(item -> item != null && !item.isBlank())
                .map(String::trim)
                .toList();
        if (limpos.isEmpty()) {
            throw new IllegalArgumentException(rotulo + " obrigatório.");
        }
        return limpos;
    }
}
