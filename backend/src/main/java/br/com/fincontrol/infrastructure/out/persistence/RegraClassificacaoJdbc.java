package br.com.fincontrol.infrastructure.out.persistence;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import br.com.fincontrol.domain.model.RegraClassificacao;
import br.com.fincontrol.domain.service.RegraClassificacaoRepositorio;

@Repository
public class RegraClassificacaoJdbc implements RegraClassificacaoRepositorio {

    private static final String COLUNAS = """
            SELECT id, cliente_id, termo, dia_inicio, dia_fim, valor_minimo, valor_maximo, categoria
            FROM regra_classificacao
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public RegraClassificacaoJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<RegraClassificacao> listar(long clienteId) {
        return jdbc.query(
                COLUNAS + " WHERE cliente_id = :clienteId ORDER BY id",
                Map.of("clienteId", clienteId),
                (rs, n) -> mapear(rs));
    }

    @Override
    public boolean existe(long clienteId, String termo, int diaInicio, int diaFim) {
        Integer total = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM regra_classificacao
                WHERE cliente_id = :clienteId AND termo = :termo AND dia_inicio = :diaInicio AND dia_fim = :diaFim
                """,
                Map.of("clienteId", clienteId, "termo", termo, "diaInicio", diaInicio, "diaFim", diaFim),
                Integer.class);
        return total != null && total > 0;
    }

    @Override
    public long inserir(RegraClassificacao regra) {
        KeyHolder chaves = new GeneratedKeyHolder();
        jdbc.update(
                """
                INSERT INTO regra_classificacao
                    (cliente_id, termo, dia_inicio, dia_fim, valor_minimo, valor_maximo, categoria)
                VALUES (:clienteId, :termo, :diaInicio, :diaFim, :valorMinimo, :valorMaximo, :categoria)
                """,
                new MapSqlParameterSource()
                        .addValue("clienteId", regra.clienteId())
                        .addValue("termo", regra.termo())
                        .addValue("diaInicio", regra.diaInicio())
                        .addValue("diaFim", regra.diaFim())
                        .addValue("valorMinimo", regra.valorMinimo())
                        .addValue("valorMaximo", regra.valorMaximo())
                        .addValue("categoria", regra.categoria()),
                chaves,
                new String[] {"id"});
        Number id = chaves.getKey();
        if (id == null) {
            throw new IllegalStateException("id ausente");
        }
        return id.longValue();
    }

    private static RegraClassificacao mapear(java.sql.ResultSet rs) throws java.sql.SQLException {
        BigDecimal minimo = rs.getBigDecimal("valor_minimo");
        BigDecimal maximo = rs.getBigDecimal("valor_maximo");
        return new RegraClassificacao(
                rs.getLong("id"),
                rs.getLong("cliente_id"),
                rs.getString("termo"),
                rs.getInt("dia_inicio"),
                rs.getInt("dia_fim"),
                minimo,
                maximo,
                rs.getString("categoria"));
    }
}
