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

import br.com.fincontrol.domain.model.Comprovante;
import br.com.fincontrol.domain.model.TipoComprovante;
import br.com.fincontrol.domain.service.ComprovanteRepositorio;

@Repository
public class ComprovanteJdbc implements ComprovanteRepositorio {

    private static final String COLUNAS = """
            SELECT id, cliente_id, lancamento_id, nome_arquivo, tipo, tamanho, storage_key, criado_em, criado_por
            FROM comprovante
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public ComprovanteJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Comprovante> listar(long clienteId, long lancamentoId) {
        return jdbc.query(
                COLUNAS + " WHERE cliente_id = :clienteId AND lancamento_id = :lancamentoId ORDER BY id",
                Map.of("clienteId", clienteId, "lancamentoId", lancamentoId),
                (rs, n) -> mapear(rs));
    }

    @Override
    public Optional<Comprovante> buscar(long clienteId, long id) {
        return jdbc.query(
                COLUNAS + " WHERE cliente_id = :clienteId AND id = :id",
                Map.of("clienteId", clienteId, "id", id),
                (rs, n) -> mapear(rs)).stream().findFirst();
    }

    @Override
    public long inserir(Comprovante comprovante) {
        KeyHolder chaves = new GeneratedKeyHolder();
        jdbc.update(
                """
                INSERT INTO comprovante
                    (cliente_id, lancamento_id, nome_arquivo, tipo, tamanho, storage_key, criado_em, criado_por)
                VALUES (:clienteId, :lancamentoId, :nomeArquivo, :tipo, :tamanho, :storageKey, :criadoEm, :criadoPor)
                """,
                new MapSqlParameterSource()
                        .addValue("clienteId", comprovante.clienteId())
                        .addValue("lancamentoId", comprovante.lancamentoId())
                        .addValue("nomeArquivo", comprovante.nomeArquivo())
                        .addValue("tipo", comprovante.tipo().name())
                        .addValue("tamanho", comprovante.tamanho())
                        .addValue("storageKey", comprovante.storageKey())
                        .addValue("criadoEm", Timestamp.from(comprovante.criadoEm()))
                        .addValue("criadoPor", comprovante.criadoPor()),
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
                "DELETE FROM comprovante WHERE cliente_id = :clienteId AND id = :id",
                Map.of("clienteId", clienteId, "id", id)) > 0;
    }

    private static Comprovante mapear(ResultSet rs) throws SQLException {
        return new Comprovante(
                rs.getLong("id"),
                rs.getLong("cliente_id"),
                rs.getLong("lancamento_id"),
                rs.getString("nome_arquivo"),
                TipoComprovante.valueOf(rs.getString("tipo")),
                rs.getLong("tamanho"),
                rs.getString("storage_key"),
                rs.getTimestamp("criado_em").toInstant(),
                rs.getString("criado_por"));
    }
}
