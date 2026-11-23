package br.com.fincontrol.infrastructure.in.web;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ProblemaAutenticacao implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        Problemas.escrever(response, Problemas.de(
                HttpStatus.UNAUTHORIZED, "Não autenticado", "Não autenticado.", request, ErroCodigo.NAO_AUTENTICADO));
    }
}
