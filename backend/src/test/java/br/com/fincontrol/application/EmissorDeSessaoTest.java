package br.com.fincontrol.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fincontrol.domain.model.DuracaoSessao;
import br.com.fincontrol.domain.model.Papel;
import br.com.fincontrol.domain.model.SujeitoToken;
import br.com.fincontrol.domain.model.TokenEmitido;
import br.com.fincontrol.domain.model.Usuario;
import br.com.fincontrol.domain.service.RefreshOpacoPort;
import br.com.fincontrol.domain.service.RefreshRepositorio;
import br.com.fincontrol.domain.service.Relogio;
import br.com.fincontrol.domain.service.TokenAcessoPort;

@ExtendWith(MockitoExtension.class)
class EmissorDeSessaoTest {

    @Mock private TokenAcessoPort tokens;
    @Mock private RefreshOpacoPort refresh;
    @Mock private RefreshRepositorio refreshRepositorio;
    @Mock private Relogio relogio;

    @Test
    void gravaOHashENaoOOpaco() {
        Instant agora = Instant.parse("2026-11-22T03:00:00Z");
        Usuario usuario = new Usuario(7L, 42L, "Voce", "voce@fincontrol.local", "hash", Papel.ADMIN, true, 0, null);
        when(relogio.agora()).thenReturn(agora);
        when(tokens.emitir(new SujeitoToken(7L, Papel.ADMIN, 42L)))
                .thenReturn(new TokenEmitido("jwt", "jti", agora, agora.plusSeconds(900)));
        when(refresh.gerar()).thenReturn("opaco");
        when(refresh.hash("opaco")).thenReturn("hash");

        SessaoEmitida sessao = new EmissorDeSessao(tokens, refresh, refreshRepositorio, relogio).emitir(usuario);

        assertThat(sessao).isEqualTo(new SessaoEmitida("jwt", "opaco"));
        verify(refreshRepositorio).inserir(7L, "hash", agora.plus(DuracaoSessao.REFRESH), "jti", agora.plusSeconds(900));
    }
}
