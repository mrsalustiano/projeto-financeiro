package br.com.fincontrol.infrastructure.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.ResultSet;
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

import br.com.fincontrol.domain.model.RegraClassificacao;

@ExtendWith(MockitoExtension.class)
class OfxJdbcTest {

    @Mock private NamedParameterJdbcTemplate jdbc;

    @Test
    void regraListaExisteEInsere() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("id")).thenReturn(1L);
        when(rs.getLong("cliente_id")).thenReturn(42L);
        when(rs.getString("termo")).thenReturn("ITAU");
        when(rs.getInt("dia_inicio")).thenReturn(15);
        when(rs.getInt("dia_fim")).thenReturn(15);
        when(rs.getBigDecimal("valor_minimo")).thenReturn(new BigDecimal("4800.00"));
        when(rs.getBigDecimal("valor_maximo")).thenReturn(new BigDecimal("4800.00"));
        when(rs.getString("categoria")).thenReturn("Salário");
        when(jdbc.query(contains("FROM regra_classificacao"), anyMap(), any(RowMapper.class))).thenAnswer(inv -> {
            RowMapper<?> mapper = inv.getArgument(2);
            return List.of(mapper.mapRow(rs, 0));
        });
        when(jdbc.queryForObject(contains("SELECT COUNT(*)"), anyMap(), eq(Integer.class))).thenReturn(1, 0);
        when(jdbc.update(contains("INSERT INTO regra_classificacao"), any(SqlParameterSource.class),
                any(KeyHolder.class), any(String[].class))).thenAnswer(inv -> {
                    KeyHolder chaves = inv.getArgument(2);
                    chaves.getKeyList().add(Map.of("id", 7));
                    return 1;
                });
        RegraClassificacaoJdbc repo = new RegraClassificacaoJdbc(jdbc);

        RegraClassificacao regra = repo.listar(42L).getFirst();
        assertThat(regra.termo()).isEqualTo("ITAU");
        assertThat(regra.categoria()).isEqualTo("Salário");
        assertThat(regra.valorMinimo()).isEqualByComparingTo("4800.00");
        assertThat(repo.existe(42L, "ITAU", 15, 15)).isTrue();
        assertThat(repo.existe(42L, "ITAU", 15, 15)).isFalse();
        assertThat(repo.inserir(regra)).isEqualTo(7L);
    }

    @Test
    void regraSemChaveFalha() {
        when(jdbc.update(contains("INSERT INTO regra_classificacao"), any(SqlParameterSource.class),
                any(KeyHolder.class), any(String[].class))).thenReturn(1);
        RegraClassificacaoJdbc repo = new RegraClassificacaoJdbc(jdbc);

        assertThatThrownBy(() -> repo.inserir(new RegraClassificacao(0L, 42L, "ITAU", 15, 15,
                new BigDecimal("4800.00"), new BigDecimal("4800.00"), "Salário")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void lancamentoReconheceFitid() {
        when(jdbc.queryForObject(contains("fitid = :fitid"), anyMap(), eq(Integer.class))).thenReturn(1, 0);
        LancamentoJdbc repo = new LancamentoJdbc(jdbc);

        assertThat(repo.existeFitid(42L, 1L, "2026101500037")).isTrue();
        assertThat(repo.existeFitid(42L, 1L, "2026101500037")).isFalse();
    }

    @Test
    void contaGuardaIdentificador() {
        when(jdbc.update(contains("identificador_conta"), anyMap())).thenReturn(1);
        ContaJdbc repo = new ContaJdbc(jdbc);

        repo.atualizarIdentificador(42L, 1L, "7224833363");

        verify(jdbc).update(contains("identificador_conta"), anyMap());
    }
}
