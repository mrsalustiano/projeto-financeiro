package br.com.fincontrol.infrastructure.out.persistence;

import java.time.LocalDate;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import br.com.fincontrol.application.ContaSeed;
import br.com.fincontrol.application.PessoaSeed;
import br.com.fincontrol.application.SeedConfig;
import br.com.fincontrol.domain.model.Papel;
import br.com.fincontrol.domain.model.Saldo;
import br.com.fincontrol.domain.model.TipoConta;

@Configuration
public class ConfiguracaoSeed {

    @Bean
    SeedConfig seedConfig(Environment env) {
        LocalDate inicio = LocalDate.of(2026, 1, 1);
        return new SeedConfig(env.getProperty("fincontrol.seed.senha", ""), List.of(
                pessoa(env, "fincontrol.seed.usuario", "Casa", Papel.ADMIN, true, true, null, List.of(
                        new ContaSeed("Nubank", "Nubank", TipoConta.CORRENTE, Saldo.zero(), inicio),
                        new ContaSeed("Flash", "Flash", TipoConta.BENEFICIO, Saldo.zero(), inicio))),
                leitor(env),
                pessoa(env, "fincontrol.seed.mae", "Familia", Papel.ADMIN, true, true, null, List.of(
                        new ContaSeed("Banco do Brasil", "Banco do Brasil", TipoConta.CORRENTE, Saldo.zero(), inicio))),
                pessoa(env, "fincontrol.seed.master", "", Papel.MASTER, true, true, null, List.of()),
                pessoa(env, "fincontrol.seed.inativo", "Pessoa inativa", Papel.LEITOR, false, true, null, List.of()),
                pessoa(env, "fincontrol.seed.cliente-inativo", "Cliente inativo", Papel.ADMIN, true, false, null, List.of())));
    }

    private static PessoaSeed leitor(Environment env) {
        return new PessoaSeed(
                env.getProperty("fincontrol.seed.leitor.nome"),
                env.getProperty("fincontrol.seed.leitor.email"),
                "",
                Papel.LEITOR,
                true,
                true,
                env.getProperty("fincontrol.seed.usuario.email"),
                List.of());
    }

    private static PessoaSeed pessoa(
            Environment env,
            String prefixo,
            String cliente,
            Papel papel,
            boolean usuarioAtivo,
            boolean clienteAtivo,
            String emailDoCliente,
            List<ContaSeed> contas) {
        return new PessoaSeed(
                env.getProperty(prefixo + ".nome"),
                env.getProperty(prefixo + ".email"),
                cliente,
                papel,
                usuarioAtivo,
                clienteAtivo,
                emailDoCliente,
                contas);
    }
}
