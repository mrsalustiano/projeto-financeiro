package br.com.fincontrol.infrastructure.out.security;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import br.com.fincontrol.domain.model.Acesso;
import br.com.fincontrol.domain.model.DuracaoSessao;
import br.com.fincontrol.domain.model.Papel;
import br.com.fincontrol.domain.model.SujeitoToken;
import br.com.fincontrol.domain.model.TokenEmitido;
import br.com.fincontrol.domain.service.Relogio;
import br.com.fincontrol.domain.service.TokenAcessoPort;

public final class JwtRs256 implements TokenAcessoPort {

    static final String EMISSOR = "fincontrol";

    private final ChavesJwt.Par chaves;
    private final Relogio relogio;

    public JwtRs256(ChavesJwt.Par chaves, Relogio relogio) {
        this.chaves = chaves;
        this.relogio = relogio;
    }

    @Override
    public TokenEmitido emitir(SujeitoToken sujeito) {
        Instant emitido = relogio.agora();
        Instant expira = emitido.plusSeconds(DuracaoSessao.ACCESS_SEGUNDOS);
        String jti = UUID.randomUUID().toString();
        SignedJWT jwt = new SignedJWT(cabecalho(), claims(sujeito, jti, emitido, expira));
        assinar(jwt);
        return new TokenEmitido(serializar(jwt), jti, emitido, expira);
    }

    @Override
    public Optional<Acesso> ler(String jwt) {
        try {
            return Optional.of(validar(SignedJWT.parse(jwt)));
        } catch (JOSEException | ParseException | RuntimeException ex) {
            return Optional.empty();
        }
    }

    private static JWSHeader cabecalho() {
        return new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).build();
    }

    private static JWTClaimsSet claims(SujeitoToken sujeito, String jti, Instant emitido, Instant expira) {
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .issuer(EMISSOR)
                .subject(Long.toString(sujeito.usuarioId()))
                .claim("role", sujeito.papel().name())
                .jwtID(jti)
                .issueTime(Date.from(emitido))
                .notBeforeTime(Date.from(emitido))
                .expirationTime(Date.from(expira));
        if (sujeito.clienteId() != null) {
            builder.claim("clienteId", Long.toString(sujeito.clienteId()));
        }
        return builder.build();
    }

    private void assinar(SignedJWT jwt) {
        try {
            jwt.sign(new RSASSASigner(chaves.privada()));
        } catch (JOSEException ex) {
            throw new IllegalStateException("Falha ao assinar o token.", ex);
        }
    }

    private static String serializar(SignedJWT jwt) {
        try {
            return jwt.serialize();
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao serializar o token.", ex);
        }
    }

    private Acesso validar(SignedJWT jwt) throws JOSEException, ParseException {
        exigirCabecalho(jwt);
        if (!jwt.verify(new RSASSAVerifier(chaves.publica()))) {
            throw new IllegalArgumentException("assinatura");
        }
        return ClaimsJwt.ler(jwt.getJWTClaimsSet(), relogio);
    }

    private static void exigirCabecalho(SignedJWT jwt) {
        if (!JWSAlgorithm.RS256.equals(jwt.getHeader().getAlgorithm())) {
            throw new IllegalArgumentException("alg");
        }
        if (!JOSEObjectType.JWT.equals(jwt.getHeader().getType())) {
            throw new IllegalArgumentException("typ");
        }
    }

}
