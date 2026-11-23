package br.com.fincontrol.infrastructure.in.web;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import br.com.fincontrol.domain.model.DuracaoSessao;

@Component
public class CookieRefresh {

    public static final String NOME = "refresh_token";
    public static final String PATH = "/api/v1/auth";

    private final boolean secure;

    public CookieRefresh(@Value("${fincontrol.cookie.secure:true}") boolean secure) {
        this.secure = secure;
    }

    public ResponseCookie criar(String valor) {
        return base(valor).maxAge(DuracaoSessao.REFRESH).build();
    }

    public ResponseCookie apagar() {
        return base("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String valor) {
        return ResponseCookie.from(NOME, valor)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path(PATH);
    }
}
