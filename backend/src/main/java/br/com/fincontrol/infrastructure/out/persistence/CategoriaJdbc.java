package br.com.fincontrol.infrastructure.out.persistence;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import br.com.fincontrol.domain.model.Categoria;
import br.com.fincontrol.domain.model.Natureza;
import br.com.fincontrol.domain.service.CategoriaRepositorio;
import br.com.fincontrol.domain.service.NomeEmUsoException;

@Repository
public class CategoriaJdbc implements CategoriaRepositorio {

    private final NamedParameterJdbcTemplate jdbc;

    public CategoriaJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Categoria> listar(long clienteId) {
        return jdbc.query(
                "SELECT id, cliente_id, nome, natureza FROM categoria WHERE cliente_id = :clienteId ORDER BY nome",
                Map.of("clienteId", clienteId),
                (rs, numero) -> new Categoria(
                        rs.getLong("id"), rs.getLong("cliente_id"), rs.getString("nome"), Natureza.valueOf(rs.getString("natureza"))));
    }

    @Override
    public Optional<Categoria> buscar(long clienteId, long id) {
        List<Categoria> lista = jdbc.query(
                "SELECT id, cliente_id, nome, natureza FROM categoria WHERE cliente_id = :clienteId AND id = :id",
                Map.of("clienteId", clienteId, "id", id),
                (rs, numero) -> new Categoria(
                        rs.getLong("id"), rs.getLong("cliente_id"), rs.getString("nome"), Natureza.valueOf(rs.getString("natureza"))));
        return lista.stream().findFirst();
    }

    @Override
    public boolean existeNome(long clienteId, String nome) {
        Integer total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM categoria WHERE cliente_id = :clienteId AND nome = :nome",
                Map.of("clienteId", clienteId, "nome", nome),
                Integer.class);
        return total != null && total > 0;
    }

    @Override
    public long inserir(long clienteId, String nome, Natureza natureza) {
        try {
            return inserirNova(clienteId, nome, natureza);
        } catch (DuplicateKeyException ex) {
            throw new NomeEmUsoException();
        }
    }

    @Override
    public void atualizar(long clienteId, long id, String nome, Natureza natureza) {
        try {
            jdbc.update(
                    "UPDATE categoria SET nome = :nome, natureza = :natureza WHERE cliente_id = :clienteId AND id = :id",
                    Map.of("nome", nome, "natureza", natureza.name(), "clienteId", clienteId, "id", id));
        } catch (DuplicateKeyException ex) {
            throw new NomeEmUsoException();
        }
    }

    @Override
    public boolean excluir(long clienteId, long id) {
        int linhas = jdbc.update(
                "DELETE FROM categoria WHERE cliente_id = :clienteId AND id = :id",
                Map.of("clienteId", clienteId, "id", id));
        return linhas == 1;
    }

    private long inserirNova(long clienteId, String nome, Natureza natureza) {
        KeyHolder chaves = new GeneratedKeyHolder();
        jdbc.update(
                "INSERT INTO categoria (cliente_id, nome, natureza) VALUES (:clienteId, :nome, :natureza)",
                new MapSqlParameterSource()
                        .addValue("clienteId", clienteId)
                        .addValue("nome", nome)
                        .addValue("natureza", natureza.name()),
                chaves,
                new String[] {"id"});
        Number id = chaves.getKey();
        if (id == null) {
            throw new IllegalStateException("id ausente");
        }
        return id.longValue();
    }
}
