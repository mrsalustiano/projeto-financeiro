package br.com.fincontrol.domain.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Mascaramento {

    public static final String OCULTO = "***";

    private static final List<String> SENSIVEIS =
            List.of("senha", "password", "hash", "token", "secret", "authorization", "credencial");

    private Mascaramento() {
    }

    public static Map<String, Object> mascarar(Map<String, Object> dados) {
        if (dados == null) {
            return Map.of();
        }
        Map<String, Object> saida = new LinkedHashMap<>();
        dados.forEach((chave, valor) -> saida.put(chave, sensivel(chave) ? OCULTO : mascararValor(valor)));
        return saida;
    }

    public static boolean sensivel(String chave) {
        if (chave == null) {
            return false;
        }
        String normalizada = chave.toLowerCase(Locale.ROOT);
        return SENSIVEIS.stream().anyMatch(normalizada::contains);
    }

    private static Object mascararValor(Object valor) {
        if (valor instanceof Map<?, ?> mapa) {
            return mascarar(textos(mapa));
        }
        if (valor instanceof List<?> lista) {
            return lista.stream().map(Mascaramento::mascararValor).toList();
        }
        return valor;
    }

    private static Map<String, Object> textos(Map<?, ?> mapa) {
        Map<String, Object> saida = new LinkedHashMap<>();
        mapa.forEach((chave, valor) -> saida.put(String.valueOf(chave), valor));
        return saida;
    }
}
