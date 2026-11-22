package br.com.fincontrol.infrastructure.out.persistence;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import br.com.fincontrol.application.PessoaSeed;
import br.com.fincontrol.application.SeedConfig;
import br.com.fincontrol.domain.model.Emails;
import br.com.fincontrol.domain.model.NovoUsuario;
import br.com.fincontrol.domain.model.Papel;
import br.com.fincontrol.domain.service.ClienteRepositorio;
import br.com.fincontrol.domain.service.SenhaPort;
import br.com.fincontrol.domain.service.UsuarioRepositorio;

@Component
public class SeedInicial implements ApplicationRunner {

    private final ClienteRepositorio clientes;
    private final UsuarioRepositorio usuarios;
    private final SenhaPort senhas;
    private final SeedConfig config;

    public SeedInicial(
            ClienteRepositorio clientes,
            UsuarioRepositorio usuarios,
            SenhaPort senhas,
            SeedConfig config) {
        this.clientes = clientes;
        this.usuarios = usuarios;
        this.senhas = senhas;
        this.config = config;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (config.senha() == null || config.senha().isBlank()) {
            return;
        }
        config.pessoas().forEach(pessoa -> semear(pessoa, senhas.hash(config.senha())));
    }

    private void semear(PessoaSeed pessoa, String hash) {
        String email = Emails.normalizar(pessoa.email());
        if (usuarios.emailExiste(email)) {
            return;
        }
        Long clienteId = cliente(pessoa);
        usuarios.inserir(new NovoUsuario(clienteId, pessoa.nome(), email, hash, pessoa.papel(), pessoa.usuarioAtivo()));
    }

    private Long cliente(PessoaSeed pessoa) {
        if (pessoa.papel() == Papel.MASTER) {
            return null;
        }
        return clientes.inserir(pessoa.nomeCliente(), pessoa.clienteAtivo());
    }
}
