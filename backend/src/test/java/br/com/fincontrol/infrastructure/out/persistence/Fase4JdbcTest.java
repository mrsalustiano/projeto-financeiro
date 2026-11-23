package br.com.fincontrol.infrastructure.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
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

import br.com.fincontrol.domain.model.Aporte;
import br.com.fincontrol.domain.model.DiaDoMes;
import br.com.fincontrol.domain.model.Dinheiro;
import br.com.fincontrol.domain.model.Meta;
import br.com.fincontrol.domain.model.Natureza;
import br.com.fincontrol.domain.model.Orcamento;
import br.com.fincontrol.domain.model.Recorrencia;
import br.com.fincontrol.domain.model.Transferencia;
import br.com.fincontrol.domain.service.NomeEmUsoException;

@ExtendWith(MockitoExtension.class)
class Fase4JdbcTest {

    private static final Instant AGORA = Instant.parse("2026-11-22T03:00:00Z");

    @Mock private NamedParameterJdbcTemplate jdbc;

    @Test
    void transferenciaInsereBuscaELista() throws Exception {
        when(jdbc.update(contains("INSERT INTO transferencia"), any(SqlParameterSource.class), any(KeyHolder.class),
                any(String[].class))).thenAnswer(inv -> {
                    KeyHolder chaves = inv.getArgument(2);
                    chaves.getKeyList().add(Map.of("id", 9));
                    return 1;
                });
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("id")).thenReturn(9L);
        when(rs.getLong("cliente_id")).thenReturn(42L);
        when(rs.getLong("conta_origem_id")).thenReturn(1L);
        when(rs.getLong("conta_destino_id")).thenReturn(2L);
        when(rs.getBigDecimal("valor")).thenReturn(new BigDecimal("100.00"));
        when(rs.getDate("data")).thenReturn(Date.valueOf(LocalDate.of(2026, 11, 10)));
        when(rs.getString("descricao")).thenReturn("Reserva");
        when(jdbc.query(contains("FROM transferencia"), anyMap(), any(RowMapper.class))).thenAnswer(inv -> {
            RowMapper<?> mapper = inv.getArgument(2);
            return List.of(mapper.mapRow(rs, 0));
        });
        TransferenciaJdbc repo = new TransferenciaJdbc(jdbc);

        assertThat(repo.inserir(transferencia())).isEqualTo(9L);
        assertThat(repo.buscar(42L, 9L).orElseThrow().valor().valor()).isEqualByComparingTo("100.00");
        assertThat(repo.listar(42L)).hasSize(1);
    }

    @Test
    void transferenciaSemChaveFalha() {
        when(jdbc.update(contains("INSERT INTO transferencia"), any(SqlParameterSource.class), any(KeyHolder.class),
                any(String[].class))).thenReturn(1);
        TransferenciaJdbc repo = new TransferenciaJdbc(jdbc);

        assertThatThrownBy(() -> repo.inserir(transferencia())).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void orcamentoSalvaBuscaListaEExclui() throws Exception {
        when(jdbc.update(contains("INSERT INTO orcamento"), any(SqlParameterSource.class))).thenReturn(1);
        when(jdbc.update(contains("DELETE FROM orcamento"), anyMap())).thenReturn(1, 0);
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("id")).thenReturn(5L);
        when(rs.getLong("cliente_id")).thenReturn(42L);
        when(rs.getLong("categoria_id")).thenReturn(3L);
        when(rs.getInt("ano")).thenReturn(2026);
        when(rs.getInt("mes")).thenReturn(11);
        when(rs.getBigDecimal("limite")).thenReturn(new BigDecimal("1000.00"));
        when(jdbc.query(contains("FROM orcamento"), anyMap(), any(RowMapper.class))).thenAnswer(inv -> {
            RowMapper<?> mapper = inv.getArgument(2);
            return List.of(mapper.mapRow(rs, 0));
        });
        OrcamentoJdbc repo = new OrcamentoJdbc(jdbc);

        assertThat(repo.salvar(orcamento())).isEqualTo(5L);
        assertThat(repo.buscar(42L, 3L, 2026, 11).orElseThrow().limite().valor()).isEqualByComparingTo("1000.00");
        assertThat(repo.listar(42L, 2026, 11)).hasSize(1);
        assertThat(repo.excluir(42L, 3L, 2026, 11)).isTrue();
        assertThat(repo.excluir(42L, 3L, 2026, 11)).isFalse();
    }

    @Test
    void metaInsereBuscaListaEAporta() throws Exception {
        when(jdbc.update(contains("INSERT INTO meta"), any(SqlParameterSource.class), any(KeyHolder.class),
                any(String[].class))).thenAnswer(inv -> {
                    KeyHolder chaves = inv.getArgument(2);
                    chaves.getKeyList().add(Map.of("id", 7));
                    return 1;
                });
        when(jdbc.update(contains("INSERT INTO aporte"), any(SqlParameterSource.class), any(KeyHolder.class),
                any(String[].class))).thenAnswer(inv -> {
                    KeyHolder chaves = inv.getArgument(2);
                    chaves.getKeyList().add(Map.of("id", 11));
                    return 1;
                });
        ResultSet meta = mock(ResultSet.class);
        when(meta.getLong("id")).thenReturn(7L);
        when(meta.getLong("cliente_id")).thenReturn(42L);
        when(meta.getString("nome")).thenReturn("Viagem");
        when(meta.getBigDecimal("valor_alvo")).thenReturn(new BigDecimal("1000.00"));
        when(meta.getDate("data_alvo")).thenReturn(null);
        when(meta.getTimestamp("criada_em")).thenReturn(Timestamp.from(AGORA));
        ResultSet aporte = mock(ResultSet.class);
        when(aporte.getLong("id")).thenReturn(11L);
        when(aporte.getLong("cliente_id")).thenReturn(42L);
        when(aporte.getLong("meta_id")).thenReturn(7L);
        when(aporte.getBigDecimal("valor")).thenReturn(new BigDecimal("250.00"));
        when(aporte.getDate("data")).thenReturn(Date.valueOf(LocalDate.of(2026, 11, 10)));
        when(jdbc.query(contains("FROM meta"), anyMap(), any(RowMapper.class))).thenAnswer(inv -> {
            RowMapper<?> mapper = inv.getArgument(2);
            return List.of(mapper.mapRow(meta, 0));
        });
        when(jdbc.query(contains("FROM aporte"), anyMap(), any(RowMapper.class))).thenAnswer(inv -> {
            RowMapper<?> mapper = inv.getArgument(2);
            return List.of(mapper.mapRow(aporte, 0));
        });
        when(jdbc.queryForObject(contains("COUNT(*) FROM meta"), anyMap(), eq(Integer.class))).thenReturn(1, 0);
        when(jdbc.queryForObject(contains("SUM(valor)"), anyMap(), eq(BigDecimal.class)))
                .thenReturn(new BigDecimal("250.00"), (BigDecimal) null);
        MetaJdbc repo = new MetaJdbc(jdbc);

        assertThat(repo.inserir(meta())).isEqualTo(7L);
        assertThat(repo.buscar(42L, 7L).orElseThrow().nome()).isEqualTo("Viagem");
        assertThat(repo.listar(42L)).hasSize(1);
        assertThat(repo.existeNome(42L, "Viagem")).isTrue();
        assertThat(repo.existeNome(42L, "Outra")).isFalse();
        assertThat(repo.inserirAporte(aporte())).isEqualTo(11L);
        assertThat(repo.listarAportes(42L, 7L)).hasSize(1);
        assertThat(repo.totalAportado(42L, 7L)).isEqualByComparingTo("250.00");
        assertThat(repo.totalAportado(42L, 7L)).isEqualByComparingTo("0");
    }

    @Test
    void metaDuplicadaENomeEmUso() {
        when(jdbc.update(contains("INSERT INTO meta"), any(SqlParameterSource.class), any(KeyHolder.class),
                any(String[].class))).thenThrow(new DuplicateKeyException("dup")).thenReturn(1);
        MetaJdbc repo = new MetaJdbc(jdbc);

        assertThatThrownBy(() -> repo.inserir(meta())).isInstanceOf(NomeEmUsoException.class);
        assertThatThrownBy(() -> repo.inserir(meta())).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void recorrenciaInsereBuscaListaEAtualiza() throws Exception {
        when(jdbc.update(contains("INSERT INTO recorrencia"), any(SqlParameterSource.class), any(KeyHolder.class),
                any(String[].class))).thenAnswer(inv -> {
                    KeyHolder chaves = inv.getArgument(2);
                    chaves.getKeyList().add(Map.of("id", 4));
                    return 1;
                });
        when(jdbc.update(contains("UPDATE recorrencia"), any(SqlParameterSource.class))).thenReturn(1);
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("id")).thenReturn(4L);
        when(rs.getLong("cliente_id")).thenReturn(42L);
        when(rs.getLong("conta_id")).thenReturn(2L);
        when(rs.getLong("categoria_id")).thenReturn(3L);
        when(rs.getString("natureza")).thenReturn("RECEITA");
        when(rs.getString("tipo_despesa")).thenReturn(null);
        when(rs.getBigDecimal("valor")).thenReturn(new BigDecimal("1570.00"));
        when(rs.getString("descricao")).thenReturn("Benefício Flash");
        when(rs.getInt("dia_do_mes")).thenReturn(0);
        when(rs.getBoolean("ativa")).thenReturn(true);
        when(rs.getTimestamp("pausada_em")).thenReturn(null);
        when(rs.getTimestamp("criada_em")).thenReturn(Timestamp.from(AGORA));
        when(jdbc.query(contains("FROM recorrencia"), anyMap(), any(RowMapper.class))).thenAnswer(inv -> {
            RowMapper<?> mapper = inv.getArgument(2);
            return List.of(mapper.mapRow(rs, 0));
        });
        when(jdbc.queryForObject(contains("COUNT(*) FROM recorrencia"), anyMap(), eq(Integer.class))).thenReturn(1, 0);
        RecorrenciaJdbc repo = new RecorrenciaJdbc(jdbc);

        assertThat(repo.inserir(recorrencia())).isEqualTo(4L);
        assertThat(repo.buscar(42L, 4L).orElseThrow().dia().dia()).isZero();
        assertThat(repo.listar(42L)).hasSize(1);
        assertThat(repo.listarAtivas()).hasSize(1);
        assertThat(repo.existeDescricao(42L, "Benefício Flash")).isTrue();
        assertThat(repo.existeDescricao(42L, "Outra")).isFalse();
        repo.atualizar(recorrencia());
    }

    @Test
    void recorrenciaDuplicadaFalha() {
        when(jdbc.update(contains("INSERT INTO recorrencia"), any(SqlParameterSource.class), any(KeyHolder.class),
                any(String[].class))).thenThrow(new DuplicateKeyException("dup")).thenReturn(1);
        RecorrenciaJdbc repo = new RecorrenciaJdbc(jdbc);

        assertThatThrownBy(() -> repo.inserir(recorrencia())).isInstanceOf(NomeEmUsoException.class);
        assertThatThrownBy(() -> repo.inserir(recorrencia())).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void ocorrenciaLeERegistra() {
        when(jdbc.query(contains("FROM recorrencia_ocorrencia"), anyMap(), any(RowMapper.class))).thenAnswer(inv -> {
            RowMapper<?> mapper = inv.getArgument(2);
            ResultSet rs = mock(ResultSet.class);
            when(rs.getLong("lancamento_id")).thenReturn(11L);
            return List.of(mapper.mapRow(rs, 0));
        });
        when(jdbc.update(contains("INSERT INTO recorrencia_ocorrencia"), any(SqlParameterSource.class)))
                .thenReturn(1)
                .thenThrow(new DuplicateKeyException("dup"));
        RecorrenciaJdbc repo = new RecorrenciaJdbc(jdbc);

        assertThat(repo.ocorrencia(42L, 4L, 2026, 11)).contains(11L);
        repo.registrarOcorrencia(42L, 4L, 2026, 11, 11L);
        assertThatThrownBy(() -> repo.registrarOcorrencia(42L, 4L, 2026, 11, 11L))
                .isInstanceOf(IllegalStateException.class);
    }

    private static Transferencia transferencia() {
        return new Transferencia(9L, 42L, 1L, 2L, new Dinheiro(new BigDecimal("100.00")),
                LocalDate.of(2026, 11, 10), "Reserva");
    }

    private static Orcamento orcamento() {
        return new Orcamento(5L, 42L, 3L, 2026, 11, new Dinheiro(new BigDecimal("1000.00")));
    }

    private static Meta meta() {
        return new Meta(7L, 42L, "Viagem", new Dinheiro(new BigDecimal("1000.00")), null, AGORA);
    }

    private static Aporte aporte() {
        return new Aporte(11L, 42L, 7L, new Dinheiro(new BigDecimal("250.00")), LocalDate.of(2026, 11, 10));
    }

    private static Recorrencia recorrencia() {
        return new Recorrencia(4L, 42L, 2L, 3L, Natureza.RECEITA, null,
                new Dinheiro(new BigDecimal("1570.00")), "Benefício Flash", DiaDoMes.ultimo(), true, null, AGORA);
    }
}
