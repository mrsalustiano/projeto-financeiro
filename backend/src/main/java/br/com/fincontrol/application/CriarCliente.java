package br.com.fincontrol.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fincontrol.domain.model.Emails;
import br.com.fincontrol.domain.model.AcaoAuditoria;
import br.com.fincontrol.domain.model.NovoUsuario;
import br.com.fincontrol.domain.model.Papel;
import br.com.fincontrol.domain.service.ClienteRepositorio;
import br.com.fincontrol.domain.service.EmailEmUsoException;
import br.com.fincontrol.domain.service.SenhaPort;
import br.com.fincontrol.domain.service.UsuarioRepositorio;

@Service
public class CriarCliente {

    private final ClienteRepositorio clientes;
    private final UsuarioRepositorio usuarios;
    private final SenhaPort senhas;
    private final GarantirCategoriasPadrao categorias;

    public CriarCliente(
            ClienteRepositorio clientes,
            UsuarioRepositorio usuarios,
            SenhaPort senhas,
            GarantirCategoriasPadrao categorias) {
        this.clientes = clientes;
        this.usuarios = usuarios;
        this.senhas = senhas;
        this.categorias = categorias;
    }

    @Transactional
    @Auditavel(acao = AcaoAuditoria.CREATE, recurso = "cliente")
    public ClienteCriado executar(String nomeCliente, String nomeAdministrador, String email, String senha) {
        String normalizado = Emails.normalizar(email);
        if (usuarios.emailExiste(normalizado)) {
            throw new EmailEmUsoException();
        }
        long clienteId = clientes.inserir(nomeCliente.trim(), true);
        long administradorId = usuarios.inserir(new NovoUsuario(
                clienteId,
                nomeAdministrador.trim(),
                normalizado,
                senhas.hash(senha),
                Papel.ADMIN,
                true));
        categorias.executar(clienteId);
        return new ClienteCriado(clienteId, administradorId);
    }
}
