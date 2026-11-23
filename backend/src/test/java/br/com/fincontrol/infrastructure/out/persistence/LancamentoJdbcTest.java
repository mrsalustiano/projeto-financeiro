package br.com.fincontrol.infrastructure.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Date;
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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.KeyHolder;

import br.com.fincontrol.domain.model.Dinheiro;
import br.com.fincontrol.domain.model.FechamentoMes;
import br.com.fincontrol.domain.model.FiltroLancamento;
import br.com.fincontrol.domain.model.Lancamento;
import br.com.fincontrol.domain.model.Natureza;
import br.com.fincontrol.domain.model.Parcelamento;
import br.com.fincontrol.domain.model.RegistroIdempotencia;
import br.com.fincontrol.domain.model.TipoDespesa;
import br.com.fincontrol.domain.service.IdempotenciaConflitoException;

@ExtendWith(MockitoExtension.class)
class LancamentoJdbcTest {

    private static final Instant AGORA = Instant.parse("2026-11-22T03:00:00Z");

    @Mock private NamedParameterJdbcTemplate jdbc;

    @Test
    void insereLancamentoEParcela() {
        when(jdbc.update(contains("INSERT INTO lancamento"), any(SqlParameterSource.class), any(KeyHolder.class), any(String[].class)))
                .thenAnswer(inv -> {
                    KeyHolder chaves = inv.getArgument(2);
                    chaves.getKeyList().add(Map.of("id", 11));
                    return 1;
                });
        when(jdbc.update(contains("INSERT INTO parcela"), any(SqlParameterSource.class), any(KeyHolder.class), any(String[].class)))
                .thenAnswer(inv -> {
                    KeyHolder chaves = inv.getArgument(2);
                    chaves.getKeyList().add(Map.of("id", 9));
                    return 1;
                });
        LancamentoJdbc repo = new LancamentoJdbc(jdbc);

        assertThat(repo.inserir(lancamento(null, null))).isEqualTo(11L);
        assertThat(repo.inserirParcela(42L, Parcelamento.planejar("g", new Dinheiro(new BigDecimal("10")), 2,
                LocalDate.of(2026, 11, 10)))).isEqualTo(9L);
    }

    @Test
    void insercaoSemChaveFalha() {
        when(jdbc.update(contains("INSERT INTO lancamento"), any(SqlParameterSource.class), any(KeyHolder.class), any(String[].class)))
                .thenReturn(1);
        when(jdbc.update(contains("INSERT INTO parcela"), any(SqlParameterSource.class), any(KeyHolder.class), any(String[].class)))
                .thenReturn(1);
        LancamentoJdbc repo = new LancamentoJdbc(jdbc);

        assertThatThrownBy(() -> repo.inserir(lancamento(null, null))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> repo.inserirParcela(42L, Parcelamento.planejar("g", new Dinheiro(new BigDecimal("10")), 1,
                LocalDate.of(2026, 11, 10)))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void mapeiaLancamentoComParcela() throws Exception {
        ResultSet rs = resultado(true);
        when(jdbc.query(contains("FROM lancamento"), anyMap(), any(RowMapper.class))).thenAnswer(inv -> {
            RowMapper<?> mapper = inv.getArgument(2);
            return List.of(mapper.mapRow(rs, 0));
        });
        when(jdbc.query(contains("FROM lancamento"), any(SqlParameterSource.class), any(RowMapper.class))).thenAnswer(inv -> {
            RowMapper<?> mapper = inv.getArgument(2);
            return List.of(mapper.mapRow(rs, 0));
        });
        LancamentoJdbc repo = new LancamentoJdbc(jdbc);

        Lancamento lancamento = repo.buscar(42L, 11L).orElseThrow();

        assertThat(lancamento.parcelaId()).isEqualTo(9L);
        assertThat(lancamento.grupoParcela()).isEqualTo("grupo");
        assertThat(lancamento.numeroParcela()).isEqualTo(1);
        assertThat(lancamento.totalParcelas()).isEqualTo(2);
        assertThat(lancamento.tipoDespesa()).isEqualTo(TipoDespesa.VARIAVEL);
        assertThat(lancamento.estornado()).isFalse();
        assertThat(repo.listar(42L, FiltroLancamento.vazio())).hasSize(1);
        assertThat(repo.listar(42L, new FiltroLancamento(1L, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30))))
                .hasSize(1);
        assertThat(repo.listarPorParcela(42L, 9L)).hasSize(1);
    }

    @Test
    void mapeiaLancamentoSemParcelaEEstornado() throws Exception {
        ResultSet rs = resultado(false);
        when(rs.wasNull()).thenReturn(true, true, true);
        when(rs.getTimestamp("estornado_em")).thenReturn(Timestamp.from(AGORA));
        when(rs.getString("estorno_motivo")).thenReturn("engano");
        when(rs.getString("estornado_por")).thenReturn("7");
        when(jdbc.query(contains("FROM lancamento"), anyMap(), any(RowMapper.class))).thenAnswer(inv -> {
            RowMapper<?> mapper = inv.getArgument(2);
            return List.of(mapper.mapRow(rs, 0));
        });
        LancamentoJdbc repo = new LancamentoJdbc(jdbc);

        Lancamento lancamento = repo.buscar(42L, 11L).orElseThrow();

        assertThat(lancamento.parcelaId()).isNull();
        assertThat(lancamento.numeroParcela()).isNull();
        assertThat(lancamento.totalParcelas()).isNull();
        assertThat(lancamento.estornado()).isTrue();
        assertThat(lancamento.estornadoEm()).isEqualTo(AGORA);
    }

    @Test
    void atualizaExcluiECalculaSaldo() throws Exception {
        when(jdbc.update(contains("UPDATE lancamento"), any(SqlParameterSource.class))).thenReturn(1);
        when(jdbc.update(contains("DELETE FROM lancamento"), anyMap())).thenReturn(1, 0);
        ResultSet rs = mock(ResultSet.class);
        when(rs.getBigDecimal("saldo_inicial")).thenReturn(new BigDecimal("100.00"));
        when(rs.getBigDecimal("creditos")).thenReturn(new BigDecimal("50.50"));
        when(rs.getBigDecimal("debitos")).thenReturn(new BigDecimal("30.25"));
        when(jdbc.query(contains("FROM conta"), anyMap(), any(RowMapper.class))).thenAnswer(inv -> {
            RowMapper<?> mapper = inv.getArgument(2);
            return List.of(mapper.mapRow(rs, 0));
        });
        LancamentoJdbc repo = new LancamentoJdbc(jdbc);

        repo.atualizar(lancamento(null, null));
        assertThat(repo.excluir(42L, 11L)).isTrue();
        assertThat(repo.excluir(42L, 11L)).isFalse();
        assertThat(repo.saldo(42L, 1L).orElseThrow().saldo()).isEqualByComparingTo("120.25");
    }

    @Test
    void fechamentoMapeiaInsereEExclui() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("id")).thenReturn(1L);
        when(rs.getLong("cliente_id")).thenReturn(42L);
        when(rs.getInt("ano")).thenReturn(2026);
        when(rs.getInt("mes")).thenReturn(11);
        when(rs.getTimestamp("fechado_em")).thenReturn(Timestamp.from(AGORA));
        when(rs.getString("fechado_por")).thenReturn("7");
        when(jdbc.query(contains("FROM fechamento_mes"), anyMap(), any(RowMapper.class))).thenAnswer(inv -> {
            RowMapper<?> mapper = inv.getArgument(2);
            return List.of(mapper.mapRow(rs, 0));
        });
        when(jdbc.update(contains("INSERT INTO fechamento_mes"), any(SqlParameterSource.class), any(KeyHolder.class), any(String[].class)))
                .thenAnswer(inv -> {
                    KeyHolder chaves = inv.getArgument(2);
                    chaves.getKeyList().add(Map.of("id", 5));
                    return 1;
                });
        when(jdbc.update(contains("DELETE FROM fechamento_mes"), anyMap())).thenReturn(1);
        FechamentoMesJdbc repo = new FechamentoMesJdbc(jdbc);

        assertThat(repo.buscar(42L, 2026, 11).orElseThrow().fechadoPor()).isEqualTo("7");
        assertThat(repo.listar(42L)).hasSize(1);
        assertThat(repo.inserir(new FechamentoMes(0L, 42L, 2026, 11, AGORA, "7"))).isEqualTo(5L);
        assertThat(repo.excluir(42L, 2026, 11)).isTrue();
    }

    @Test
    void fechamentoSemChaveFalha() {
        when(jdbc.update(contains("INSERT INTO fechamento_mes"), any(SqlParameterSource.class), any(KeyHolder.class), any(String[].class)))
                .thenReturn(1);
        FechamentoMesJdbc repo = new FechamentoMesJdbc(jdbc);

        assertThatThrownBy(() -> repo.inserir(new FechamentoMes(0L, 42L, 2026, 11, AGORA, "7")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void idempotenciaLeEGuarda() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("hash_corpo")).thenReturn("hash");
        when(rs.getLong("recurso_id")).thenReturn(11L);
        when(jdbc.query(contains("FROM idempotency_key"), anyMap(), any(RowMapper.class))).thenAnswer(inv -> {
            RowMapper<?> mapper = inv.getArgument(2);
            return List.of(mapper.mapRow(rs, 0));
        });
        when(jdbc.update(contains("INSERT INTO idempotency_key"), any(SqlParameterSource.class)))
                .thenReturn(1)
                .thenThrow(new DuplicateKeyException("dup"));
        IdempotenciaJdbc repo = new IdempotenciaJdbc(jdbc);

        RegistroIdempotencia registro = repo.buscar(42L, "k").orElseThrow();
        assertThat(registro.hashCorpo()).isEqualTo("hash");
        assertThat(registro.recursoId()).isEqualTo(11L);
        repo.guardar(42L, "k", "hash", 11L);
        assertThatThrownBy(() -> repo.guardar(42L, "k", "hash", 11L))
                .isInstanceOf(IdempotenciaConflitoException.class);
    }

    private static ResultSet resultado(boolean comParcela) throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("id")).thenReturn(11L);
        when(rs.getLong("cliente_id")).thenReturn(42L);
        when(rs.getLong("conta_id")).thenReturn(1L);
        when(rs.getLong("categoria_id")).thenReturn(2L);
        when(rs.getString("natureza")).thenReturn("DESPESA");
        when(rs.getString("tipo_despesa")).thenReturn("VARIAVEL");
        when(rs.getBigDecimal("valor")).thenReturn(new BigDecimal("100.00"));
        when(rs.getDate("data")).thenReturn(Date.valueOf(LocalDate.of(2026, 11, 10)));
        when(rs.getString("descricao")).thenReturn("Compra");
        when(rs.getLong("parcela_id")).thenReturn(comParcela ? 9L : 0L);
        when(rs.getString("grupo_parcela")).thenReturn(comParcela ? "grupo" : null);
        when(rs.getInt("numero_parcela")).thenReturn(comParcela ? 1 : 0);
        when(rs.getInt("total_parcelas")).thenReturn(comParcela ? 2 : 0);
        when(rs.getTimestamp("estornado_em")).thenReturn(null);
        when(rs.getString("estorno_motivo")).thenReturn(null);
        when(rs.getString("estornado_por")).thenReturn(null);
        return rs;
    }

    private static Lancamento lancamento(Long parcelaId, String grupo) {
        return new Lancamento(11L, 42L, 1L, 2L, Natureza.DESPESA, TipoDespesa.VARIAVEL,
                new Dinheiro(new BigDecimal("100.00")), LocalDate.of(2026, 11, 10), "Compra",
                parcelaId, grupo, parcelaId == null ? null : 1, parcelaId == null ? null : 2, null, null, null);
    }
}
