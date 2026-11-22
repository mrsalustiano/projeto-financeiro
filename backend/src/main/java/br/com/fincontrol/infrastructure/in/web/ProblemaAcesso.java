package br.com.fincontrol.infrastructure.in.web;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ProblemaAcesso implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        Problemas.escrever(response, Problemas.de(
                HttpStatus.FORBIDDEN, "Acesso negado", "Acesso negado.", request, ErroCodigo.ACESSO_NEGADO));
    }
}
