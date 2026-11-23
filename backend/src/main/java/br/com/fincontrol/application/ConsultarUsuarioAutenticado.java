package br.com.fincontrol.application;

import org.springframework.stereotype.Service;

import br.com.fincontrol.domain.model.Acesso;
import br.com.fincontrol.domain.model.Cliente;
import br.com.fincontrol.domain.model.Papel;
import br.com.fincontrol.domain.model.Usuario;
import br.com.fincontrol.domain.service.ClienteRepositorio;
import br.com.fincontrol.domain.service.NaoAutenticadoException;
import br.com.fincontrol.domain.service.UsuarioRepositorio;

@Service
public class ConsultarUsuarioAutenticado {

    private final UsuarioRepositorio usuarios;
    private final ClienteRepositorio clientes;

    public ConsultarUsuarioAutenticado(UsuarioRepositorio usuarios, ClienteRepositorio clientes) {
        this.usuarios = usuarios;
        this.clientes = clientes;
    }

    public UsuarioVisao executar(Acesso acesso) {
        if (acesso == null) {
            throw new NaoAutenticadoException();
        }
        Usuario usuario = usuarios.buscarPorId(Long.parseLong(acesso.usuarioId()))
                .orElseThrow(NaoAutenticadoException::new);
        if (!usuario.ativo()) {
            throw new NaoAutenticadoException();
        }
        exigirCliente(usuario);
        String clienteId = usuario.clienteId() == null ? null : Long.toString(usuario.clienteId());
        return new UsuarioVisao(Long.toString(usuario.id()), usuario.nome(), usuario.email(), usuario.papel(), clienteId);
    }

    private void exigirCliente(Usuario usuario) {
        if (usuario.papel() == Papel.MASTER) {
            return;
        }
        if (usuario.clienteId() == null) {
            throw new NaoAutenticadoException();
        }
        Cliente cliente = clientes.buscarPorId(usuario.clienteId()).orElseThrow(NaoAutenticadoException::new);
        if (!cliente.ativo()) {
            throw new NaoAutenticadoException();
        }
    }
}
