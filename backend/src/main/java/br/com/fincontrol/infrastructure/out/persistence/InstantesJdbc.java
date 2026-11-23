package br.com.fincontrol.infrastructure.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

final class InstantesJdbc {

    private InstantesJdbc() {
    }

    static Instant ler(ResultSet rs, String coluna) throws SQLException {
        Timestamp valor = rs.getTimestamp(coluna);
        if (valor == null) {
            return null;
        }
        return valor.toInstant();
    }

    static Timestamp gravar(Instant instante) {
        if (instante == null) {
            return null;
        }
        return Timestamp.from(instante);
    }
}
