package br.com.fincontrol.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import br.com.fincontrol.domain.model.DecisaoLogin.Tipo;
import br.com.fincontrol.domain.model.Papel;
import br.com.fincontrol.domain.model.Usuario;

class PoliticaDeLoginTest {

    private final PoliticaDeLogin politica = new PoliticaDeLogin();
    private final Instant agora = Instant.parse("2026-11-22T03:00:00Z");

    @Test
    void senhaCertaZeraFalhas() {
        var decisao = politica.avaliar(ativo(1), true, true, agora);

        assertThat(decisao.tipo()).isEqualTo(Tipo.OK);
        assertThat(decisao.persistir()).isTrue();
        assertThat(decisao.usuario().falhasLogin()).isZero();
    }

    @Test
    void senhaErradaAindaNaoBloqueia() {
        var decisao = politica.avaliar(ativo(0), true, false, agora);

        assertThat(decisao.tipo()).isEqualTo(Tipo.RECUSADO);
        assertThat(decisao.persistir()).isTrue();
        assertThat(decisao.usuario().falhasLogin()).isEqualTo(1);
    }

    @Test
    void quintaSenhaErradaBloqueia() {
        var decisao = politica.avaliar(ativo(4), true, false, agora);

        assertThat(decisao.tipo()).isEqualTo(Tipo.BLOQUEADO);
        assertThat(decisao.usuario().bloqueadoEm(agora)).isTrue();
    }

    @Test
    void bloqueioAbertoNaoGravaDeNovo() {
        Usuario bloqueado = ativo(0).registrarFalha(agora).registrarFalha(agora).registrarFalha(agora)
                .registrarFalha(agora).registrarFalha(agora);
        var decisao = politica.avaliar(bloqueado, true, true, agora);

        assertThat(decisao.tipo()).isEqualTo(Tipo.BLOQUEADO);
        assertThat(decisao.persistir()).isFalse();
    }

    @Test
    void inativoOuClienteInativoRecusaSemGravar() {
        Usuario inativo = new Usuario(1L, 2L, "Voce", "a@b.com", "hash", Papel.ADMIN, false, 0, null);

        assertThat(politica.avaliar(inativo, true, true, agora).persistir()).isFalse();
        assertThat(politica.avaliar(ativo(0), false, true, agora).tipo()).isEqualTo(Tipo.RECUSADO);
    }

    private static Usuario ativo(int falhas) {
        return new Usuario(1L, 2L, "Voce", "a@b.com", "hash", Papel.ADMIN, true, falhas, null);
    }
}
