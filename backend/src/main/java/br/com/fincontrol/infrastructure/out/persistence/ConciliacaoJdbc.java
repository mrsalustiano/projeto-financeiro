package br.com.fincontrol.infrastructure.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import br.com.fincontrol.domain.model.Conciliacao;
import br.com.fincontrol.domain.service.ConciliacaoRepositorio;

@Repository
public class ConciliacaoJdbc implements ConciliacaoRepositorio {

    private static final String COLUNAS = """
            SELECT id, cliente_id, conta_id, lancamento_id, extrato_id, conciliado_em, conciliado_por
            FROM conciliacao
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public ConciliacaoJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Conciliacao> listar(long clienteId) {
        return jdbc.query(
                COLUNAS + " WHERE cliente_id = :clienteId ORDER BY id",
                Map.of("clienteId", clienteId),
                (rs, n) -> mapear(rs));
    }

    @Override
    public Optional<Conciliacao> buscar(long clienteId, long id) {
        return jdbc.query(
                COLUNAS + " WHERE cliente_id = :clienteId AND id = :id",
                Map.of("clienteId", clienteId, "id", id),
                (rs, n) -> mapear(rs)).stream().findFirst();
    }

    @Override
    public Optional<Conciliacao> buscarPorLancamento(long clienteId, long lancamentoId) {
        return jdbc.query(
                COLUNAS + """
                 WHERE cliente_id = :clienteId AND (lancamento_id = :lancamentoId OR extrato_id = :lancamentoId)
                """,
                Map.of("clienteId", clienteId, "lancamentoId", lancamentoId),
                (rs, n) -> mapear(rs)).stream().findFirst();
    }

    @Override
    public long inserir(Conciliacao conciliacao) {
        KeyHolder chaves = new GeneratedKeyHolder();
        jdbc.update(
                """
                INSERT INTO conciliacao
                    (cliente_id, conta_id, lancamento_id, extrato_id, conciliado_em, conciliado_por)
                VALUES (:clienteId, :contaId, :lancamentoId, :extratoId, :conciliadoEm, :conciliadoPor)
                """,
                new MapSqlParameterSource()
                        .addValue("clienteId", conciliacao.clienteId())
                        .addValue("contaId", conciliacao.contaId())
                        .addValue("lancamentoId", conciliacao.lancamentoId())
                        .addValue("extratoId", conciliacao.extratoId())
                        .addValue("conciliadoEm", Timestamp.from(conciliacao.conciliadoEm()))
                        .addValue("conciliadoPor", conciliacao.conciliadoPor()),
                chaves,
                new String[] {"id"});
        Number id = chaves.getKey();
        if (id == null) {
            throw new IllegalStateException("id ausente");
        }
        return id.longValue();
    }

    @Override
    public boolean excluir(long clienteId, long id) {
        return jdbc.update(
                "DELETE FROM conciliacao WHERE cliente_id = :clienteId AND id = :id",
                Map.of("clienteId", clienteId, "id", id)) > 0;
    }

    private static Conciliacao mapear(ResultSet rs) throws SQLException {
        return new Conciliacao(
                rs.getLong("id"),
                rs.getLong("cliente_id"),
                rs.getLong("conta_id"),
                rs.getLong("lancamento_id"),
                rs.getLong("extrato_id"),
                rs.getTimestamp("conciliado_em").toInstant(),
                rs.getString("conciliado_por"));
    }
}
