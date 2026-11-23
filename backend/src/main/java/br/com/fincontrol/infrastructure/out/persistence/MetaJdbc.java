package br.com.fincontrol.infrastructure.out.persistence;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import br.com.fincontrol.domain.model.Aporte;
import br.com.fincontrol.domain.model.Dinheiro;
import br.com.fincontrol.domain.model.Meta;
import br.com.fincontrol.domain.service.MetaRepositorio;
import br.com.fincontrol.domain.service.NomeEmUsoException;

@Repository
public class MetaJdbc implements MetaRepositorio {

    private static final String COLUNAS = """
            SELECT id, cliente_id, nome, valor_alvo, data_alvo, criada_em
            FROM meta
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public MetaJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public long inserir(Meta meta) {
        KeyHolder chaves = new GeneratedKeyHolder();
        try {
            jdbc.update(
                    """
                    INSERT INTO meta (cliente_id, nome, valor_alvo, data_alvo, criada_em)
                    VALUES (:clienteId, :nome, :valorAlvo, :dataAlvo, :criadaEm)
                    """,
                    new MapSqlParameterSource()
                            .addValue("clienteId", meta.clienteId())
                            .addValue("nome", meta.nome())
                            .addValue("valorAlvo", meta.valorAlvo().valor())
                            .addValue("dataAlvo", meta.dataAlvo() == null ? null : java.sql.Date.valueOf(meta.dataAlvo()))
                            .addValue("criadaEm", Timestamp.from(meta.criadaEm())),
                    chaves,
                    new String[] {"id"});
        } catch (DuplicateKeyException ex) {
            throw new NomeEmUsoException();
        }
        Number id = chaves.getKey();
        if (id == null) {
            throw new IllegalStateException("id ausente");
        }
        return id.longValue();
    }

    @Override
    public Optional<Meta> buscar(long clienteId, long id) {
        List<Meta> lista = jdbc.query(
                COLUNAS + " WHERE cliente_id = :clienteId AND id = :id",
                Map.of("clienteId", clienteId, "id", id),
                (rs, n) -> mapear(rs));
        return lista.stream().findFirst();
    }

    @Override
    public List<Meta> listar(long clienteId) {
        return jdbc.query(
                COLUNAS + " WHERE cliente_id = :clienteId ORDER BY nome",
                Map.of("clienteId", clienteId),
                (rs, n) -> mapear(rs));
    }

    @Override
    public boolean existeNome(long clienteId, String nome) {
        Integer total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM meta WHERE cliente_id = :clienteId AND nome = :nome",
                Map.of("clienteId", clienteId, "nome", nome),
                Integer.class);
        return total != null && total > 0;
    }

    @Override
    public long inserirAporte(Aporte aporte) {
        KeyHolder chaves = new GeneratedKeyHolder();
        jdbc.update(
                """
                INSERT INTO aporte (cliente_id, meta_id, valor, data)
                VALUES (:clienteId, :metaId, :valor, :data)
                """,
                new MapSqlParameterSource()
                        .addValue("clienteId", aporte.clienteId())
                        .addValue("metaId", aporte.metaId())
                        .addValue("valor", aporte.valor().valor())
                        .addValue("data", java.sql.Date.valueOf(aporte.data())),
                chaves,
                new String[] {"id"});
        Number id = chaves.getKey();
        if (id == null) {
            throw new IllegalStateException("id ausente");
        }
        return id.longValue();
    }

    @Override
    public List<Aporte> listarAportes(long clienteId, long metaId) {
        return jdbc.query(
                """
                SELECT id, cliente_id, meta_id, valor, data
                FROM aporte
                WHERE cliente_id = :clienteId AND meta_id = :metaId
                ORDER BY data, id
                """,
                Map.of("clienteId", clienteId, "metaId", metaId),
                (rs, n) -> new Aporte(
                        rs.getLong("id"),
                        rs.getLong("cliente_id"),
                        rs.getLong("meta_id"),
                        new Dinheiro(rs.getBigDecimal("valor")),
                        rs.getDate("data").toLocalDate()));
    }

    @Override
    public BigDecimal totalAportado(long clienteId, long metaId) {
        BigDecimal total = jdbc.queryForObject(
                "SELECT COALESCE(SUM(valor), 0) FROM aporte WHERE cliente_id = :clienteId AND meta_id = :metaId",
                Map.of("clienteId", clienteId, "metaId", metaId),
                BigDecimal.class);
        return total == null ? BigDecimal.ZERO : total;
    }

    private static Meta mapear(java.sql.ResultSet rs) throws java.sql.SQLException {
        java.sql.Date dataAlvo = rs.getDate("data_alvo");
        return new Meta(
                rs.getLong("id"),
                rs.getLong("cliente_id"),
                rs.getString("nome"),
                new Dinheiro(rs.getBigDecimal("valor_alvo")),
                dataAlvo == null ? null : dataAlvo.toLocalDate(),
                rs.getTimestamp("criada_em").toInstant());
    }
}
