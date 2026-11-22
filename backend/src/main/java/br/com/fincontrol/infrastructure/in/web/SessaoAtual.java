package br.com.fincontrol.infrastructure.in.web;

import org.springframework.security.core.context.SecurityContextHolder;

import br.com.fincontrol.domain.model.Acesso;
import br.com.fincontrol.domain.service.NaoAutenticadoException;

public final class SessaoAtual {

    private SessaoAtual() {
    }

    public static Acesso exigir() {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            throw new NaoAutenticadoException();
        }
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Acesso acesso) {
            return acesso;
        }
        throw new NaoAutenticadoException();
    }
}
