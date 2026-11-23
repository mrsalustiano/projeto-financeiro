package br.com.fincontrol.application;

import java.util.List;

import org.springframework.stereotype.Service;

import br.com.fincontrol.domain.model.Acesso;
import br.com.fincontrol.domain.model.FiltroAuditoria;
import br.com.fincontrol.domain.model.PaginaAuditoria;
import br.com.fincontrol.domain.model.Papel;
import br.com.fincontrol.domain.model.RegistroAuditoria;
import br.com.fincontrol.domain.service.AcessoNegadoException;
import br.com.fincontrol.domain.service.AuditoriaRepositorio;

@Service
public class Auditorias {

    public static final int LIMITE_PADRAO = 20;
    public static final int LIMITE_MAXIMO = 100;

    private final AuditoriaRepositorio auditoria;

    public Auditorias(AuditoriaRepositorio auditoria) {
        this.auditoria = auditoria;
    }

    public PaginaAuditoria consultar(Acesso acesso, FiltroAuditoria filtro, String cursor, Integer limite) {
        exigirMaster(acesso);
        int tamanho = limite(limite);
        Long posicao = cursor(cursor);
        List<RegistroAuditoria> itens = auditoria.listar(
                filtro == null ? FiltroAuditoria.vazio() : filtro, posicao, tamanho);
        Long proximo = itens.size() < tamanho ? null : itens.getLast().id();
        return new PaginaAuditoria(itens, proximo);
    }

    private static void exigirMaster(Acesso acesso) {
        if (acesso == null || acesso.papel() != Papel.MASTER) {
            throw new AcessoNegadoException();
        }
    }

    private static int limite(Integer limite) {
        if (limite == null) {
            return LIMITE_PADRAO;
        }
        if (limite < 1 || limite > LIMITE_MAXIMO) {
            throw new IllegalArgumentException("Limite deve ficar entre 1 e 100.");
        }
        return limite;
    }

    private static Long cursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            long valor = Long.parseLong(cursor.trim());
            if (valor <= 0) {
                throw new IllegalArgumentException("Cursor inválido.");
            }
            return valor;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Cursor inválido.", ex);
        }
    }
}
