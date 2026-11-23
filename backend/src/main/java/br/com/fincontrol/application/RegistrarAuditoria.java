package br.com.fincontrol.application;

import org.springframework.stereotype.Service;

import br.com.fincontrol.domain.model.AcaoAuditoria;
import br.com.fincontrol.domain.model.Acesso;
import br.com.fincontrol.domain.model.RegistroAuditoria;
import br.com.fincontrol.domain.model.Usuario;
import br.com.fincontrol.domain.service.AuditoriaRepositorio;
import br.com.fincontrol.domain.service.Relogio;
import br.com.fincontrol.domain.service.UsuarioRepositorio;

@Service
public class RegistrarAuditoria {

    private final AuditoriaRepositorio auditoria;
    private final UsuarioRepositorio usuarios;
    private final Relogio relogio;

    public RegistrarAuditoria(AuditoriaRepositorio auditoria, UsuarioRepositorio usuarios, Relogio relogio) {
        this.auditoria = auditoria;
        this.usuarios = usuarios;
        this.relogio = relogio;
    }

    public long registrar(
            Acesso acesso,
            AcaoAuditoria acao,
            String recurso,
            Long recursoId,
            String dadosAntes,
            String dadosDepois) {
        Long usuarioId = usuarioId(acesso);
        RegistroAuditoria registro = new RegistroAuditoria(
                0L,
                acesso == null ? null : acesso.clienteId(),
                usuarioId,
                email(usuarioId),
                acesso == null || acesso.papel() == null ? null : acesso.papel().name(),
                acao,
                recurso,
                recursoId,
                dadosAntes,
                dadosDepois,
                relogio.agora());
        return auditoria.inserir(registro);
    }

    private String email(Long usuarioId) {
        if (usuarioId == null) {
            return null;
        }
        return usuarios.buscarPorId(usuarioId).map(Usuario::email).orElse(null);
    }

    private static Long usuarioId(Acesso acesso) {
        if (acesso == null || acesso.usuarioId() == null || acesso.usuarioId().isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(acesso.usuarioId().trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
