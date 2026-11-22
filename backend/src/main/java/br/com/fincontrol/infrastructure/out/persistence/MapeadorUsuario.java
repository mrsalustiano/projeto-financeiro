package br.com.fincontrol.infrastructure.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;

import br.com.fincontrol.domain.model.Papel;
import br.com.fincontrol.domain.model.Usuario;

final class MapeadorUsuario {

    Usuario mapear(ResultSet rs) throws SQLException {
        return new Usuario(
                rs.getLong("id"),
                cliente(rs),
                rs.getString("nome"),
                rs.getString("email"),
                rs.getString("senha_hash"),
                Papel.valueOf(rs.getString("role")),
                rs.getBoolean("ativo"),
                rs.getInt("falhas_login"),
                InstantesJdbc.ler(rs, "bloqueado_ate"));
    }

    private static Long cliente(ResultSet rs) throws SQLException {
        long valor = rs.getLong("cliente_id");
        if (rs.wasNull()) {
            return null;
        }
        return valor;
    }
}
