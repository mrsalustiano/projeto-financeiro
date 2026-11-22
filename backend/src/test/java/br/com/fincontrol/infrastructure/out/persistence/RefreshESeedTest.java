package br.com.fincontrol.infrastructure.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.mock.env.MockEnvironment;

import br.com.fincontrol.application.PessoaSeed;
import br.com.fincontrol.application.SeedConfig;
import br.com.fincontrol.domain.model.Papel;
import br.com.fincontrol.domain.service.ClienteRepositorio;
import br.com.fincontrol.domain.service.SenhaPort;
import br.com.fincontrol.domain.service.UsuarioRepositorio;

@ExtendWith(MockitoExtension.class)
class RefreshESeedTest {

    private static final Instant AGORA = Instant.parse("2026-11-22T03:00:00Z");

    @Mock private NamedParameterJdbcTemplate jdbc;
    @Mock private ClienteRepositorio clientes;
    @Mock private UsuarioRepositorio usuarios;
    @Mock private SenhaPort senhas;

    @Test
    void refreshERevogacao() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("id")).thenReturn(3L);
        when(rs.getLong("usuario_id")).thenReturn(7L);
        when(rs.getTimestamp("expira_em")).thenReturn(Timestamp.from(AGORA));
        when(rs.getBoolean("revogado")).thenReturn(false);
        when(rs.getString("jti_access")).thenReturn("jti");
        when(rs.getTimestamp("access_expira_em")).thenReturn(null);
        when(jdbc.query(contains("refresh_token"), anyMap(), any(RowMapper.class))).thenAnswer(inv -> {
            RowMapper<?> mapper = inv.getArgument(2);
            return List.of(mapper.mapRow(rs, 0));
        });
        RefreshJdbc refresh = new RefreshJdbc(jdbc);

        assertThat(refresh.buscarPorHash("abc").orElseThrow().jtiAccess()).isEqualTo("jti");
        refresh.inserir(7L, "abc", AGORA, "jti", AGORA);
        refresh.revogar(3L);
        verify(jdbc).update(contains("INSERT INTO refresh_token"), any(SqlParameterSource.class));
        verify(jdbc).update(contains("revogado"), anyMap());

        RevogacaoJdbc revogacao = new RevogacaoJdbc(jdbc);
        when(jdbc.queryForObject(anyString(), anyMap(), org.mockito.ArgumentMatchers.eq(Integer.class)))
                .thenReturn(1, null);
        revogacao.revogar("jti", AGORA);
        assertThat(revogacao.revogado("jti")).isTrue();
        assertThat(revogacao.revogado("outro")).isFalse();
        verify(jdbc).update(contains("access_token_revogado"), any(SqlParameterSource.class));
    }

    @Test
    void seedPulaSemSenhaEQuemJaExiste() {
        SeedConfig vazio = new SeedConfig(" ", List.of());
        new SeedInicial(clientes, usuarios, senhas, vazio).run(null);
        verify(senhas, never()).hash(anyString());

        PessoaSeed master = new PessoaSeed("Master", " Master@Fincontrol.local ", "", Papel.MASTER, true, true);
        PessoaSeed casa = new PessoaSeed("Voce", "voce@fincontrol.local", "Casa", Papel.ADMIN, true, true);
        when(senhas.hash("segredo")).thenReturn("hash");
        when(usuarios.emailExiste("master@fincontrol.local")).thenReturn(true);
        when(usuarios.emailExiste("voce@fincontrol.local")).thenReturn(false);
        when(clientes.inserir("Casa", true)).thenReturn(42L);

        new SeedInicial(clientes, usuarios, senhas, new SeedConfig("segredo", List.of(master, casa))).run(null);

        verify(clientes, never()).inserir("", true);
        verify(usuarios).inserir(org.mockito.ArgumentMatchers.argThat(novo -> novo.clienteId() != null && novo.clienteId() == 42L));
    }

    @Test
    void configuracaoLeCincoPessoas() {
        MockEnvironment env = new MockEnvironment().withProperty("fincontrol.seed.senha", "segredo");
        prefixos().forEach(prefixo -> {
            env.setProperty(prefixo + ".nome", "Nome");
            env.setProperty(prefixo + ".email", prefixo + "@fincontrol.local");
        });

        SeedConfig config = new ConfiguracaoSeed().seedConfig(env);

        assertThat(config.senha()).isEqualTo("segredo");
        assertThat(config.pessoas()).hasSize(5);
        assertThat(config.pessoas().get(2).papel()).isEqualTo(Papel.MASTER);
        assertThat(config.pessoas().get(3).usuarioAtivo()).isFalse();
        assertThat(config.pessoas().get(4).clienteAtivo()).isFalse();
    }

    private static List<String> prefixos() {
        return List.of(
                "fincontrol.seed.usuario",
                "fincontrol.seed.mae",
                "fincontrol.seed.master",
                "fincontrol.seed.inativo",
                "fincontrol.seed.cliente-inativo");
    }
}
