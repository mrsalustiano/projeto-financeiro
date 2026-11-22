package br.com.fincontrol.application;

import java.time.Instant;

import org.springframework.stereotype.Component;

import br.com.fincontrol.domain.model.DuracaoSessao;
import br.com.fincontrol.domain.model.SujeitoToken;
import br.com.fincontrol.domain.model.TokenEmitido;
import br.com.fincontrol.domain.model.Usuario;
import br.com.fincontrol.domain.service.RefreshOpacoPort;
import br.com.fincontrol.domain.service.RefreshRepositorio;
import br.com.fincontrol.domain.service.Relogio;
import br.com.fincontrol.domain.service.TokenAcessoPort;

@Component
public class EmissorDeSessao {

    private final TokenAcessoPort tokens;
    private final RefreshOpacoPort refresh;
    private final RefreshRepositorio refreshRepositorio;
    private final Relogio relogio;

    public EmissorDeSessao(
            TokenAcessoPort tokens,
            RefreshOpacoPort refresh,
            RefreshRepositorio refreshRepositorio,
            Relogio relogio) {
        this.tokens = tokens;
        this.refresh = refresh;
        this.refreshRepositorio = refreshRepositorio;
        this.relogio = relogio;
    }

    public SessaoEmitida emitir(Usuario usuario) {
        TokenEmitido token = tokens.emitir(new SujeitoToken(usuario.id(), usuario.papel(), usuario.clienteId()));
        String opaco = refresh.gerar();
        Instant expira = relogio.agora().plus(DuracaoSessao.REFRESH);
        refreshRepositorio.inserir(usuario.id(), refresh.hash(opaco), expira, token.jti(), token.expiraEm());
        return new SessaoEmitida(token.valor(), opaco);
    }
}
