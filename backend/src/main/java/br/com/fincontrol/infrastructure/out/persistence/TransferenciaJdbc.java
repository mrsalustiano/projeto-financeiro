package br.com.fincontrol.infrastructure.out.persistence;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import br.com.fincontrol.domain.model.Dinheiro;
import br.com.fincontrol.domain.model.Transferencia;
import br.com.fincontrol.domain.service.TransferenciaRepositorio;

@Repository
public class TransferenciaJdbc implements TransferenciaRepositorio {

    private static final String COLUNAS = """
            SELECT id, cliente_id, conta_origem_id, conta_destino_id, valor, data, descricao
            FROM transferencia
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public TransferenciaJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public long inserir(Transferencia transferencia) {
        KeyHolder chaves = new GeneratedKeyHolder();
        jdbc.update(
                """
                INSERT INTO transferencia (cliente_id, conta_origem_id, conta_destino_id, valor, data, descricao)
                VALUES (:clienteId, :origem, :destino, :valor, :data, :descricao)
                """,
                new MapSqlParameterSource()
                        .addValue("clienteId", transferencia.clienteId())
                        .addValue("origem", transferencia.contaOrigemId())
                        .addValue("destino", transferencia.contaDestinoId())
                        .addValue("valor", transferencia.valor().valor())
                        .addValue("data", java.sql.Date.valueOf(transferencia.data()))
                        .addValue("descricao", transferencia.descricao()),
                chaves,
                new String[] {"id"});
        Number id = chaves.getKey();
        if (id == null) {
            throw new IllegalStateException("id ausente");
        }
        return id.longValue();
    }

    @Override
    public Optional<Transferencia> buscar(long clienteId, long id) {
        List<Transferencia> lista = jdbc.query(
                COLUNAS + " WHERE cliente_id = :clienteId AND id = :id",
                Map.of("clienteId", clienteId, "id", id),
                (rs, n) -> mapear(rs));
        return lista.stream().findFirst();
    }

    @Override
    public List<Transferencia> listar(long clienteId) {
        return jdbc.query(
                COLUNAS + " WHERE cliente_id = :clienteId ORDER BY data, id",
                Map.of("clienteId", clienteId),
                (rs, n) -> mapear(rs));
    }

    private static Transferencia mapear(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new Transferencia(
                rs.getLong("id"),
                rs.getLong("cliente_id"),
                rs.getLong("conta_origem_id"),
                rs.getLong("conta_destino_id"),
                new Dinheiro(rs.getBigDecimal("valor")),
                rs.getDate("data").toLocalDate(),
                rs.getString("descricao"));
    }
}
