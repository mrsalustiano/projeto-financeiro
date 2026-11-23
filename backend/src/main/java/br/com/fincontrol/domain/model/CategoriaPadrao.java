package br.com.fincontrol.domain.model;

public enum CategoriaPadrao {
    SALARIO("Salário", Natureza.RECEITA),
    RENDIMENTOS("Rendimentos", Natureza.RECEITA),
    BENEFICIOS("Benefícios", Natureza.RECEITA),
    ALIMENTACAO("Alimentação", Natureza.DESPESA),
    TRANSPORTE("Transporte", Natureza.DESPESA),
    INVESTIMENTO("Investimento", Natureza.DESPESA);

    private final String nome;
    private final Natureza natureza;

    CategoriaPadrao(String nome, Natureza natureza) {
        this.nome = nome;
        this.natureza = natureza;
    }

    public String nome() {
        return nome;
    }

    public Natureza natureza() {
        return natureza;
    }
}
