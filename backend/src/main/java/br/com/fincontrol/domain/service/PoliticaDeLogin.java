package br.com.fincontrol.domain.service;

import java.time.Instant;

import br.com.fincontrol.domain.model.DecisaoLogin;
import br.com.fincontrol.domain.model.DecisaoLogin.Tipo;
import br.com.fincontrol.domain.model.Usuario;

public final class PoliticaDeLogin {

    public DecisaoLogin avaliar(Usuario usuario, boolean clienteAtivo, boolean senhaConfere, Instant agora) {
        if (usuario.bloqueadoEm(agora)) {
            return new DecisaoLogin(Tipo.BLOQUEADO, usuario, false);
        }
        if (!usuario.ativo() || !clienteAtivo) {
            return new DecisaoLogin(Tipo.RECUSADO, usuario, false);
        }
        if (!senhaConfere) {
            return falha(usuario, agora);
        }
        return new DecisaoLogin(Tipo.OK, usuario.zerarFalhas(), true);
    }

    private static DecisaoLogin falha(Usuario usuario, Instant agora) {
        Usuario atualizado = usuario.registrarFalha(agora);
        Tipo tipo = atualizado.bloqueadoEm(agora) ? Tipo.BLOQUEADO : Tipo.RECUSADO;
        return new DecisaoLogin(tipo, atualizado, true);
    }
}
