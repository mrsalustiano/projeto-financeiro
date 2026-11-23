package br.com.fincontrol.infrastructure.out.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import br.com.fincontrol.domain.service.RefreshOpacoPort;
import br.com.fincontrol.domain.service.SenhaPort;

class CriptografiaTest {

    @Test
    void argon2ConfereERejeita() {
        SenhaPort senha = new FabricaJwt().senha();
        String hash = senha.hash("segredo");

        assertThat(senha.confere("segredo", hash)).isTrue();
        assertThat(senha.confere("outra", hash)).isFalse();
        assertThat(senha.hashFicticio()).isNotBlank();
    }

    @Test
    void refreshTem256BitsEHashEstavel() {
        RefreshOpacoPort refresh = new FabricaJwt().refreshOpaco();
        String primeiro = refresh.gerar();
        String segundo = refresh.gerar();

        assertThat(primeiro).hasSize(43);
        assertThat(primeiro).isNotEqualTo(segundo);
        assertThat(refresh.hash(primeiro)).hasSize(64).isEqualTo(refresh.hash(primeiro));
        assertThat(new RelogioSistema().agora()).isNotNull();
    }
}
