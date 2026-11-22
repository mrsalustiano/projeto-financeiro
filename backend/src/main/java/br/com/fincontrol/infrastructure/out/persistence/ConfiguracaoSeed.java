package br.com.fincontrol.infrastructure.out.persistence;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import br.com.fincontrol.application.PessoaSeed;
import br.com.fincontrol.application.SeedConfig;
import br.com.fincontrol.domain.model.Papel;

@Configuration
public class ConfiguracaoSeed {

    @Bean
    SeedConfig seedConfig(Environment env) {
        return new SeedConfig(env.getProperty("fincontrol.seed.senha", ""), List.of(
                pessoa(env, "fincontrol.seed.usuario", "Casa", Papel.ADMIN, true, true),
                pessoa(env, "fincontrol.seed.mae", "Familia", Papel.ADMIN, true, true),
                pessoa(env, "fincontrol.seed.master", "", Papel.MASTER, true, true),
                pessoa(env, "fincontrol.seed.inativo", "Pessoa inativa", Papel.LEITOR, false, true),
                pessoa(env, "fincontrol.seed.cliente-inativo", "Cliente inativo", Papel.ADMIN, true, false)));
    }

    private static PessoaSeed pessoa(
            Environment env, String prefixo, String cliente, Papel papel, boolean usuarioAtivo, boolean clienteAtivo) {
        return new PessoaSeed(
                env.getProperty(prefixo + ".nome"),
                env.getProperty(prefixo + ".email"),
                cliente,
                papel,
                usuarioAtivo,
                clienteAtivo);
    }
}
