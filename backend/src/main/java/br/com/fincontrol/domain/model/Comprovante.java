package br.com.fincontrol.domain.model;

import java.time.Instant;

public record Comprovante(
        long id,
        long clienteId,
        long lancamentoId,
        String nomeArquivo,
        TipoComprovante tipo,
        long tamanho,
        String storageKey,
        Instant criadoEm,
        String criadoPor) {

    public static final long TAMANHO_MAXIMO = 1_048_576L;

    public Comprovante {
        if (nomeArquivo == null || nomeArquivo.isBlank()) {
            throw new IllegalArgumentException("Nome do arquivo obrigatório.");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("Tipo do comprovante obrigatório.");
        }
        if (tamanho <= 0 || tamanho > TAMANHO_MAXIMO) {
            throw new IllegalArgumentException("Tamanho do comprovante inválido.");
        }
        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("Chave de armazenamento obrigatória.");
        }
        if (criadoEm == null) {
            throw new IllegalArgumentException("Data do comprovante obrigatória.");
        }
        if (criadoPor == null || criadoPor.isBlank()) {
            throw new IllegalArgumentException("Responsável pelo comprovante obrigatório.");
        }
        nomeArquivo = nomeArquivo.trim();
    }
}
