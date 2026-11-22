package br.com.fincontrol.infrastructure.out.persistence;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import br.com.fincontrol.domain.model.Cliente;
import br.com.fincontrol.domain.service.ClienteRepositorio;

@Repository
public class ClienteJdbc implements ClienteRepositorio {

    private final NamedParameterJdbcTemplate jdbc;

    public ClienteJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Cliente> buscarPorId(long id) {
        List<Cliente> lista = jdbc.query(
                "SELECT id, nome, ativo FROM cliente WHERE id = :id",
                Map.of("id", id),
                (rs, numero) -> new Cliente(rs.getLong("id"), rs.getString("nome"), rs.getBoolean("ativo")));
        return lista.stream().findFirst();
    }

    @Override
    public long inserir(String nome, boolean ativo) {
        KeyHolder chaves = new GeneratedKeyHolder();
        jdbc.update(
                "INSERT INTO cliente (nome, ativo) VALUES (:nome, :ativo)",
                new MapSqlParameterSource().addValue("nome", nome).addValue("ativo", ativo),
                chaves,
                new String[] {"id"});
        Number id = chaves.getKey();
        if (id == null) {
            throw new IllegalStateException("id ausente");
        }
        return id.longValue();
    }
}
