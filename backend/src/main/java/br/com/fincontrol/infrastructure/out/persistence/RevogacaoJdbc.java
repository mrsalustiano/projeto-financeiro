package br.com.fincontrol.infrastructure.out.persistence;

import java.time.Instant;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import br.com.fincontrol.domain.service.RevogacaoRepositorio;

@Repository
public class RevogacaoJdbc implements RevogacaoRepositorio {

    private final NamedParameterJdbcTemplate jdbc;

    public RevogacaoJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void revogar(String jti, Instant expiraEm) {
        jdbc.update(
                """
                INSERT INTO access_token_revogado (jti, expira_em) VALUES (:jti, :expira) AS novo
                ON DUPLICATE KEY UPDATE expira_em = novo.expira_em
                """,
                new MapSqlParameterSource()
                        .addValue("jti", jti)
                        .addValue("expira", InstantesJdbc.gravar(expiraEm)));
    }

    @Override
    public boolean revogado(String jti) {
        Integer total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM access_token_revogado WHERE jti = :jti",
                Map.of("jti", jti),
                Integer.class);
        return total != null && total > 0;
    }
}
