package br.com.fincontrol.infrastructure.out.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.nimbusds.jwt.SignedJWT;

import br.com.fincontrol.domain.model.Acesso;
import br.com.fincontrol.domain.model.DuracaoSessao;
import br.com.fincontrol.domain.model.Papel;
import br.com.fincontrol.domain.model.SujeitoToken;
import br.com.fincontrol.domain.service.Relogio;

class JwtRs256Test {

    private RelogioMutavel relogio;
    private JwtRs256 jwt;
    private ChavesJwt.Par chaves;

    @BeforeEach
    void setUp() throws Exception {
        relogio = new RelogioMutavel(Instant.parse("2026-11-22T03:00:00Z"));
        chaves = ParDeChaves.gerar();
        jwt = new JwtRs256(chaves, relogio);
    }

    @Test
    void emiteClaimsDoClienteENadaMais() throws Exception {
        var primeiro = jwt.emitir(new SujeitoToken(7L, Papel.ADMIN, 3L));
        var segundo = jwt.emitir(new SujeitoToken(7L, Papel.ADMIN, 3L));
        Acesso acesso = jwt.ler(primeiro.valor()).orElseThrow();
        Set<String> claims = SignedJWT.parse(primeiro.valor()).getJWTClaimsSet().getClaims().keySet();

        assertThat(claims).containsExactlyInAnyOrder("iss", "sub", "role", "clienteId", "jti", "iat", "nbf", "exp");
        assertThat(SignedJWT.parse(primeiro.valor()).getHeader().getAlgorithm().getName()).isEqualTo("RS256");
        assertThat(acesso.usuarioId()).isEqualTo("7");
        assertThat(acesso.papel()).isEqualTo(Papel.ADMIN);
        assertThat(acesso.clienteId()).isEqualTo(3L);
        assertThat(acesso.jti()).isNotEqualTo(segundo.jti());
        assertThat(acesso.expiraEm()).isEqualTo(relogio.agora().plusSeconds(DuracaoSessao.ACCESS_SEGUNDOS));
    }

    @Test
    void masterNaoLevaClienteId() throws Exception {
        var token = jwt.emitir(new SujeitoToken(1L, Papel.MASTER, null));
        Set<String> claims = SignedJWT.parse(token.valor()).getJWTClaimsSet().getClaims().keySet();

        assertThat(claims).doesNotContain("clienteId");
        assertThat(jwt.ler(token.valor()).orElseThrow().clienteId()).isNull();
    }

    @Test
    void recusaAssinaturaTrocaExpiracaoELixo() {
        var token = jwt.emitir(new SujeitoToken(7L, Papel.ADMIN, 3L));
        var outra = new JwtRs256(ParDeChaves.outroPar(), relogio);

        assertThat(outra.ler(token.valor())).isEmpty();
        relogio.avancar(Duration.ofSeconds(DuracaoSessao.ACCESS_SEGUNDOS));
        assertThat(jwt.ler(token.valor())).isEmpty();
        assertThat(jwt.ler("nao.e.jwt")).isEmpty();
        assertThat(jwt.ler(null)).isEmpty();
    }

    @Test
    void aindaValeUmSegundoAntesDeExpirar() {
        var token = jwt.emitir(new SujeitoToken(7L, Papel.LEITOR, 9L));
        relogio.avancar(Duration.ofSeconds(DuracaoSessao.ACCESS_SEGUNDOS - 1));

        assertThat(jwt.ler(token.valor())).isPresent();
    }

    @Test
    void chaveAusenteOuInvalidaFalha() {
        assertThatThrownBy(() -> ChavesJwt.ler(" ", "x")).isInstanceOf(IllegalStateException.class);
        assertThat(ChavesJwt.ler("classpath:jwt/private.pem", "classpath:jwt/public.pem").publica().getAlgorithm())
                .isEqualTo("RSA");
        assertThatThrownBy(() -> ChavesJwt.ler("classpath:jwt/ausente.pem", "classpath:jwt/public.pem"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> ChavesJwt.ler(pem("PRIVATE KEY", new byte[] {1}), pem("PUBLIC KEY", new byte[] {1})))
                .isInstanceOf(IllegalStateException.class);
    }

    private static String pem(String tipo, byte[] corpo) {
        return "-----BEGIN " + tipo + "-----\n" + Base64.getEncoder().encodeToString(corpo) + "\n-----END " + tipo + "-----";
    }

    static final class RelogioMutavel implements Relogio {
        private Instant agora;

        RelogioMutavel(Instant agora) {
            this.agora = agora;
        }

        @Override
        public Instant agora() {
            return agora;
        }

        void avancar(Duration duracao) {
            agora = agora.plus(duracao);
        }
    }

    static final class ParDeChaves {
        static ChavesJwt.Par gerar() {
            return par(2048);
        }

        static ChavesJwt.Par outroPar() {
            return par(2048);
        }

        private static ChavesJwt.Par par(int bits) {
            try {
                KeyPairGenerator gerador = KeyPairGenerator.getInstance("RSA");
                gerador.initialize(bits);
                KeyPair par = gerador.generateKeyPair();
                return ChavesJwt.ler(pem(par.getPrivate().getEncoded(), "PRIVATE KEY"), pem(par.getPublic().getEncoded(), "PUBLIC KEY"));
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }

        private static String pem(byte[] corpo, String tipo) {
            String base64 = Base64.getMimeEncoder(64, new byte[] {'\n'}).encodeToString(corpo);
            return "-----BEGIN " + tipo + "-----\n" + base64 + "\n-----END " + tipo + "-----";
        }
    }
}
