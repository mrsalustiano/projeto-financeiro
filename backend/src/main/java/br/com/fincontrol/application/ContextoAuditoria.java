package br.com.fincontrol.application;

public final class ContextoAuditoria {

    private static final ThreadLocal<Object> ANTES = new ThreadLocal<>();

    private ContextoAuditoria() {
    }

    public static void antes(Object valor) {
        ANTES.set(valor);
    }

    public static Object consumirAntes() {
        Object valor = ANTES.get();
        ANTES.remove();
        return valor;
    }

    public static void limpar() {
        ANTES.remove();
    }
}
