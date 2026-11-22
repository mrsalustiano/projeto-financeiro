package br.com.fincontrol.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fincontrol.domain.model.Acesso;
import br.com.fincontrol.domain.service.NaoAutenticadoException;
import br.com.fincontrol.domain.service.RefreshOpacoPort;
import br.com.fincontrol.domain.service.RefreshRepositorio;
import br.com.fincontrol.domain.service.RevogacaoRepositorio;

@Service
public class EncerrarSessao {

    private final RefreshRepositorio refreshRepositorio;
    private final RefreshOpacoPort refresh;
    private final RevogacaoRepositorio revogacao;

    public EncerrarSessao(
            RefreshRepositorio refreshRepositorio,
            RefreshOpacoPort refresh,
            RevogacaoRepositorio revogacao) {
        this.refreshRepositorio = refreshRepositorio;
        this.refresh = refresh;
        this.revogacao = revogacao;
    }

    @Transactional
    public void executar(Acesso acesso, String opaco) {
        if (acesso == null) {
            throw new NaoAutenticadoException();
        }
        revogarRefresh(opaco);
        revogacao.revogar(acesso.jti(), acesso.expiraEm());
    }

    private void revogarRefresh(String opaco) {
        if (opaco == null || opaco.isBlank()) {
            return;
        }
        refreshRepositorio.buscarPorHash(refresh.hash(opaco)).ifPresent(guardado -> refreshRepositorio.revogar(guardado.id()));
    }
}
