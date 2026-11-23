package br.com.fincontrol.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


public record Parcelamento(String grupo, int total, Dinheiro valorTotal, List<ParcelaPlanejada> parcelas) {

    public static final int MINIMO = 1;
    public static final int MAXIMO = 60;

    public Parcelamento {
        if (grupo == null || grupo.isBlank()) {
            throw new IllegalArgumentException("Grupo da parcela obrigatório.");
        }
        if (total < MINIMO || total > MAXIMO) {
            throw new IllegalArgumentException("Parcelamento inválido.");
        }
        if (parcelas == null || parcelas.size() != total) {
            throw new IllegalArgumentException("Parcelamento inválido.");
        }
        parcelas = List.copyOf(parcelas);
    }

    public static Parcelamento planejar(String grupo, Dinheiro valorTotal, int total, LocalDate primeiraData) {
        if (total < MINIMO || total > MAXIMO) {
            throw new IllegalArgumentException("Parcelamento inválido.");
        }
        BigDecimal base = valorTotal.valor().divide(BigDecimal.valueOf(total), 2, RoundingMode.DOWN);
        if (base.signum() <= 0) {
            throw new IllegalArgumentException("Parcelamento inválido.");
        }
        BigDecimal resto = valorTotal.valor().subtract(base.multiply(BigDecimal.valueOf(total)));
        List<ParcelaPlanejada> lista = new ArrayList<>(total);
        for (int numero = 1; numero <= total; numero++) {
            BigDecimal valor = numero == 1 ? base.add(resto) : base;
            lista.add(new ParcelaPlanejada(numero, new Dinheiro(valor), vencimento(primeiraData, numero - 1)));
        }
        return new Parcelamento(grupo, total, valorTotal, lista);
    }

    private static LocalDate vencimento(LocalDate base, int meses) {
        LocalDate mes = base.plusMonths(meses);
        int dia = Math.min(base.getDayOfMonth(), mes.lengthOfMonth());
        return LocalDate.of(mes.getYear(), mes.getMonthValue(), dia);
    }
}
