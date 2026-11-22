package br.com.fincontrol.infrastructure.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
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

import br.com.fincontrol.domain.model.Conta;
import br.com.fincontrol.domain.model.Natureza;
import br.com.fincontrol.domain.model.Saldo;
import br.com.fincontrol.domain.model.TipoConta;
import br.com.fincontrol.domain.service.NomeEmUsoException;

@ExtendWith(MockitoExtension.class)
class CatalogoJdbcTest {

    @Mock private NamedParameterJdbcTemplate jdbc;

    @Test
    void categoriaMapeiaInsereEExclui() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("id")).thenReturn(1L);
        when(rs.getLong("cliente_id")).thenReturn(42L);
        when(rs.getString("nome")).thenReturn("Salário");
        when(rs.getString("natureza")).thenReturn("RECEITA");
        when(jdbc.query(contains("FROM categoria"), anyMap(), any(RowMapper.class))).thenAnswer(inv -> {
            RowMapper<?> mapper = inv.getArgument(2);
            return List.of(mapper.mapRow(rs, 0));
        });
        when(jdbc.queryForObject(contains("COUNT"), anyMap(), org.mockito.ArgumentMatchers.eq(Integer.class))).thenReturn(1);
        when(jdbc.update(contains("DELETE"), anyMap())).thenReturn(0);
        when(jdbc.update(contains("UPDATE categoria"), anyMap())).thenThrow(new DuplicateKeyException("dup"));
        CategoriaJdbc repo = new CategoriaJdbc(jdbc);

        assertThat(repo.listar(42L)).hasSize(1);
        assertThat(repo.buscar(42L, 1L).orElseThrow().natureza()).isEqualTo(Natureza.RECEITA);
        assertThat(repo.existeNome(42L, "Salário")).isTrue();
        assertThat(repo.excluir(42L, 1L)).isFalse();
        assertThatThrownBy(() -> repo.atualizar(42L, 1L, "Salário", Natureza.RECEITA)).isInstanceOf(NomeEmUsoException.class);
    }

    @Test
    void categoriaInsereERecusaDuplicidade() {
        when(jdbc.update(contains("INSERT INTO categoria"), any(SqlParameterSource.class), any(KeyHolder.class), any(String[].class)))
                .thenAnswer(inv -> {
                    KeyHolder chaves = inv.getArgument(2);
                    chaves.getKeyList().add(Map.of("id", 4));
                    return 1;
                })
                .thenThrow(new DuplicateKeyException("dup"))
                .thenAnswer(inv -> 1);
        CategoriaJdbc repo = new CategoriaJdbc(jdbc);

        assertThat(repo.inserir(42L, "Lazer", Natureza.DESPESA)).isEqualTo(4L);
        assertThatThrownBy(() -> repo.inserir(42L, "Lazer", Natureza.DESPESA)).isInstanceOf(NomeEmUsoException.class);
        when(jdbc.update(anyString(), any(SqlParameterSource.class), any(KeyHolder.class), any(String[].class)))
                .thenAnswer(inv -> 1);
        assertThatThrownBy(() -> repo.inserir(42L, "Outra", Natureza.DESPESA)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void contaMapeiaFlashSemLimite() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("id")).thenReturn(8L);
        when(rs.getLong("cliente_id")).thenReturn(42L);
        when(rs.getString("nome")).thenReturn("Flash");
        when(rs.getString("banco")).thenReturn("Flash");
        when(rs.getString("tipo")).thenReturn("BENEFICIO", "CARTAO_CREDITO");
        when(rs.getBigDecimal("saldo_inicial")).thenReturn(new BigDecimal("0.00"));
        when(rs.getDate("data_saldo")).thenReturn(Date.valueOf(LocalDate.of(2026, 1, 1)));
        when(rs.getBigDecimal("limite")).thenReturn(null, new BigDecimal("100.00"));
        when(jdbc.query(contains("FROM conta"), anyMap(), any(RowMapper.class))).thenAnswer(inv -> {
            RowMapper<?> mapper = inv.getArgument(2);
            return List.of(mapper.mapRow(rs, 0));
        });
        when(jdbc.queryForObject(contains("COUNT"), anyMap(), org.mockito.ArgumentMatchers.eq(Integer.class))).thenReturn(0, null);
        when(jdbc.update(contains("DELETE FROM conta"), anyMap())).thenReturn(1);
        when(jdbc.update(contains("UPDATE conta"), any(SqlParameterSource.class))).thenThrow(new DuplicateKeyException("dup"));
        ContaJdbc repo = new ContaJdbc(jdbc);

        Conta flash = repo.listar(42L).getFirst();
        assertThat(flash.limite()).isNull();
        assertThat(flash.tipo()).isEqualTo(TipoConta.BENEFICIO);
        assertThat(repo.buscar(42L, 8L)).isPresent();
        assertThat(repo.existeNome(42L, "Flash")).isFalse();
        assertThat(repo.existeNome(42L, "Flash")).isFalse();
        assertThat(repo.excluir(42L, 8L)).isTrue();
        assertThatThrownBy(() -> repo.atualizar(flash)).isInstanceOf(NomeEmUsoException.class);
    }

    @Test
    void contaInsere() {
        when(jdbc.update(contains("INSERT INTO conta"), any(SqlParameterSource.class), any(KeyHolder.class), any(String[].class)))
                .thenAnswer(inv -> {
                    KeyHolder chaves = inv.getArgument(2);
                    chaves.getKeyList().add(Map.of("id", 8));
                    return 1;
                })
                .thenThrow(new DuplicateKeyException("dup"));
        ContaJdbc repo = new ContaJdbc(jdbc);
        Conta flash = new Conta(0L, 42L, "Flash", "Flash", TipoConta.BENEFICIO, Saldo.zero(), LocalDate.of(2026, 1, 1), null);

        assertThat(repo.inserir(flash)).isEqualTo(8L);
        assertThatThrownBy(() -> repo.inserir(flash)).isInstanceOf(NomeEmUsoException.class);
    }
}
