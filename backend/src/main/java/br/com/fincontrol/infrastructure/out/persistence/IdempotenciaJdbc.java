package br.com.fincontrol.infrastructure.out.persistence;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import br.com.fincontrol.domain.model.RegistroIdempotencia;
import br.com.fincontrol.domain.service.IdempotenciaConflitoException;
import br.com.fincontrol.domain.service.IdempotenciaRepositorio;

@Repository
public class IdempotenciaJdbc implements IdempotenciaRepositorio {

    private final NamedParameterJdbcTemplate jdbc;

    public IdempotenciaJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<RegistroIdempotencia> buscar(long clienteId, String chave) {
        List<RegistroIdempotencia> lista = jdbc.query(
                "SELECT hash_corpo, recurso_id FROM idempotency_key WHERE cliente_id = :clienteId AND chave = :chave",
                Map.of("clienteId", clienteId, "chave", chave),
                (rs, n) -> new RegistroIdempotencia(rs.getString("hash_corpo"), rs.getLong("recurso_id")));
        return lista.stream().findFirst();
    }

    @Override
    public void guardar(long clienteId, String chave, String hashCorpo, long recursoId) {
        try {
            jdbc.update(
                    """
                    INSERT INTO idempotency_key (cliente_id, chave, hash_corpo, recurso_id)
                    VALUES (:clienteId, :chave, :hashCorpo, :recursoId)
                    """,
                    new MapSqlParameterSource()
                            .addValue("clienteId", clienteId)
                            .addValue("chave", chave)
                            .addValue("hashCorpo", hashCorpo)
                            .addValue("recursoId", recursoId));
        } catch (DuplicateKeyException ex) {
            throw new IdempotenciaConflitoException();
        }
    }
}
