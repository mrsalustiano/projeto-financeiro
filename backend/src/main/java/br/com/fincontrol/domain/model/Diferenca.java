package br.com.fincontrol.domain.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public final class Diferenca {

    private Diferenca() {
    }

    public static Map<String, Object> entre(Map<String, Object> antes, Map<String, Object> depois) {
        if (antes == null) {
            return depois == null ? Map.of() : depois;
        }
        if (depois == null) {
            return antes;
        }
        Map<String, Object> diff = new LinkedHashMap<>();
        for (String chave : chaves(antes, depois)) {
            Object valorAntes = antes.get(chave);
            Object valorDepois = depois.get(chave);
            if (!Objects.equals(valorAntes, valorDepois)) {
                diff.put(chave, par(valorAntes, valorDepois));
            }
        }
        return diff;
    }

    private static Set<String> chaves(Map<String, Object> antes, Map<String, Object> depois) {
        Set<String> chaves = new TreeSet<>(antes.keySet());
        chaves.addAll(depois.keySet());
        return chaves;
    }

    private static Map<String, Object> par(Object antes, Object depois) {
        Map<String, Object> par = new LinkedHashMap<>();
        par.put("antes", antes);
        par.put("depois", depois);
        return par;
    }
}
