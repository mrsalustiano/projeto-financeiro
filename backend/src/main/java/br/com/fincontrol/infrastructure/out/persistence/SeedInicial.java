package br.com.fincontrol.infrastructure.out.persistence;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import br.com.fincontrol.application.GarantirCategoriasPadrao;
import br.com.fincontrol.application.GarantirContas;
import br.com.fincontrol.application.GarantirLancamentos;
import br.com.fincontrol.application.GarantirRecorrencias;
import br.com.fincontrol.application.PessoaSeed;
import br.com.fincontrol.application.SeedConfig;
import br.com.fincontrol.domain.model.Emails;
import br.com.fincontrol.domain.model.NovoUsuario;
import br.com.fincontrol.domain.model.Papel;
import br.com.fincontrol.domain.model.Usuario;
import br.com.fincontrol.domain.service.ClienteRepositorio;
import br.com.fincontrol.domain.service.SenhaPort;
import br.com.fincontrol.domain.service.UsuarioRepositorio;

@Component
public class SeedInicial implements ApplicationRunner {

    private final ClienteRepositorio clientes;
    private final UsuarioRepositorio usuarios;
    private final SenhaPort senhas;
    private final SeedConfig config;
    private final GarantirCategoriasPadrao categorias;
    private final GarantirContas contas;
    private final GarantirLancamentos lancamentos;
    private final GarantirRecorrencias recorrencias;

    public SeedInicial(
            ClienteRepositorio clientes,
            UsuarioRepositorio usuarios,
            SenhaPort senhas,
            SeedConfig config,
            GarantirCategoriasPadrao categorias,
            GarantirContas contas,
            GarantirLancamentos lancamentos,
            GarantirRecorrencias recorrencias) {
        this.clientes = clientes;
        this.usuarios = usuarios;
        this.senhas = senhas;
        this.config = config;
        this.categorias = categorias;
        this.contas = contas;
        this.lancamentos = lancamentos;
        this.recorrencias = recorrencias;
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
            prepararExistente(pessoa, email);
            return;
        }
        Long clienteId = cliente(pessoa);
        usuarios.inserir(new NovoUsuario(clienteId, pessoa.nome(), email, hash, pessoa.papel(), pessoa.usuarioAtivo()));
        preparar(clienteId, pessoa);
    }

    private void prepararExistente(PessoaSeed pessoa, String email) {
        if (pessoa.papel() == Papel.MASTER) {
            return;
        }
        Usuario usuario = usuarios.buscarPorEmail(email).orElse(null);
        if (usuario == null) {
            return;
        }
        preparar(usuario.clienteId(), pessoa);
    }

    private void preparar(Long clienteId, PessoaSeed pessoa) {
        if (clienteId == null) {
            return;
        }
        categorias.executar(clienteId);
        contas.executar(clienteId, pessoa.contas());
        lancamentos.executar(clienteId, pessoa.lancamentos());
        recorrencias.executar(clienteId, pessoa.recorrencias());
    }

    private Long cliente(PessoaSeed pessoa) {
        if (pessoa.papel() == Papel.MASTER) {
            return null;
        }
        if (pessoa.emailDoCliente() != null) {
            return usuarios.buscarPorEmail(Emails.normalizar(pessoa.emailDoCliente()))
                    .map(Usuario::clienteId)
                    .orElseThrow();
        }
        return clientes.inserir(pessoa.nomeCliente(), pessoa.clienteAtivo());
    }
}
