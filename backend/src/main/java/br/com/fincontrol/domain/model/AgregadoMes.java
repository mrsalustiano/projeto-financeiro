package br.com.fincontrol.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;

public record AgregadoMes(
        YearMonth mes,
        BigDecimal receitas,
        BigDecimal despesas,
        int lancamentos,
        List<LinhaCategoria> categorias,
        List<LinhaConta> contas) {

    public AgregadoMes {
        if (mes == null) {
            throw new IllegalArgumentException("Mês obrigatório.");
        }
        receitas = exigir(receitas);
        despesas = exigir(despesas);
        categorias = categorias == null ? List.of() : List.copyOf(categorias);
        contas = contas == null ? List.of() : List.copyOf(contas);
    }

    public BigDecimal saldo() {
        return receitas.subtract(despesas).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal exigir(BigDecimal valor) {
        if (valor == null) {
            throw new IllegalArgumentException("Valor obrigatório.");
        }
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    public record LinhaCategoria(String nome, BigDecimal valor) {

        public LinhaCategoria {
            nome = nome == null || nome.isBlank() ? "Sem categoria" : nome.trim();
            valor = exigir(valor);
        }
    }

    public record LinhaConta(String nome, TipoConta tipo, BigDecimal saldo) {

        public LinhaConta {
            if (nome == null || nome.isBlank()) {
                throw new IllegalArgumentException("Nome da conta obrigatório.");
            }
            if (tipo == null) {
                throw new IllegalArgumentException("Tipo da conta obrigatório.");
            }
            nome = nome.trim();
            saldo = exigir(saldo);
        }
    }
}
