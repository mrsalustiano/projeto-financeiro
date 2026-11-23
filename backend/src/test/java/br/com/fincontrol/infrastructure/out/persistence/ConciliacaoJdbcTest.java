package br.com.fincontrol.infrastructure.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
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

import br.com.fincontrol.domain.model.Comprovante;
import br.com.fincontrol.domain.model.Conciliacao;
import br.com.fincontrol.domain.model.TipoComprovante;

@ExtendWith(MockitoExtension.class)
class ConciliacaoJdbcTest {

    private static final Instant AGORA = Instant.parse("2026-11-23T12:00:00Z");

    @Mock private NamedParameterJdbcTemplate jdbc;

    @Test
    void conciliacaoListaBuscaInsereEExclui() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("id")).thenReturn(5L);
        when(rs.getLong("cliente_id")).thenReturn(42L);
        when(rs.getLong("conta_id")).thenReturn(10L);
        when(rs.getLong("lancamento_id")).thenReturn(1L);
        when(rs.getLong("extrato_id")).thenReturn(2L);
        when(rs.getTimestamp("conciliado_em")).thenReturn(Timestamp.from(AGORA));
        when(rs.getString("conciliado_por")).thenReturn("7");
        when(jdbc.query(contains("FROM conciliacao"), anyMap(), any(RowMapper.class))).thenAnswer(inv -> {
            RowMapper<?> mapper = inv.getArgument(2);
            return List.of(mapper.mapRow(rs, 0));
        });
        when(jdbc.update(contains("INSERT INTO conciliacao"), any(SqlParameterSource.class), any(KeyHolder.class),
                any(String[].class))).thenAnswer(inv -> {
                    KeyHolder chaves = inv.getArgument(2);
                    chaves.getKeyList().add(Map.of("id", 5));
                    return 1;
                });
        when(jdbc.update(contains("DELETE FROM conciliacao"), anyMap())).thenReturn(1, 0);
        ConciliacaoJdbc repo = new ConciliacaoJdbc(jdbc);

        Conciliacao conciliacao = repo.listar(42L).getFirst();
        assertThat(conciliacao.lancamentoId()).isEqualTo(1L);
        assertThat(conciliacao.conciliadoEm()).isEqualTo(AGORA);
        assertThat(repo.buscar(42L, 5L)).isPresent();
        assertThat(repo.buscarPorLancamento(42L, 2L)).isPresent();
        assertThat(repo.inserir(conciliacao)).isEqualTo(5L);
        assertThat(repo.excluir(42L, 5L)).isTrue();
        assertThat(repo.excluir(42L, 5L)).isFalse();
    }

    @Test
    void conciliacaoSemChaveFalha() {
        when(jdbc.update(contains("INSERT INTO conciliacao"), any(SqlParameterSource.class), any(KeyHolder.class),
                any(String[].class))).thenReturn(1);
        ConciliacaoJdbc repo = new ConciliacaoJdbc(jdbc);

        assertThatThrownBy(() -> repo.inserir(new Conciliacao(0L, 42L, 10L, 1L, 2L, AGORA, "7")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void comprovanteListaBuscaInsereEExclui() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("id")).thenReturn(3L);
        when(rs.getLong("cliente_id")).thenReturn(42L);
        when(rs.getLong("lancamento_id")).thenReturn(1L);
        when(rs.getString("nome_arquivo")).thenReturn("recibo.png");
        when(rs.getString("tipo")).thenReturn("PNG");
        when(rs.getLong("tamanho")).thenReturn(4L);
        when(rs.getString("storage_key")).thenReturn("chave");
        when(rs.getTimestamp("criado_em")).thenReturn(Timestamp.from(AGORA));
        when(rs.getString("criado_por")).thenReturn("7");
        when(jdbc.query(contains("FROM comprovante"), anyMap(), any(RowMapper.class))).thenAnswer(inv -> {
            RowMapper<?> mapper = inv.getArgument(2);
            return List.of(mapper.mapRow(rs, 0));
        });
        when(jdbc.update(contains("INSERT INTO comprovante"), any(SqlParameterSource.class), any(KeyHolder.class),
                any(String[].class))).thenAnswer(inv -> {
                    KeyHolder chaves = inv.getArgument(2);
                    chaves.getKeyList().add(Map.of("id", 3));
                    return 1;
                });
        when(jdbc.update(contains("DELETE FROM comprovante"), anyMap())).thenReturn(1, 0);
        ComprovanteJdbc repo = new ComprovanteJdbc(jdbc);

        Comprovante comprovante = repo.listar(42L, 1L).getFirst();
        assertThat(comprovante.tipo()).isEqualTo(TipoComprovante.PNG);
        assertThat(comprovante.storageKey()).isEqualTo("chave");
        assertThat(repo.buscar(42L, 3L)).isPresent();
        assertThat(repo.inserir(comprovante)).isEqualTo(3L);
        assertThat(repo.excluir(42L, 3L)).isTrue();
        assertThat(repo.excluir(42L, 3L)).isFalse();
    }

    @Test
    void comprovanteSemChaveFalha() {
        when(jdbc.update(contains("INSERT INTO comprovante"), any(SqlParameterSource.class), any(KeyHolder.class),
                any(String[].class))).thenReturn(1);
        ComprovanteJdbc repo = new ComprovanteJdbc(jdbc);

        assertThatThrownBy(() -> repo.inserir(new Comprovante(0L, 42L, 1L, "recibo.png", TipoComprovante.PNG, 4L,
                "chave", AGORA, "7")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void buscaVaziaDevolveOpcionalVazio() {
        when(jdbc.query(contains("FROM conciliacao"), anyMap(), any(RowMapper.class))).thenReturn(List.of());
        when(jdbc.query(contains("FROM comprovante"), anyMap(), any(RowMapper.class))).thenReturn(List.of());

        assertThat(new ConciliacaoJdbc(jdbc).buscar(42L, 5L)).isEmpty();
        assertThat(new ComprovanteJdbc(jdbc).buscar(42L, 3L)).isEmpty();
    }
}
