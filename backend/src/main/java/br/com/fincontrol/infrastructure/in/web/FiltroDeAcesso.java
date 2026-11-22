package br.com.fincontrol.infrastructure.in.web;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import br.com.fincontrol.domain.model.Acesso;
import br.com.fincontrol.domain.service.RevogacaoRepositorio;
import br.com.fincontrol.domain.service.TokenAcessoPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class FiltroDeAcesso extends OncePerRequestFilter {

    private final TokenAcessoPort tokens;
    private final RevogacaoRepositorio revogacao;

    public FiltroDeAcesso(TokenAcessoPort tokens, RevogacaoRepositorio revogacao) {
        this.tokens = tokens;
        this.revogacao = revogacao;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String caminho = request.getRequestURI();
        return "/api/v1/auth/login".equals(caminho) || "/api/v1/auth/refresh_token".equals(caminho);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || header.isBlank()) {
            chain.doFilter(request, response);
            return;
        }
        if (!autenticar(header, request, response)) {
            return;
        }
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private boolean autenticar(String header, HttpServletRequest request, HttpServletResponse response) throws IOException {
        Optional<Acesso> acesso = bearer(header).flatMap(tokens::ler);
        if (acesso.isEmpty() || revogacao.revogado(acesso.get().jti())) {
            recusar(request, response);
            return false;
        }
        Acesso atual = acesso.get();
        var autoridade = new SimpleGrantedAuthority("ROLE_" + atual.papel().name());
        var autenticacao = new UsernamePasswordAuthenticationToken(atual, null, List.of(autoridade));
        SecurityContextHolder.getContext().setAuthentication(autenticacao);
        return true;
    }

    private static Optional<String> bearer(String header) {
        if (!header.startsWith("Bearer ")) {
            return Optional.empty();
        }
        String token = header.substring("Bearer ".length()).trim();
        if (token.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(token);
    }

    private void recusar(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Problemas.escrever(response, Problemas.de(
                HttpStatus.UNAUTHORIZED, "Não autenticado", "Não autenticado.", request, ErroCodigo.NAO_AUTENTICADO));
    }
}
