package br.com.fincontrol.infrastructure.out.security;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public final class ChavesJwt {

    public record Par(RSAPrivateKey privada, RSAPublicKey publica) {
    }

    private ChavesJwt() {
    }

    public static Par ler(String privadaPem, String publicaPem) {
        if (privadaPem == null || privadaPem.isBlank() || publicaPem == null || publicaPem.isBlank()) {
            throw new IllegalStateException("Chaves JWT ausentes.");
        }
        return new Par(privada(privadaPem), publica(publicaPem));
    }

    private static RSAPrivateKey privada(String pem) {
        try {
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(corpo(pem));
            return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (Exception ex) {
            throw new IllegalStateException("Chave privada JWT inválida.", ex);
        }
    }

    private static RSAPublicKey publica(String pem) {
        try {
            X509EncodedKeySpec spec = new X509EncodedKeySpec(corpo(pem));
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception ex) {
            throw new IllegalStateException("Chave pública JWT inválida.", ex);
        }
    }

    private static byte[] corpo(String pem) {
        String base64 = pem
                .replaceAll("-----BEGIN [A-Z ]+-----", "")
                .replaceAll("-----END [A-Z ]+-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(base64);
    }
}
