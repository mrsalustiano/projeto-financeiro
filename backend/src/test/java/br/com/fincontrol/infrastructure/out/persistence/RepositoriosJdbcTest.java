package br.com.fincontrol.infrastructure.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.KeyHolder;

import br.com.fincontrol.domain.model.NovoUsuario;
import br.com.fincontrol.domain.model.Papel;
import br.com.fincontrol.domain.model.Usuario;
import br.com.fincontrol.domain.service.EmailEmUsoException;

@ExtendWith(MockitoExtension.class)
class RepositoriosJdbcTest {

    private static final Instant AGORA = Instant.parse("2026-11-22T03:00:00Z");

    @Mock private NamedParameterJdbcTemplate jdbc;

    private UsuarioJdbc usuarios;

    @BeforeEach
    void setUp() {
        usuarios = new UsuarioJdbc(jdbc);
    }

    @Test
    void buscaMapeiaLinhaEAusencia() throws Exception {
        ResultSet rs = linha();
        when(jdbc.query(contains("email"), anyMap(), any(RowMapper.class))).thenAnswer(inv -> {
            RowMapper<Usuario> mapper = inv.getArgument(2);
            return List.of(mapper.mapRow(rs, 0));
        });
        when(jdbc.query(contains("id ="), anyMap(), any(RowMapper.class))).thenReturn(List.of());

        Usuario usuario = usuarios.buscarPorEmail("voce@fincontrol.local").orElseThrow();

        assertThat(usuario.clienteId()).isEqualTo(42L);
        assertThat(usuario.papel()).isEqualTo(Papel.ADMIN);
        assertThat(usuario.bloqueadoAte()).isEqualTo(AGORA);
        assertThat(usuarios.buscarPorId(1L)).isEmpty();
    }

    @Test
    void masterSemClienteEContagem() throws Exception {
        ResultSet rs = linha();
        when(rs.getLong("cliente_id")).thenReturn(0L);
        when(rs.wasNull()).thenReturn(true);
        when(jdbc.query(anyString(), anyMap(), any(RowMapper.class))).thenAnswer(inv -> {
            RowMapper<Usuario> mapper = inv.getArgument(2);
            return List.of(mapper.mapRow(rs, 0));
        });
        when(jdbc.queryForObject(anyString(), anyMap(), org.mockito.ArgumentMatchers.eq(Integer.class)))
                .thenReturn(1, 0, null);

        assertThat(usuarios.buscarPorId(1L).orElseThrow().clienteId()).isNull();
        assertThat(usuarios.emailExiste("a@b.com")).isTrue();
        assertThat(usuarios.emailExiste("a@b.com")).isFalse();
        assertThat(usuarios.emailExiste("a@b.com")).isFalse();
    }

    @Test
    void insereETraduzDuplicidade() {
        when(jdbc.update(anyString(), any(SqlParameterSource.class), any(KeyHolder.class), any(String[].class)))
                .thenAnswer(inv -> {
                    KeyHolder chaves = inv.getArgument(2);
                    chaves.getKeyList().add(Map.of("id", 9));
                    return 1;
                });
        NovoUsuario novo = new NovoUsuario(42L, "Ana", "ana@fincontrol.local", "hash", Papel.ADMIN, true);

        assertThat(usuarios.inserir(novo)).isEqualTo(9L);

        when(jdbc.update(anyString(), any(SqlParameterSource.class), any(KeyHolder.class), any(String[].class)))
                .thenThrow(new DuplicateKeyException("dup"));
        assertThatThrownBy(() -> usuarios.inserir(novo)).isInstanceOf(EmailEmUsoException.class);
    }

    @Test
    void salvaFalhasEInsereCliente() throws Exception {
        Usuario usuario = new Usuario(7L, 42L, "Voce", "a@b.com", "hash", Papel.ADMIN, true, 2, AGORA);
        usuarios.salvarEstado(usuario);
        verify(jdbc).update(contains("falhas_login"), any(SqlParameterSource.class));

        ClienteJdbc clientes = new ClienteJdbc(jdbc);
        ResultSet cliente = mock(ResultSet.class);
        when(cliente.getLong("id")).thenReturn(5L);
        when(cliente.getString("nome")).thenReturn("Casa");
        when(cliente.getBoolean("ativo")).thenReturn(true);
        when(jdbc.query(contains("FROM cliente"), anyMap(), any(RowMapper.class))).thenAnswer(inv -> {
            RowMapper<?> mapper = inv.getArgument(2);
            return List.of(mapper.mapRow(cliente, 0));
        }).thenReturn(List.of());
        assertThat(clientes.buscarPorId(5L).orElseThrow().nome()).isEqualTo("Casa");
        assertThat(clientes.buscarPorId(1L)).isEmpty();
        when(jdbc.update(contains("INSERT INTO cliente"), any(SqlParameterSource.class), any(KeyHolder.class), any(String[].class)))
                .thenAnswer(inv -> {
                    KeyHolder chaves = inv.getArgument(2);
                    chaves.getKeyList().add(Map.of("id", 5));
                    return 1;
                });
        assertThat(clientes.inserir("Casa", true)).isEqualTo(5L);
    }

    @Test
    void idAusenteFalha() {
        when(jdbc.update(anyString(), any(SqlParameterSource.class), any(KeyHolder.class), any(String[].class)))
                .thenReturn(1);
        NovoUsuario novo = new NovoUsuario(null, "Master", "m@fincontrol.local", "hash", Papel.MASTER, true);

        assertThatThrownBy(() -> usuarios.inserir(novo)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new ClienteJdbc(jdbc).inserir("Casa", false)).isInstanceOf(IllegalStateException.class);
    }

    private static ResultSet linha() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("id")).thenReturn(7L);
        when(rs.getLong("cliente_id")).thenReturn(42L);
        when(rs.wasNull()).thenReturn(false);
        when(rs.getString("nome")).thenReturn("Voce");
        when(rs.getString("email")).thenReturn("voce@fincontrol.local");
        when(rs.getString("senha_hash")).thenReturn("hash");
        when(rs.getString("role")).thenReturn("ADMIN");
        when(rs.getBoolean("ativo")).thenReturn(true);
        when(rs.getInt("falhas_login")).thenReturn(1);
        when(rs.getTimestamp("bloqueado_ate")).thenReturn(Timestamp.from(AGORA));
        return rs;
    }
}
