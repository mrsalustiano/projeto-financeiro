package br.com.fincontrol.domain.model;

import java.util.Optional;

public enum TipoComprovante {
    PNG("image/png", "png", new byte[] {(byte) 0x89, 'P', 'N', 'G'}),
    JPEG("image/jpeg", "jpg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
    PDF("application/pdf", "pdf", new byte[] {'%', 'P', 'D', 'F'});

    private final String contentType;
    private final String extensao;
    private final byte[] assinatura;

    TipoComprovante(String contentType, String extensao, byte[] assinatura) {
        this.contentType = contentType;
        this.extensao = extensao;
        this.assinatura = assinatura.clone();
    }

    public String contentType() {
        return contentType;
    }

    public String extensao() {
        return extensao;
    }

    public static Optional<TipoComprovante> detectar(byte[] conteudo) {
        if (conteudo == null || conteudo.length == 0) {
            return Optional.empty();
        }
        for (TipoComprovante tipo : values()) {
            if (tipo.comeca(conteudo)) {
                return Optional.of(tipo);
            }
        }
        return Optional.empty();
    }

    private boolean comeca(byte[] conteudo) {
        if (conteudo.length < assinatura.length) {
            return false;
        }
        for (int indice = 0; indice < assinatura.length; indice++) {
            if (conteudo[indice] != assinatura[indice]) {
                return false;
            }
        }
        return true;
    }
}
