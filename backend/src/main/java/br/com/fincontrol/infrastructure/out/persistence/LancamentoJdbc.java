package br.com.fincontrol.infrastructure.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import br.com.fincontrol.domain.model.Dinheiro;
import br.com.fincontrol.domain.model.FiltroLancamento;
import br.com.fincontrol.domain.model.Lancamento;
import br.com.fincontrol.domain.model.Natureza;
import br.com.fincontrol.domain.model.Parcelamento;
import br.com.fincontrol.domain.model.SaldoConta;
import br.com.fincontrol.domain.model.TipoDespesa;
import br.com.fincontrol.domain.service.LancamentoRepositorio;

@Repository
public class LancamentoJdbc implements LancamentoRepositorio {

    private static final String COLUNAS = """
            SELECT l.id, l.cliente_id, l.conta_id, l.categoria_id, l.natureza, l.tipo_despesa, l.valor, l.data,
                   l.descricao, l.parcela_id, p.grupo AS grupo_parcela, l.numero_parcela, l.total_parcelas,
                   l.estornado_em, l.estorno_motivo, l.estornado_por
            FROM lancamento l
            LEFT JOIN parcela p ON p.id = l.parcela_id
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public LancamentoJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public long inserir(Lancamento lancamento) {
        KeyHolder chaves = new GeneratedKeyHolder();
        jdbc.update(
                """
                INSERT INTO lancamento (cliente_id, conta_id, categoria_id, natureza, tipo_despesa, valor, data,
                                        descricao, parcela_id, numero_parcela, total_parcelas)
                VALUES (:clienteId, :contaId, :categoriaId, :natureza, :tipoDespesa, :valor, :data,
                        :descricao, :parcelaId, :numeroParcela, :totalParcelas)
                """,
                parametros(lancamento),
                chaves,
                new String[] {"id"});
        Number id = chaves.getKey();
        if (id == null) {
            throw new IllegalStateException("id ausente");
        }
        return id.longValue();
    }

    @Override
    public long inserirParcela(long clienteId, Parcelamento parcelamento) {
        KeyHolder chaves = new GeneratedKeyHolder();
        jdbc.update(
                """
                INSERT INTO parcela (cliente_id, grupo, total, valor_total)
                VALUES (:clienteId, :grupo, :total, :valorTotal)
                """,
                new MapSqlParameterSource()
                        .addValue("clienteId", clienteId)
                        .addValue("grupo", parcelamento.grupo())
                        .addValue("total", parcelamento.total())
                        .addValue("valorTotal", parcelamento.valorTotal().valor()),
                chaves,
                new String[] {"id"});
        Number id = chaves.getKey();
        if (id == null) {
            throw new IllegalStateException("id ausente");
        }
        return id.longValue();
    }

    @Override
    public Optional<Lancamento> buscar(long clienteId, long id) {
        List<Lancamento> lista = jdbc.query(
                COLUNAS + " WHERE l.cliente_id = :clienteId AND l.id = :id",
                Map.of("clienteId", clienteId, "id", id),
                (rs, n) -> mapear(rs));
        return lista.stream().findFirst();
    }

    @Override
    public List<Lancamento> listar(long clienteId, FiltroLancamento filtro) {
        StringBuilder sql = new StringBuilder(COLUNAS).append(" WHERE l.cliente_id = :clienteId");
        MapSqlParameterSource params = new MapSqlParameterSource("clienteId", clienteId);
        if (filtro.contaId() != null) {
            sql.append(" AND l.conta_id = :contaId");
            params.addValue("contaId", filtro.contaId());
        }
        if (filtro.de() != null) {
            sql.append(" AND l.data >= :de");
            params.addValue("de", java.sql.Date.valueOf(filtro.de()));
        }
        if (filtro.ate() != null) {
            sql.append(" AND l.data <= :ate");
            params.addValue("ate", java.sql.Date.valueOf(filtro.ate()));
        }
        sql.append(" ORDER BY l.data, l.id");
        return jdbc.query(sql.toString(), params, (rs, n) -> mapear(rs));
    }

    @Override
    public List<Lancamento> listarPorParcela(long clienteId, long parcelaId) {
        return jdbc.query(
                COLUNAS + " WHERE l.cliente_id = :clienteId AND l.parcela_id = :parcelaId ORDER BY l.numero_parcela",
                Map.of("clienteId", clienteId, "parcelaId", parcelaId),
                (rs, n) -> mapear(rs));
    }

    @Override
    public void atualizar(Lancamento lancamento) {
        jdbc.update(
                """
                UPDATE lancamento
                SET conta_id = :contaId, categoria_id = :categoriaId, natureza = :natureza,
                    tipo_despesa = :tipoDespesa, valor = :valor, data = :data, descricao = :descricao,
                    estornado_em = :estornadoEm, estorno_motivo = :estornoMotivo, estornado_por = :estornadoPor
                WHERE cliente_id = :clienteId AND id = :id
                """,
                parametros(lancamento));
    }

    @Override
    public boolean excluir(long clienteId, long id) {
        return jdbc.update(
                "DELETE FROM lancamento WHERE cliente_id = :clienteId AND id = :id",
                Map.of("clienteId", clienteId, "id", id)) == 1;
    }

    @Override
    public Optional<SaldoConta> saldo(long clienteId, long contaId) {
        List<SaldoConta> lista = jdbc.query(
                """
                SELECT c.saldo_inicial,
                       COALESCE(SUM(CASE WHEN l.natureza = 'RECEITA' AND l.estornado_em IS NULL
                                         THEN l.valor ELSE 0 END), 0) AS creditos,
                       COALESCE(SUM(CASE WHEN l.natureza = 'DESPESA' AND l.estornado_em IS NULL
                                         THEN l.valor ELSE 0 END), 0) AS debitos
                FROM conta c
                LEFT JOIN lancamento l ON l.cliente_id = c.cliente_id AND l.conta_id = c.id
                WHERE c.cliente_id = :clienteId AND c.id = :contaId
                GROUP BY c.id, c.saldo_inicial
                """,
                Map.of("clienteId", clienteId, "contaId", contaId),
                (rs, n) -> new SaldoConta(
                        rs.getBigDecimal("saldo_inicial"),
                        rs.getBigDecimal("creditos"),
                        rs.getBigDecimal("debitos")));
        return lista.stream().findFirst();
    }

    private static MapSqlParameterSource parametros(Lancamento lancamento) {
        return new MapSqlParameterSource()
                .addValue("id", lancamento.id())
                .addValue("clienteId", lancamento.clienteId())
                .addValue("contaId", lancamento.contaId())
                .addValue("categoriaId", lancamento.categoriaId())
                .addValue("natureza", lancamento.natureza().name())
                .addValue("tipoDespesa", lancamento.tipoDespesa() == null ? null : lancamento.tipoDespesa().name())
                .addValue("valor", lancamento.valor().valor())
                .addValue("data", java.sql.Date.valueOf(lancamento.data()))
                .addValue("descricao", lancamento.descricao())
                .addValue("parcelaId", lancamento.parcelaId())
                .addValue("numeroParcela", lancamento.numeroParcela())
                .addValue("totalParcelas", lancamento.totalParcelas())
                .addValue("estornadoEm", lancamento.estornadoEm() == null
                        ? null : Timestamp.from(lancamento.estornadoEm()))
                .addValue("estornoMotivo", lancamento.estornoMotivo())
                .addValue("estornadoPor", lancamento.estornadoPor());
    }

    private static Lancamento mapear(ResultSet rs) throws SQLException {
        long parcelaId = rs.getLong("parcela_id");
        Long parcela = rs.wasNull() ? null : parcelaId;
        int numero = rs.getInt("numero_parcela");
        Integer numeroParcela = rs.wasNull() ? null : numero;
        int total = rs.getInt("total_parcelas");
        Integer totalParcelas = rs.wasNull() ? null : total;
        Timestamp estornado = rs.getTimestamp("estornado_em");
        Instant estornadoEm = estornado == null ? null : estornado.toInstant();
        String tipo = rs.getString("tipo_despesa");
        return new Lancamento(
                rs.getLong("id"),
                rs.getLong("cliente_id"),
                rs.getLong("conta_id"),
                rs.getLong("categoria_id"),
                Natureza.valueOf(rs.getString("natureza")),
                tipo == null ? null : TipoDespesa.valueOf(tipo),
                new Dinheiro(rs.getBigDecimal("valor")),
                rs.getDate("data").toLocalDate(),
                rs.getString("descricao"),
                parcela,
                rs.getString("grupo_parcela"),
                numeroParcela,
                totalParcelas,
                estornadoEm,
                rs.getString("estorno_motivo"),
                rs.getString("estornado_por"));
    }
}
