package br.com.fincontrol.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class Idempotencia {

    private Idempotencia() {
    }

    public static String hash(ComandoLancamento comando) {
        String canonico = String.join("|",
                Long.toString(comando.contaId()),
                Long.toString(comando.categoriaId()),
                comando.natureza().name(),
                comando.tipoDespesa() == null ? "" : comando.tipoDespesa().name(),
                comando.valor().valor().toPlainString(),
                comando.data().toString(),
                comando.descricao() == null ? "" : comando.descricao().trim(),
                Integer.toString(comando.parcelas()));
        return sha256(canonico);
    }

    static String sha256(String texto) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponível.", ex);
        }
    }
}
