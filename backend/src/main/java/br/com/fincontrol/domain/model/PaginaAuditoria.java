package br.com.fincontrol.domain.model;

import java.util.List;

public record PaginaAuditoria(List<RegistroAuditoria> itens, Long proximoCursor) {

    public PaginaAuditoria {
        itens = itens == null ? List.of() : List.copyOf(itens);
    }

    public static PaginaAuditoria vazia() {
        return new PaginaAuditoria(List.of(), null);
    }
}
