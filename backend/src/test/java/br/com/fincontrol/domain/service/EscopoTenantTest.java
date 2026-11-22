package br.com.fincontrol.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import br.com.fincontrol.domain.model.Acesso;
import br.com.fincontrol.domain.model.Papel;

class EscopoTenantTest {

    @Test
    void leitorNaoEscreveEMasterNaoEntraNoTenant() {
        Acesso leitor = new Acesso("3", Papel.LEITOR, 42L, "jti", Instant.EPOCH);
        assertThat(EscopoTenant.leitura(leitor)).isEqualTo(42L);
        assertThatThrownBy(() -> EscopoTenant.escrita(leitor)).isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> EscopoTenant.leitura(new Acesso("1", Papel.MASTER, null, "jti", Instant.EPOCH)))
                .isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> EscopoTenant.leitura(null)).isInstanceOf(AcessoNegadoException.class);
    }
}
