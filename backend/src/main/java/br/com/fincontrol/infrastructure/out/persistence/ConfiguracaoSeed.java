package br.com.fincontrol.infrastructure.out.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import br.com.fincontrol.application.ContaSeed;
import br.com.fincontrol.application.LancamentoSeed;
import br.com.fincontrol.application.PessoaSeed;
import br.com.fincontrol.application.RecorrenciaSeed;
import br.com.fincontrol.application.SeedConfig;
import br.com.fincontrol.domain.model.Dinheiro;
import br.com.fincontrol.domain.model.Natureza;
import br.com.fincontrol.domain.model.Papel;
import br.com.fincontrol.domain.model.Saldo;
import br.com.fincontrol.domain.model.TipoConta;
import br.com.fincontrol.domain.model.TipoDespesa;

@Configuration
public class ConfiguracaoSeed {

    private static final LocalDate INICIO = LocalDate.of(2026, 1, 1);

    @Bean
    SeedConfig seedConfig(Environment env) {
        return new SeedConfig(env.getProperty("fincontrol.seed.senha", ""), List.of(
                pessoa(env, "fincontrol.seed.usuario", "Casa", Papel.ADMIN, true, true, null,
                        List.of(
                                new ContaSeed("Nubank", "Nubank", TipoConta.CORRENTE, Saldo.zero(), INICIO),
                                new ContaSeed("Flash", "Flash", TipoConta.BENEFICIO, Saldo.zero(), INICIO)),
                        List.of(
                                new LancamentoSeed("Flash", "Alimentação", Natureza.DESPESA, TipoDespesa.VARIAVEL,
                                        dinheiro("320.00"), LocalDate.of(2026, 11, 5), "Mercado do mês"),
                                new LancamentoSeed("Flash", "Transporte", Natureza.DESPESA, TipoDespesa.VARIAVEL,
                                        dinheiro("180.00"), LocalDate.of(2026, 11, 8), "Combustível")),
                        List.of(
                                new RecorrenciaSeed("Flash", "Benefícios", Natureza.RECEITA, null,
                                        dinheiro("1570.00"), "Benefício Flash", 0))),
                leitor(env),
                pessoa(env, "fincontrol.seed.mae", "Familia", Papel.ADMIN, true, true, null,
                        List.of(new ContaSeed("Banco do Brasil", "Banco do Brasil", TipoConta.CORRENTE, Saldo.zero(), INICIO)),
                        List.of(),
                        List.of()),
                pessoa(env, "fincontrol.seed.master", "", Papel.MASTER, true, true, null, List.of(), List.of(), List.of()),
                pessoa(env, "fincontrol.seed.inativo", "Pessoa inativa", Papel.LEITOR, false, true, null,
                        List.of(), List.of(), List.of()),
                pessoa(env, "fincontrol.seed.cliente-inativo", "Cliente inativo", Papel.ADMIN, true, false, null,
                        List.of(), List.of(), List.of())));
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
            List<ContaSeed> contas,
            List<LancamentoSeed> lancamentos,
            List<RecorrenciaSeed> recorrencias) {
        return new PessoaSeed(
                env.getProperty(prefixo + ".nome"),
                env.getProperty(prefixo + ".email"),
                cliente,
                papel,
                usuarioAtivo,
                clienteAtivo,
                emailDoCliente,
                contas,
                lancamentos,
                recorrencias);
    }

    private static Dinheiro dinheiro(String valor) {
        return new Dinheiro(new BigDecimal(valor));
    }
}
