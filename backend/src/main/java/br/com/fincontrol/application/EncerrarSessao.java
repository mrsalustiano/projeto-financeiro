package br.com.fincontrol.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fincontrol.domain.model.Acesso;
import br.com.fincontrol.domain.service.NaoAutenticadoException;
import br.com.fincontrol.domain.service.RefreshRepositorio;
import br.com.fincontrol.domain.service.RevogacaoRepositorio;

@Service
public class EncerrarSessao {

    private final RefreshRepositorio refreshRepositorio;
    private final RevogacaoRepositorio revogacao;

    public EncerrarSessao(RefreshRepositorio refreshRepositorio, RevogacaoRepositorio revogacao) {
        this.refreshRepositorio = refreshRepositorio;
        this.revogacao = revogacao;
    }

    @Transactional
    public void executar(Acesso acesso) {
        if (acesso == null) {
            throw new NaoAutenticadoException();
        }
        refreshRepositorio.revogarPorJtiAccess(acesso.jti());
        revogacao.revogar(acesso.jti(), acesso.expiraEm());
    }
}
