package br.com.fincontrol.infrastructure.out.security;

import java.text.ParseException;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import com.nimbusds.jwt.JWTClaimsSet;

import br.com.fincontrol.domain.model.Acesso;
import br.com.fincontrol.domain.model.DuracaoSessao;
import br.com.fincontrol.domain.model.Papel;
import br.com.fincontrol.domain.service.Relogio;

final class ClaimsJwt {

    private static final Set<String> COM_CLIENTE = Set.of("iss", "sub", "role", "clienteId", "jti", "iat", "nbf", "exp");
    private static final Set<String> SEM_CLIENTE = Set.of("iss", "sub", "role", "jti", "iat", "nbf", "exp");

    private ClaimsJwt() {
    }

    static Acesso ler(JWTClaimsSet claims, Relogio relogio) throws ParseException {
        exigirTempos(claims, relogio);
        Papel papel = Papel.valueOf(claims.getStringClaim("role"));
        Long clienteId = cliente(claims, papel);
        UUID.fromString(claims.getJWTID());
        return new Acesso(claims.getSubject(), papel, clienteId, claims.getJWTID(), claims.getExpirationTime().toInstant());
    }

    private static void exigirTempos(JWTClaimsSet claims, Relogio relogio) {
        long iat = segundos(claims.getIssueTime());
        long nbf = segundos(claims.getNotBeforeTime());
        long exp = segundos(claims.getExpirationTime());
        long agora = relogio.agora().getEpochSecond();
        if (nbf != iat || exp != iat + DuracaoSessao.ACCESS_SEGUNDOS || agora < nbf || agora >= exp) {
            throw new IllegalArgumentException("tempo");
        }
    }

    private static Long cliente(JWTClaimsSet claims, Papel papel) throws ParseException {
        boolean temCliente = claims.getClaims().containsKey("clienteId");
        exigirConjunto(claims, temCliente);
        exigirPapel(papel, temCliente);
        exigirSujeito(claims);
        if (!temCliente) {
            return null;
        }
        return Long.valueOf(claims.getStringClaim("clienteId"));
    }

    private static void exigirConjunto(JWTClaimsSet claims, boolean temCliente) {
        Set<String> esperadas = temCliente ? COM_CLIENTE : SEM_CLIENTE;
        if (!claims.getClaims().keySet().equals(esperadas)) {
            throw new IllegalArgumentException("claims");
        }
    }

    private static void exigirPapel(Papel papel, boolean temCliente) {
        if (papel == Papel.MASTER && temCliente || papel != Papel.MASTER && !temCliente) {
            throw new IllegalArgumentException("papel");
        }
    }

    private static void exigirSujeito(JWTClaimsSet claims) {
        if (!JwtRs256.EMISSOR.equals(claims.getIssuer()) || claims.getSubject() == null || claims.getSubject().isBlank()) {
            throw new IllegalArgumentException("sujeito");
        }
    }

    private static long segundos(Date data) {
        if (data == null) {
            throw new IllegalArgumentException("data");
        }
        return data.toInstant().getEpochSecond();
    }
}
