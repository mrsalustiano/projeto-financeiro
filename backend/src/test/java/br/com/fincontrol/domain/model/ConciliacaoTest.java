package br.com.fincontrol.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class ConciliacaoTest {

    private static final Instant AGORA = Instant.parse("2026-11-23T12:00:00Z");

    @Test
    void criaRegistroValido() {
        Conciliacao conciliacao = new Conciliacao(5L, 42L, 10L, 1L, 2L, AGORA, "  voce  ");

        assertThat(conciliacao.lancamentoId()).isEqualTo(1L);
        assertThat(conciliacao.extratoId()).isEqualTo(2L);
        assertThat(conciliacao.conciliadoPor()).isEqualTo("voce");
        assertThat(conciliacao.envolve(1L)).isTrue();
        assertThat(conciliacao.envolve(2L)).isTrue();
        assertThat(conciliacao.envolve(3L)).isFalse();
    }

    @Test
    void validaRegistro() {
        assertThatThrownBy(() -> new Conciliacao(1L, 0L, 10L, 1L, 2L, AGORA, "voce"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Conciliacao(1L, 42L, 10L, 0L, 2L, AGORA, "voce"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Conciliacao(1L, 42L, 10L, 1L, 1L, AGORA, "voce"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Conciliacao(1L, 42L, 10L, 1L, 2L, null, "voce"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Conciliacao(1L, 42L, 10L, 1L, 2L, AGORA, " "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
