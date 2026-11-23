package br.com.fincontrol.infrastructure.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.KeyHolder;

import br.com.fincontrol.domain.model.AcaoAuditoria;
import br.com.fincontrol.domain.model.FiltroAuditoria;
import br.com.fincontrol.domain.model.RegistroAuditoria;

@ExtendWith(MockitoExtension.class)
class AuditoriaJdbcTest {

    private static final Instant AGORA = Instant.parse("2026-11-23T12:00:00Z");

    @Mock private NamedParameterJdbcTemplate jdbc;

    @Test
    void insereEDevolveId() {
        when(jdbc.update(contains("INSERT INTO auditoria"), any(SqlParameterSource.class), any(KeyHolder.class),
                any(String[].class))).thenAnswer(inv -> {
                    KeyHolder chaves = inv.getArgument(2);
                    chaves.getKeyList().add(Map.of("id", 11));
                    return 1;
                });
        AuditoriaJdbc repo = new AuditoriaJdbc(jdbc);

        assertThat(repo.inserir(registro())).isEqualTo(11L);
    }

    @Test
    void insercaoSemChaveFalha() {
        when(jdbc.update(contains("INSERT INTO auditoria"), any(SqlParameterSource.class), any(KeyHolder.class),
                any(String[].class))).thenReturn(1);

        assertThatThrownBy(() -> new AuditoriaJdbc(jdbc).inserir(registro()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void listaComTodosOsFiltros() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("id")).thenReturn(9L);
        when(rs.getLong("cliente_id")).thenReturn(42L);
        when(rs.getLong("usuario_id")).thenReturn(0L);
        when(rs.getLong("recurso_id")).thenReturn(0L);
        when(rs.wasNull()).thenReturn(false, true, true);
        when(rs.getString("usuario_email")).thenReturn("voce@fincontrol.local");
        when(rs.getString("usuario_perfil")).thenReturn("ADMIN");
        when(rs.getString("acao")).thenReturn("CREATE");
        when(rs.getString("recurso")).thenReturn("lancamento");
        when(rs.getString("dados_antes")).thenReturn(null);
        when(rs.getString("dados_depois")).thenReturn("{}");
        when(rs.getTimestamp("criado_em")).thenReturn(Timestamp.from(AGORA));
        when(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class))).thenAnswer(inv -> {
            RowMapper<?> mapper = inv.getArgument(2);
            return List.of(mapper.mapRow(rs, 0));
        });
        AuditoriaJdbc repo = new AuditoriaJdbc(jdbc);

        List<RegistroAuditoria> itens = repo.listar(
                new FiltroAuditoria(42L, "lancamento", AcaoAuditoria.CREATE, LocalDate.of(2026, 11, 1),
                        LocalDate.of(2026, 11, 30)),
                100L,
                20);

        assertThat(itens).hasSize(1);
        assertThat(itens.getFirst().clienteId()).isEqualTo(42L);
        assertThat(itens.getFirst().usuarioId()).isNull();
        assertThat(itens.getFirst().recursoId()).isNull();
        assertThat(itens.getFirst().criadoEm()).isEqualTo(AGORA);
    }

    @Test
    void listaSemFiltros() {
        when(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of());
        AuditoriaJdbc repo = new AuditoriaJdbc(jdbc);

        assertThat(repo.listar(FiltroAuditoria.vazio(), null, 20)).isEmpty();
        assertThat(repo.listar(new FiltroAuditoria(null, null, null, null, LocalDate.of(2026, 11, 30)), null, 20))
                .isEmpty();
        assertThat(repo.listar(new FiltroAuditoria(null, null, null, LocalDate.of(2026, 11, 1), null), null, 20))
                .isEmpty();
    }

    private static RegistroAuditoria registro() {
        return new RegistroAuditoria(0L, 42L, 7L, "voce@fincontrol.local", "ADMIN", AcaoAuditoria.CREATE,
                "lancamento", 44L, null, "{}", AGORA);
    }
}
