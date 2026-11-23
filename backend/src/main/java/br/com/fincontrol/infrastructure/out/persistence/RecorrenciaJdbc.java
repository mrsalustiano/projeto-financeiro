package br.com.fincontrol.infrastructure.out.persistence;

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

import br.com.fincontrol.domain.model.DiaDoMes;
import br.com.fincontrol.domain.model.Dinheiro;
import br.com.fincontrol.domain.model.Natureza;
import br.com.fincontrol.domain.model.Recorrencia;
import br.com.fincontrol.domain.model.TipoDespesa;
import br.com.fincontrol.domain.service.NomeEmUsoException;
import br.com.fincontrol.domain.service.RecorrenciaRepositorio;

@Repository
public class RecorrenciaJdbc implements RecorrenciaRepositorio {

    private static final String COLUNAS = """
            SELECT id, cliente_id, conta_id, categoria_id, natureza, tipo_despesa, valor, descricao,
                   dia_do_mes, ativa, pausada_em, criada_em
            FROM recorrencia
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public RecorrenciaJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public long inserir(Recorrencia recorrencia) {
        KeyHolder chaves = new GeneratedKeyHolder();
        try {
            jdbc.update(
                    """
                    INSERT INTO recorrencia (cliente_id, conta_id, categoria_id, natureza, tipo_despesa, valor,
                                             descricao, dia_do_mes, ativa, pausada_em, criada_em)
                    VALUES (:clienteId, :contaId, :categoriaId, :natureza, :tipoDespesa, :valor,
                            :descricao, :dia, :ativa, :pausadaEm, :criadaEm)
                    """,
                    parametros(recorrencia),
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
    public Optional<Recorrencia> buscar(long clienteId, long id) {
        List<Recorrencia> lista = jdbc.query(
                COLUNAS + " WHERE cliente_id = :clienteId AND id = :id",
                Map.of("clienteId", clienteId, "id", id),
                (rs, n) -> mapear(rs));
        return lista.stream().findFirst();
    }

    @Override
    public List<Recorrencia> listar(long clienteId) {
        return jdbc.query(
                COLUNAS + " WHERE cliente_id = :clienteId ORDER BY descricao",
                Map.of("clienteId", clienteId),
                (rs, n) -> mapear(rs));
    }

    @Override
    public List<Recorrencia> listarAtivas() {
        return jdbc.query(
                COLUNAS + " WHERE ativa = 1 ORDER BY id",
                Map.of(),
                (rs, n) -> mapear(rs));
    }

    @Override
    public boolean existeDescricao(long clienteId, String descricao) {
        Integer total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM recorrencia WHERE cliente_id = :clienteId AND descricao = :descricao",
                Map.of("clienteId", clienteId, "descricao", descricao),
                Integer.class);
        return total != null && total > 0;
    }

    @Override
    public void atualizar(Recorrencia recorrencia) {
        jdbc.update(
                """
                UPDATE recorrencia
                SET conta_id = :contaId, categoria_id = :categoriaId, natureza = :natureza,
                    tipo_despesa = :tipoDespesa, valor = :valor, descricao = :descricao,
                    dia_do_mes = :dia, ativa = :ativa, pausada_em = :pausadaEm
                WHERE cliente_id = :clienteId AND id = :id
                """,
                parametros(recorrencia));
    }

    @Override
    public Optional<Long> ocorrencia(long clienteId, long recorrenciaId, int ano, int mes) {
        List<Long> lista = jdbc.query(
                """
                SELECT lancamento_id FROM recorrencia_ocorrencia
                WHERE cliente_id = :clienteId AND recorrencia_id = :recorrenciaId AND ano = :ano AND mes = :mes
                """,
                Map.of("clienteId", clienteId, "recorrenciaId", recorrenciaId, "ano", ano, "mes", mes),
                (rs, n) -> rs.getLong("lancamento_id"));
        return lista.stream().findFirst();
    }

    @Override
    public void registrarOcorrencia(long clienteId, long recorrenciaId, int ano, int mes, long lancamentoId) {
        try {
            jdbc.update(
                    """
                    INSERT INTO recorrencia_ocorrencia (cliente_id, recorrencia_id, ano, mes, lancamento_id)
                    VALUES (:clienteId, :recorrenciaId, :ano, :mes, :lancamentoId)
                    """,
                    new MapSqlParameterSource()
                            .addValue("clienteId", clienteId)
                            .addValue("recorrenciaId", recorrenciaId)
                            .addValue("ano", ano)
                            .addValue("mes", mes)
                            .addValue("lancamentoId", lancamentoId));
        } catch (DuplicateKeyException ex) {
            throw new IllegalStateException("Ocorrência repetida.", ex);
        }
    }

    private static MapSqlParameterSource parametros(Recorrencia recorrencia) {
        return new MapSqlParameterSource()
                .addValue("id", recorrencia.id())
                .addValue("clienteId", recorrencia.clienteId())
                .addValue("contaId", recorrencia.contaId())
                .addValue("categoriaId", recorrencia.categoriaId())
                .addValue("natureza", recorrencia.natureza().name())
                .addValue("tipoDespesa", recorrencia.tipoDespesa() == null ? null : recorrencia.tipoDespesa().name())
                .addValue("valor", recorrencia.valor().valor())
                .addValue("descricao", recorrencia.descricao())
                .addValue("dia", recorrencia.dia().dia())
                .addValue("ativa", recorrencia.ativa())
                .addValue("pausadaEm", recorrencia.pausadaEm() == null ? null : Timestamp.from(recorrencia.pausadaEm()))
                .addValue("criadaEm", Timestamp.from(recorrencia.criadaEm()));
    }

    private static Recorrencia mapear(java.sql.ResultSet rs) throws java.sql.SQLException {
        String tipo = rs.getString("tipo_despesa");
        Timestamp pausada = rs.getTimestamp("pausada_em");
        return new Recorrencia(
                rs.getLong("id"),
                rs.getLong("cliente_id"),
                rs.getLong("conta_id"),
                rs.getLong("categoria_id"),
                Natureza.valueOf(rs.getString("natureza")),
                tipo == null ? null : TipoDespesa.valueOf(tipo),
                new Dinheiro(rs.getBigDecimal("valor")),
                rs.getString("descricao"),
                new DiaDoMes(rs.getInt("dia_do_mes")),
                rs.getBoolean("ativa"),
                pausada == null ? null : pausada.toInstant(),
                rs.getTimestamp("criada_em").toInstant());
    }
}
