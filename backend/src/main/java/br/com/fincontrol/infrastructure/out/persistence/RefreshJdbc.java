package br.com.fincontrol.infrastructure.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import br.com.fincontrol.domain.model.RefreshGuardado;
import br.com.fincontrol.domain.service.RefreshRepositorio;

@Repository
public class RefreshJdbc implements RefreshRepositorio {

    private final NamedParameterJdbcTemplate jdbc;

    public RefreshJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<RefreshGuardado> buscarPorHash(String hash) {
        List<RefreshGuardado> lista = jdbc.query(
                """
                SELECT id, usuario_id, expira_em, revogado, jti_access, access_expira_em
                FROM refresh_token WHERE hash = :hash
                """,
                Map.of("hash", hash),
                (rs, numero) -> new RefreshGuardado(
                        rs.getLong("id"),
                        rs.getLong("usuario_id"),
                        InstantesJdbc.ler(rs, "expira_em"),
                        rs.getBoolean("revogado"),
                        rs.getString("jti_access"),
                        InstantesJdbc.ler(rs, "access_expira_em")));
        return lista.stream().findFirst();
    }

    @Override
    public void inserir(long usuarioId, String hash, Instant expiraEm, String jtiAccess, Instant accessExpiraEm) {
        jdbc.update(
                """
                INSERT INTO refresh_token (usuario_id, hash, expira_em, jti_access, access_expira_em)
                VALUES (:usuarioId, :hash, :expiraEm, :jti, :accessExpira)
                """,
                new MapSqlParameterSource()
                        .addValue("usuarioId", usuarioId)
                        .addValue("hash", hash)
                        .addValue("expiraEm", InstantesJdbc.gravar(expiraEm))
                        .addValue("jti", jtiAccess)
                        .addValue("accessExpira", InstantesJdbc.gravar(accessExpiraEm)));
    }

    @Override
    public void revogar(long id) {
        jdbc.update("UPDATE refresh_token SET revogado = 1 WHERE id = :id", Map.of("id", id));
    }

    @Override
    public void revogarPorJtiAccess(String jtiAccess) {
        jdbc.update(
                "UPDATE refresh_token SET revogado = 1 WHERE jti_access = :jti AND revogado = 0",
                Map.of("jti", jtiAccess));
    }
}
