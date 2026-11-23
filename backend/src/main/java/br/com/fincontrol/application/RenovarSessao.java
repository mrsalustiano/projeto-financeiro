package br.com.fincontrol.application;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fincontrol.domain.model.Cliente;
import br.com.fincontrol.domain.model.Papel;
import br.com.fincontrol.domain.model.RefreshGuardado;
import br.com.fincontrol.domain.model.Usuario;
import br.com.fincontrol.domain.service.ClienteRepositorio;
import br.com.fincontrol.domain.service.NaoAutenticadoException;
import br.com.fincontrol.domain.service.RefreshOpacoPort;
import br.com.fincontrol.domain.service.RefreshRepositorio;
import br.com.fincontrol.domain.service.Relogio;
import br.com.fincontrol.domain.service.RevogacaoRepositorio;
import br.com.fincontrol.domain.service.UsuarioRepositorio;

@Service
public class RenovarSessao {

    private final RefreshRepositorio refreshRepositorio;
    private final RefreshOpacoPort refresh;
    private final UsuarioRepositorio usuarios;
    private final ClienteRepositorio clientes;
    private final RevogacaoRepositorio revogacao;
    private final Relogio relogio;
    private final EmissorDeSessao emissor;

    public RenovarSessao(
            RefreshRepositorio refreshRepositorio,
            RefreshOpacoPort refresh,
            UsuarioRepositorio usuarios,
            ClienteRepositorio clientes,
            RevogacaoRepositorio revogacao,
            Relogio relogio,
            EmissorDeSessao emissor) {
        this.refreshRepositorio = refreshRepositorio;
        this.refresh = refresh;
        this.usuarios = usuarios;
        this.clientes = clientes;
        this.revogacao = revogacao;
        this.relogio = relogio;
        this.emissor = emissor;
    }

    @Transactional
    public SessaoEmitida executar(String opaco) {
        RefreshGuardado guardado = exigirValido(opaco);
        Usuario usuario = exigirUsuario(guardado.usuarioId());
        refreshRepositorio.revogar(guardado.id());
        revogacao.revogar(guardado.jtiAccess(), guardado.accessExpiraEm());
        return emissor.emitir(usuario);
    }

    private RefreshGuardado exigirValido(String opaco) {
        if (opaco == null || opaco.isBlank()) {
            throw new NaoAutenticadoException();
        }
        RefreshGuardado guardado = refreshRepositorio.buscarPorHash(refresh.hash(opaco))
                .orElseThrow(NaoAutenticadoException::new);
        Instant agora = relogio.agora();
        if (guardado.revogado() || !guardado.expiraEm().isAfter(agora)) {
            throw new NaoAutenticadoException();
        }
        return guardado;
    }

    private Usuario exigirUsuario(long id) {
        Usuario usuario = usuarios.buscarPorId(id).orElseThrow(NaoAutenticadoException::new);
        if (!usuario.ativo() || !clienteAtivo(usuario)) {
            throw new NaoAutenticadoException();
        }
        return usuario;
    }

    private boolean clienteAtivo(Usuario usuario) {
        if (usuario.papel() == Papel.MASTER) {
            return true;
        }
        if (usuario.clienteId() == null) {
            return false;
        }
        return clientes.buscarPorId(usuario.clienteId()).map(Cliente::ativo).orElse(false);
    }
}
