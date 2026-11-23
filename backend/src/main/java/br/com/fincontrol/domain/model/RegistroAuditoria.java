package br.com.fincontrol.domain.model;

import java.time.Instant;

public record RegistroAuditoria(
        long id,
        Long clienteId,
        Long usuarioId,
        String usuarioEmail,
        String usuarioPerfil,
        AcaoAuditoria acao,
        String recurso,
        Long recursoId,
        String dadosAntes,
        String dadosDepois,
        Instant criadoEm) {

    public RegistroAuditoria {
        if (acao == null) {
            throw new IllegalArgumentException("Ação da auditoria obrigatória.");
        }
        if (recurso == null || recurso.isBlank()) {
            throw new IllegalArgumentException("Recurso da auditoria obrigatório.");
        }
        if (criadoEm == null) {
            throw new IllegalArgumentException("Data da auditoria obrigatória.");
        }
        recurso = recurso.trim();
        usuarioEmail = normalizar(usuarioEmail);
        usuarioPerfil = normalizar(usuarioPerfil);
    }

    public boolean deCliente(Long cliente) {
        return clienteId != null && clienteId.equals(cliente);
    }

    private static String normalizar(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }
}
