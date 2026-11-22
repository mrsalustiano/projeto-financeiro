package br.com.fincontrol.application;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fincontrol.domain.model.Cliente;
import br.com.fincontrol.domain.model.DecisaoLogin;
import br.com.fincontrol.domain.model.Emails;
import br.com.fincontrol.domain.model.Papel;
import br.com.fincontrol.domain.model.Usuario;
import br.com.fincontrol.domain.service.ClienteRepositorio;
import br.com.fincontrol.domain.service.LoginBloqueadoException;
import br.com.fincontrol.domain.service.NaoAutenticadoException;
import br.com.fincontrol.domain.service.PoliticaDeLogin;
import br.com.fincontrol.domain.service.Relogio;
import br.com.fincontrol.domain.service.SenhaPort;
import br.com.fincontrol.domain.service.UsuarioRepositorio;

@Service
public class AutenticarUsuario {

    private final UsuarioRepositorio usuarios;
    private final ClienteRepositorio clientes;
    private final SenhaPort senhas;
    private final Relogio relogio;
    private final EmissorDeSessao emissor;
    private final PoliticaDeLogin politica = new PoliticaDeLogin();

    public AutenticarUsuario(
            UsuarioRepositorio usuarios,
            ClienteRepositorio clientes,
            SenhaPort senhas,
            Relogio relogio,
            EmissorDeSessao emissor) {
        this.usuarios = usuarios;
        this.clientes = clientes;
        this.senhas = senhas;
        this.relogio = relogio;
        this.emissor = emissor;
    }

    @Transactional
    public SessaoEmitida executar(String email, String senha) {
        Optional<Usuario> encontrado = usuarios.buscarPorEmail(Emails.normalizar(email));
        if (encontrado.isEmpty()) {
            senhas.confere(senha, senhas.hashFicticio());
            throw new NaoAutenticadoException();
        }
        return decidir(encontrado.get(), senha);
    }

    private SessaoEmitida decidir(Usuario usuario, String senha) {
        Instant agora = relogio.agora();
        boolean confere = senhas.confere(senha, usuario.senhaHash());
        DecisaoLogin decisao = politica.avaliar(usuario, clienteAtivo(usuario), confere, agora);
        if (decisao.persistir()) {
            usuarios.salvarEstado(decisao.usuario());
        }
        return concluir(decisao);
    }

    private SessaoEmitida concluir(DecisaoLogin decisao) {
        if (decisao.tipo() == DecisaoLogin.Tipo.BLOQUEADO) {
            throw new LoginBloqueadoException();
        }
        if (decisao.tipo() == DecisaoLogin.Tipo.RECUSADO) {
            throw new NaoAutenticadoException();
        }
        return emissor.emitir(decisao.usuario());
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
