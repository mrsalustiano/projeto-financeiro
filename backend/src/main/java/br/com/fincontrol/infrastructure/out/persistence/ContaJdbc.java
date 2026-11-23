package br.com.fincontrol.infrastructure.out.persistence;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import br.com.fincontrol.domain.model.Conta;
import br.com.fincontrol.domain.model.Dinheiro;
import br.com.fincontrol.domain.model.Saldo;
import br.com.fincontrol.domain.model.TipoConta;
import br.com.fincontrol.domain.service.ContaRepositorio;
import br.com.fincontrol.domain.service.NomeEmUsoException;

@Repository
public class ContaJdbc implements ContaRepositorio {

    private static final String COLUNAS = """
            SELECT id, cliente_id, nome, banco, tipo, saldo_inicial, data_saldo, limite
            FROM conta
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public ContaJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Conta> listar(long clienteId) {
        return jdbc.query(COLUNAS + " WHERE cliente_id = :clienteId ORDER BY nome", Map.of("clienteId", clienteId), (rs, n) -> mapear(rs));
    }

    @Override
    public Optional<Conta> buscar(long clienteId, long id) {
        List<Conta> lista = jdbc.query(
                COLUNAS + " WHERE cliente_id = :clienteId AND id = :id",
                Map.of("clienteId", clienteId, "id", id),
                (rs, n) -> mapear(rs));
        return lista.stream().findFirst();
    }

    @Override
    public boolean existeNome(long clienteId, String nome) {
        Integer total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM conta WHERE cliente_id = :clienteId AND nome = :nome",
                Map.of("clienteId", clienteId, "nome", nome),
                Integer.class);
        return total != null && total > 0;
    }

    @Override
    public long inserir(Conta conta) {
        try {
            return inserirNova(conta);
        } catch (DuplicateKeyException ex) {
            throw new NomeEmUsoException();
        }
    }

    @Override
    public void atualizar(Conta conta) {
        try {
            jdbc.update(
                    """
                    UPDATE conta
                    SET nome = :nome, banco = :banco, tipo = :tipo, saldo_inicial = :saldo, data_saldo = :data, limite = :limite
                    WHERE cliente_id = :clienteId AND id = :id
                    """,
                    parametros(conta));
        } catch (DuplicateKeyException ex) {
            throw new NomeEmUsoException();
        }
    }

    @Override
    public boolean excluir(long clienteId, long id) {
        return jdbc.update(
                "DELETE FROM conta WHERE cliente_id = :clienteId AND id = :id",
                Map.of("clienteId", clienteId, "id", id)) == 1;
    }

    private long inserirNova(Conta conta) {
        KeyHolder chaves = new GeneratedKeyHolder();
        jdbc.update(
                """
                INSERT INTO conta (cliente_id, nome, banco, tipo, saldo_inicial, data_saldo, limite)
                VALUES (:clienteId, :nome, :banco, :tipo, :saldo, :data, :limite)
                """,
                parametros(conta),
                chaves,
                new String[] {"id"});
        Number id = chaves.getKey();
        if (id == null) {
            throw new IllegalStateException("id ausente");
        }
        return id.longValue();
    }

    private static MapSqlParameterSource parametros(Conta conta) {
        BigDecimal limite = conta.limite() == null ? null : conta.limite().valor();
        return new MapSqlParameterSource()
                .addValue("id", conta.id())
                .addValue("clienteId", conta.clienteId())
                .addValue("nome", conta.nome())
                .addValue("banco", conta.banco())
                .addValue("tipo", conta.tipo().name())
                .addValue("saldo", conta.saldoInicial().valor())
                .addValue("data", java.sql.Date.valueOf(conta.dataSaldo()))
                .addValue("limite", limite);
    }

    private static Conta mapear(java.sql.ResultSet rs) throws java.sql.SQLException {
        BigDecimal limite = rs.getBigDecimal("limite");
        return new Conta(
                rs.getLong("id"),
                rs.getLong("cliente_id"),
                rs.getString("nome"),
                rs.getString("banco"),
                TipoConta.valueOf(rs.getString("tipo")),
                new Saldo(rs.getBigDecimal("saldo_inicial")),
                rs.getDate("data_saldo").toLocalDate(),
                limite == null ? null : new Dinheiro(limite));
    }
}
