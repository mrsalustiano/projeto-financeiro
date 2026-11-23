package br.com.fincontrol.infrastructure.out.persistence;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import br.com.fincontrol.domain.model.Dinheiro;
import br.com.fincontrol.domain.model.Conciliacao;
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
                   l.estornado_em, l.estorno_motivo, l.estornado_por, l.fitid
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
                                        descricao, parcela_id, numero_parcela, total_parcelas, fitid)
                VALUES (:clienteId, :contaId, :categoriaId, :natureza, :tipoDespesa, :valor, :data,
                        :descricao, :parcelaId, :numeroParcela, :totalParcelas, :fitid)
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
    public List<Lancamento> sugerirPar(long clienteId, Lancamento alvo) {
        return jdbc.query(
                COLUNAS + """
                 WHERE l.cliente_id = :clienteId
                   AND l.conta_id = :contaId
                   AND l.id <> :id
                   AND l.valor = :valor
                   AND l.natureza = :natureza
                   AND l.estornado_em IS NULL
                   AND ABS(DATEDIFF(l.data, :data)) <= :janela
                   AND ((:temFitid = 1 AND l.fitid IS NULL) OR (:temFitid = 0 AND l.fitid IS NOT NULL))
                   AND NOT EXISTS (SELECT 1 FROM conciliacao c
                                   WHERE c.cliente_id = l.cliente_id
                                     AND (c.lancamento_id = l.id OR c.extrato_id = l.id))
                 ORDER BY ABS(DATEDIFF(l.data, :data)), l.id
                """,
                new MapSqlParameterSource()
                        .addValue("clienteId", clienteId)
                        .addValue("contaId", alvo.contaId())
                        .addValue("id", alvo.id())
                        .addValue("valor", alvo.valor().valor())
                        .addValue("natureza", alvo.natureza().name())
                        .addValue("data", java.sql.Date.valueOf(alvo.data()))
                        .addValue("janela", Conciliacao.JANELA_DIAS)
                        .addValue("temFitid", alvo.fitid() == null ? 0 : 1),
                (rs, n) -> mapear(rs));
    }

    @Override
    public boolean existeFitid(long clienteId, long contaId, String fitid) {
        Integer total = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM lancamento
                WHERE cliente_id = :clienteId AND conta_id = :contaId AND fitid = :fitid
                """,
                Map.of("clienteId", clienteId, "contaId", contaId, "fitid", fitid),
                Integer.class);
        return total != null && total > 0;
    }

    @Override
    public void atualizar(Lancamento lancamento) {
        jdbc.update(
                """
                UPDATE lancamento
                SET conta_id = :contaId, categoria_id = :categoriaId, natureza = :natureza,
                    tipo_despesa = :tipoDespesa, valor = :valor, data = :data, descricao = :descricao,
                    estornado_em = :estornadoEm, estorno_motivo = :estornoMotivo, estornado_por = :estornadoPor,
                    fitid = :fitid
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
                       COALESCE((SELECT SUM(l.valor) FROM lancamento l
                                 WHERE l.cliente_id = c.cliente_id AND l.conta_id = c.id
                                   AND l.natureza = 'RECEITA' AND l.estornado_em IS NULL
                                   AND NOT EXISTS (SELECT 1 FROM conciliacao k
                                                   WHERE k.cliente_id = l.cliente_id AND k.extrato_id = l.id)), 0) AS creditos,
                       COALESCE((SELECT SUM(l.valor) FROM lancamento l
                                 WHERE l.cliente_id = c.cliente_id AND l.conta_id = c.id
                                   AND l.natureza = 'DESPESA' AND l.estornado_em IS NULL
                                   AND NOT EXISTS (SELECT 1 FROM conciliacao k
                                                   WHERE k.cliente_id = l.cliente_id AND k.extrato_id = l.id)), 0) AS debitos,
                       COALESCE((SELECT SUM(t.valor) FROM transferencia t
                                 WHERE t.cliente_id = c.cliente_id AND t.conta_destino_id = c.id), 0) AS entradas,
                       COALESCE((SELECT SUM(t.valor) FROM transferencia t
                                 WHERE t.cliente_id = c.cliente_id AND t.conta_origem_id = c.id), 0) AS saidas
                FROM conta c
                WHERE c.cliente_id = :clienteId AND c.id = :contaId
                """,
                Map.of("clienteId", clienteId, "contaId", contaId),
                (rs, n) -> new SaldoConta(
                        rs.getBigDecimal("saldo_inicial"),
                        rs.getBigDecimal("creditos"),
                        rs.getBigDecimal("debitos"),
                        rs.getBigDecimal("entradas"),
                        rs.getBigDecimal("saidas")));
        return lista.stream().findFirst();
    }

    @Override
    public BigDecimal gastoPorCategoria(long clienteId, long categoriaId, LocalDate de, LocalDate ate) {
        BigDecimal total = jdbc.queryForObject(
                """
                SELECT COALESCE(SUM(valor), 0)
                FROM lancamento
                WHERE cliente_id = :clienteId AND categoria_id = :categoriaId
                  AND natureza = 'DESPESA' AND estornado_em IS NULL
                  AND data BETWEEN :de AND :ate
                """,
                new MapSqlParameterSource()
                        .addValue("clienteId", clienteId)
                        .addValue("categoriaId", categoriaId)
                        .addValue("de", java.sql.Date.valueOf(de))
                        .addValue("ate", java.sql.Date.valueOf(ate)),
                BigDecimal.class);
        return total == null ? BigDecimal.ZERO : total;
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
                .addValue("estornadoPor", lancamento.estornadoPor())
                .addValue("fitid", lancamento.fitid());
    }

    private static Lancamento mapear(ResultSet rs) throws SQLException {
        long categoriaId = rs.getLong("categoria_id");
        Long categoria = rs.wasNull() ? null : categoriaId;
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
                categoria,
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
                rs.getString("estornado_por"),
                rs.getString("fitid"));
    }
}
