package br.com.fincontrol.infrastructure.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import br.com.fincontrol.domain.model.AcaoAuditoria;
import br.com.fincontrol.domain.model.FiltroAuditoria;
import br.com.fincontrol.domain.model.RegistroAuditoria;
import br.com.fincontrol.domain.service.AuditoriaRepositorio;

@Repository
public class AuditoriaJdbc implements AuditoriaRepositorio {

    private static final String COLUNAS = """
            SELECT id, cliente_id, usuario_id, usuario_email, usuario_perfil, acao, recurso, recurso_id,
                   dados_antes, dados_depois, criado_em
            FROM auditoria
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public AuditoriaJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public long inserir(RegistroAuditoria registro) {
        KeyHolder chaves = new GeneratedKeyHolder();
        jdbc.update(
                """
                INSERT INTO auditoria
                    (cliente_id, usuario_id, usuario_email, usuario_perfil, acao, recurso, recurso_id,
                     dados_antes, dados_depois, criado_em)
                VALUES (:clienteId, :usuarioId, :usuarioEmail, :usuarioPerfil, :acao, :recurso, :recursoId,
                        :dadosAntes, :dadosDepois, :criadoEm)
                """,
                new MapSqlParameterSource()
                        .addValue("clienteId", registro.clienteId())
                        .addValue("usuarioId", registro.usuarioId())
                        .addValue("usuarioEmail", registro.usuarioEmail())
                        .addValue("usuarioPerfil", registro.usuarioPerfil())
                        .addValue("acao", registro.acao().name())
                        .addValue("recurso", registro.recurso())
                        .addValue("recursoId", registro.recursoId())
                        .addValue("dadosAntes", registro.dadosAntes())
                        .addValue("dadosDepois", registro.dadosDepois())
                        .addValue("criadoEm", Timestamp.from(registro.criadoEm())),
                chaves,
                new String[] {"id"});
        Number id = chaves.getKey();
        if (id == null) {
            throw new IllegalStateException("id ausente");
        }
        return id.longValue();
    }

    @Override
    public List<RegistroAuditoria> listar(FiltroAuditoria filtro, Long cursor, int limite) {
        MapSqlParameterSource parametros = new MapSqlParameterSource().addValue("limite", limite);
        String sql = COLUNAS + condicoes(filtro, cursor, parametros) + " ORDER BY id DESC LIMIT :limite";
        return jdbc.query(sql, parametros, (rs, n) -> mapear(rs));
    }

    private static String condicoes(FiltroAuditoria filtro, Long cursor, MapSqlParameterSource parametros) {
        StringBuilder sql = new StringBuilder(" WHERE 1 = 1");
        if (filtro.clienteId() != null) {
            sql.append(" AND cliente_id = :clienteId");
            parametros.addValue("clienteId", filtro.clienteId());
        }
        if (filtro.recurso() != null) {
            sql.append(" AND recurso = :recurso");
            parametros.addValue("recurso", filtro.recurso());
        }
        if (filtro.acao() != null) {
            sql.append(" AND acao = :acao");
            parametros.addValue("acao", filtro.acao().name());
        }
        if (filtro.de() != null) {
            sql.append(" AND criado_em >= :de");
            parametros.addValue("de", filtro.de().atStartOfDay());
        }
        if (filtro.ate() != null) {
            sql.append(" AND criado_em < :ate");
            parametros.addValue("ate", filtro.ate().plusDays(1).atStartOfDay());
        }
        if (cursor != null) {
            sql.append(" AND id < :cursor");
            parametros.addValue("cursor", cursor);
        }
        return sql.toString();
    }

    private static RegistroAuditoria mapear(ResultSet rs) throws SQLException {
        return new RegistroAuditoria(
                rs.getLong("id"),
                numero(rs, "cliente_id"),
                numero(rs, "usuario_id"),
                rs.getString("usuario_email"),
                rs.getString("usuario_perfil"),
                AcaoAuditoria.valueOf(rs.getString("acao")),
                rs.getString("recurso"),
                numero(rs, "recurso_id"),
                rs.getString("dados_antes"),
                rs.getString("dados_depois"),
                rs.getTimestamp("criado_em").toInstant());
    }

    private static Long numero(ResultSet rs, String coluna) throws SQLException {
        long valor = rs.getLong(coluna);
        return rs.wasNull() ? null : valor;
    }
}
