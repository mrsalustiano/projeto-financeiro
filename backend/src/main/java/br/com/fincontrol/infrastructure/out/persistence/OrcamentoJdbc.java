package br.com.fincontrol.infrastructure.out.persistence;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import br.com.fincontrol.domain.model.Dinheiro;
import br.com.fincontrol.domain.model.Orcamento;
import br.com.fincontrol.domain.service.OrcamentoRepositorio;

@Repository
public class OrcamentoJdbc implements OrcamentoRepositorio {

    private static final String COLUNAS = """
            SELECT id, cliente_id, categoria_id, ano, mes, limite
            FROM orcamento
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public OrcamentoJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Orcamento> buscar(long clienteId, long categoriaId, int ano, int mes) {
        List<Orcamento> lista = jdbc.query(
                COLUNAS + " WHERE cliente_id = :clienteId AND categoria_id = :categoriaId AND ano = :ano AND mes = :mes",
                Map.of("clienteId", clienteId, "categoriaId", categoriaId, "ano", ano, "mes", mes),
                (rs, n) -> mapear(rs));
        return lista.stream().findFirst();
    }

    @Override
    public List<Orcamento> listar(long clienteId, int ano, int mes) {
        return jdbc.query(
                COLUNAS + " WHERE cliente_id = :clienteId AND ano = :ano AND mes = :mes ORDER BY categoria_id",
                Map.of("clienteId", clienteId, "ano", ano, "mes", mes),
                (rs, n) -> mapear(rs));
    }

    @Override
    public long salvar(Orcamento orcamento) {
        jdbc.update(
                """
                INSERT INTO orcamento (cliente_id, categoria_id, ano, mes, limite)
                VALUES (:clienteId, :categoriaId, :ano, :mes, :limite) AS novo
                ON DUPLICATE KEY UPDATE limite = novo.limite
                """,
                new MapSqlParameterSource()
                        .addValue("clienteId", orcamento.clienteId())
                        .addValue("categoriaId", orcamento.categoriaId())
                        .addValue("ano", orcamento.ano())
                        .addValue("mes", orcamento.mes())
                        .addValue("limite", orcamento.limite().valor()));
        return buscar(orcamento.clienteId(), orcamento.categoriaId(), orcamento.ano(), orcamento.mes())
                .map(Orcamento::id)
                .orElseThrow();
    }

    @Override
    public boolean excluir(long clienteId, long categoriaId, int ano, int mes) {
        return jdbc.update(
                "DELETE FROM orcamento WHERE cliente_id = :clienteId AND categoria_id = :categoriaId AND ano = :ano AND mes = :mes",
                Map.of("clienteId", clienteId, "categoriaId", categoriaId, "ano", ano, "mes", mes)) == 1;
    }

    private static Orcamento mapear(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new Orcamento(
                rs.getLong("id"),
                rs.getLong("cliente_id"),
                rs.getLong("categoria_id"),
                rs.getInt("ano"),
                rs.getInt("mes"),
                new Dinheiro(rs.getBigDecimal("limite")));
    }
}
