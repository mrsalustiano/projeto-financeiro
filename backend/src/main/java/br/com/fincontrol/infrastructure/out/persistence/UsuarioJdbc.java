package br.com.fincontrol.infrastructure.out.persistence;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.fincontrol.domain.model.NovoUsuario;
import br.com.fincontrol.domain.model.Usuario;
import br.com.fincontrol.domain.service.EmailEmUsoException;
import br.com.fincontrol.domain.service.UsuarioRepositorio;

@Repository
public class UsuarioJdbc implements UsuarioRepositorio {

    private static final String COLUNAS = """
            SELECT id, cliente_id, nome, email, senha_hash, role, ativo, falhas_login, bloqueado_ate
            FROM usuario
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final MapeadorUsuario mapeador = new MapeadorUsuario();
    private final RowMapper<Usuario> linha = (rs, numero) -> mapeador.mapear(rs);

    public UsuarioJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        return primeiro(COLUNAS + " WHERE email = :email", Map.of("email", email));
    }

    @Override
    public Optional<Usuario> buscarPorId(long id) {
        return primeiro(COLUNAS + " WHERE id = :id", Map.of("id", id));
    }

    @Override
    public boolean emailExiste(String email) {
        Integer total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM usuario WHERE email = :email",
                Map.of("email", email),
                Integer.class);
        return total != null && total > 0;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void salvarEstado(Usuario usuario) {
        jdbc.update(
                "UPDATE usuario SET falhas_login = :falhas, bloqueado_ate = :ate WHERE id = :id",
                new MapSqlParameterSource()
                        .addValue("falhas", usuario.falhasLogin())
                        .addValue("ate", InstantesJdbc.gravar(usuario.bloqueadoAte()))
                        .addValue("id", usuario.id()));
    }

    @Override
    public long inserir(NovoUsuario usuario) {
        try {
            return inserirNovo(usuario);
        } catch (DuplicateKeyException ex) {
            throw new EmailEmUsoException();
        }
    }

    private Optional<Usuario> primeiro(String sql, Map<String, ?> params) {
        List<Usuario> lista = jdbc.query(sql, params, linha);
        return lista.stream().findFirst();
    }

    private long inserirNovo(NovoUsuario usuario) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("clienteId", usuario.clienteId())
                .addValue("nome", usuario.nome())
                .addValue("email", usuario.email())
                .addValue("senha", usuario.senhaHash())
                .addValue("role", usuario.papel().name())
                .addValue("ativo", usuario.ativo());
        KeyHolder chaves = new GeneratedKeyHolder();
        jdbc.update("""
                INSERT INTO usuario (cliente_id, nome, email, senha_hash, role, ativo)
                VALUES (:clienteId, :nome, :email, :senha, :role, :ativo)
                """, params, chaves, new String[] {"id"});
        Number id = chaves.getKey();
        if (id == null) {
            throw new IllegalStateException("id ausente");
        }
        return id.longValue();
    }
}
