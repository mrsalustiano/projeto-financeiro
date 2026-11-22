package br.com.fincontrol.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class UsuarioTest {

    private final Instant agora = Instant.parse("2026-11-22T03:00:00Z");

    @Test
    void bloqueioOlhaOInstante() {
        Usuario livre = usuario(0, null);
        Usuario futuro = usuario(0, agora.plusSeconds(1));
        Usuario passado = usuario(0, agora.minusSeconds(1));

        assertThat(livre.bloqueadoEm(agora)).isFalse();
        assertThat(futuro.bloqueadoEm(agora)).isTrue();
        assertThat(passado.bloqueadoEm(agora)).isFalse();
    }

    @Test
    void quintaFalhaBloqueiaEZerarLimpa() {
        Usuario quarta = usuario(3, null).registrarFalha(agora);
        Usuario quinta = quarta.registrarFalha(agora);

        assertThat(quarta.falhasLogin()).isEqualTo(4);
        assertThat(quarta.bloqueadoAte()).isNull();
        assertThat(quinta.falhasLogin()).isZero();
        assertThat(quinta.bloqueadoAte()).isEqualTo(agora.plus(DuracaoSessao.BLOQUEIO));
        assertThat(quinta.zerarFalhas().falhasLogin()).isZero();
        assertThat(quinta.zerarFalhas().bloqueadoAte()).isNull();
    }

    @Test
    void emailPerdeEspacoEMaiuscula() {
        assertThat(Emails.normalizar("  A@B.COM ")).isEqualTo("a@b.com");
    }

    private static Usuario usuario(int falhas, Instant bloqueio) {
        return new Usuario(1L, 2L, "Voce", "a@b.com", "hash", Papel.ADMIN, true, falhas, bloqueio);
    }
}
