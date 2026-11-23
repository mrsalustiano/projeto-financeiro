package br.com.fincontrol.infrastructure.out.persistence;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import br.com.fincontrol.domain.model.FechamentoMes;
import br.com.fincontrol.domain.service.FechamentoMesRepositorio;

@Repository
public class FechamentoMesJdbc implements FechamentoMesRepositorio {

    private static final String COLUNAS = """
            SELECT id, cliente_id, ano, mes, fechado_em, fechado_por
            FROM fechamento_mes
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public FechamentoMesJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<FechamentoMes> buscar(long clienteId, int ano, int mes) {
        List<FechamentoMes> lista = jdbc.query(
                COLUNAS + " WHERE cliente_id = :clienteId AND ano = :ano AND mes = :mes",
                Map.of("clienteId", clienteId, "ano", ano, "mes", mes),
                (rs, n) -> mapear(rs));
        return lista.stream().findFirst();
    }

    @Override
    public List<FechamentoMes> listar(long clienteId) {
        return jdbc.query(
                COLUNAS + " WHERE cliente_id = :clienteId ORDER BY ano, mes",
                Map.of("clienteId", clienteId),
                (rs, n) -> mapear(rs));
    }

    @Override
    public long inserir(FechamentoMes fechamento) {
        KeyHolder chaves = new GeneratedKeyHolder();
        jdbc.update(
                """
                INSERT INTO fechamento_mes (cliente_id, ano, mes, fechado_em, fechado_por)
                VALUES (:clienteId, :ano, :mes, :fechadoEm, :fechadoPor)
                """,
                new MapSqlParameterSource()
                        .addValue("clienteId", fechamento.clienteId())
                        .addValue("ano", fechamento.ano())
                        .addValue("mes", fechamento.mes())
                        .addValue("fechadoEm", Timestamp.from(fechamento.fechadoEm()))
                        .addValue("fechadoPor", fechamento.fechadoPor()),
                chaves,
                new String[] {"id"});
        Number id = chaves.getKey();
        if (id == null) {
            throw new IllegalStateException("id ausente");
        }
        return id.longValue();
    }

    @Override
    public boolean excluir(long clienteId, int ano, int mes) {
        return jdbc.update(
                "DELETE FROM fechamento_mes WHERE cliente_id = :clienteId AND ano = :ano AND mes = :mes",
                Map.of("clienteId", clienteId, "ano", ano, "mes", mes)) == 1;
    }

    private static FechamentoMes mapear(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new FechamentoMes(
                rs.getLong("id"),
                rs.getLong("cliente_id"),
                rs.getInt("ano"),
                rs.getInt("mes"),
                rs.getTimestamp("fechado_em").toInstant(),
                rs.getString("fechado_por"));
    }
}
