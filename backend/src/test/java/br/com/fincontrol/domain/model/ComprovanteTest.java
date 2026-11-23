package br.com.fincontrol.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class ComprovanteTest {

    private static final Instant AGORA = Instant.parse("2026-11-23T12:00:00Z");

    @Test
    void criaComprovanteValido() {
        Comprovante comprovante = new Comprovante(1L, 42L, 7L, "  recibo.pdf  ", TipoComprovante.PDF, 1024L,
                "chave-1", AGORA, "voce");

        assertThat(comprovante.nomeArquivo()).isEqualTo("recibo.pdf");
        assertThat(comprovante.tipo()).isEqualTo(TipoComprovante.PDF);
        assertThat(comprovante.tamanho()).isEqualTo(1024L);
    }

    @Test
    void recusaCamposInvalidos() {
        assertThatThrownBy(() -> new Comprovante(1L, 42L, 7L, " ", TipoComprovante.PDF, 10L, "k", AGORA, "voce"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Comprovante(1L, 42L, 7L, "a.pdf", null, 10L, "k", AGORA, "voce"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Comprovante(1L, 42L, 7L, "a.pdf", TipoComprovante.PDF, 0L, "k", AGORA, "voce"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Comprovante(1L, 42L, 7L, "a.pdf", TipoComprovante.PDF,
                Comprovante.TAMANHO_MAXIMO + 1, "k", AGORA, "voce"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Comprovante(1L, 42L, 7L, "a.pdf", TipoComprovante.PDF, 10L, " ", AGORA, "voce"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Comprovante(1L, 42L, 7L, "a.pdf", TipoComprovante.PDF, 10L, "k", null, "voce"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Comprovante(1L, 42L, 7L, "a.pdf", TipoComprovante.PDF, 10L, "k", AGORA, " "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void detectaTipoPelaAssinatura() {
        assertThat(TipoComprovante.detectar(new byte[] {(byte) 0x89, 'P', 'N', 'G', 0})).contains(TipoComprovante.PNG);
        assertThat(TipoComprovante.detectar(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0}))
                .contains(TipoComprovante.JPEG);
        assertThat(TipoComprovante.detectar("%PDF-1.7".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .contains(TipoComprovante.PDF);
        assertThat(TipoComprovante.detectar(null)).isEmpty();
        assertThat(TipoComprovante.detectar(new byte[0])).isEmpty();
        assertThat(TipoComprovante.detectar(new byte[] {'G', 'I', 'F'})).isEmpty();
        assertThat(TipoComprovante.detectar(new byte[] {(byte) 0x89})).isEmpty();
        assertThat(TipoComprovante.PNG.contentType()).isEqualTo("image/png");
        assertThat(TipoComprovante.JPEG.extensao()).isEqualTo("jpg");
    }
}
