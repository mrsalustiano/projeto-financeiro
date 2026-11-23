package br.com.fincontrol.infrastructure.in.web;

public final class Periodos {

    private Periodos() {
    }

    public static int ano(String texto) {
        return ler(texto, 2000, 2100);
    }

    public static int mes(String texto) {
        return ler(texto, 1, 12);
    }

    private static int ler(String texto, int minimo, int maximo) {
        try {
            int valor = Integer.parseInt(texto);
            if (valor < minimo || valor > maximo) {
                throw new IllegalArgumentException("Período inválido.");
            }
            return valor;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Período inválido.", ex);
        }
    }
}
