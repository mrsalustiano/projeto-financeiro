package br.com.fincontrol.infrastructure.in.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import br.com.fincontrol.application.AutenticarUsuario;
import br.com.fincontrol.application.ConsultarUsuarioAutenticado;
import br.com.fincontrol.application.EncerrarSessao;
import br.com.fincontrol.application.RenovarSessao;
import br.com.fincontrol.application.SessaoEmitida;
import br.com.fincontrol.application.UsuarioVisao;
import br.com.fincontrol.domain.model.DuracaoSessao;
import br.com.fincontrol.infrastructure.in.web.api.AuthApi;
import br.com.fincontrol.infrastructure.in.web.api.model.LoginRequest;
import br.com.fincontrol.infrastructure.in.web.api.model.TokenResponse;
import br.com.fincontrol.infrastructure.in.web.api.model.UsuarioResponse;

@RestController
public class AuthController implements AuthApi {

    private final AutenticarUsuario autenticar;
    private final RenovarSessao renovar;
    private final EncerrarSessao encerrar;
    private final ConsultarUsuarioAutenticado consultar;
    private final CookieRefresh cookie;

    public AuthController(
            AutenticarUsuario autenticar,
            RenovarSessao renovar,
            EncerrarSessao encerrar,
            ConsultarUsuarioAutenticado consultar,
            CookieRefresh cookie) {
        this.autenticar = autenticar;
        this.renovar = renovar;
        this.encerrar = encerrar;
        this.consultar = consultar;
        this.cookie = cookie;
    }

    @Override
    public ResponseEntity<TokenResponse> login(LoginRequest loginRequest) {
        SessaoEmitida sessao = autenticar.executar(loginRequest.getEmail(), loginRequest.getSenha());
        return comCookie(sessao, cookie.criar(sessao.refreshOpaco()).toString());
    }

    @Override
    public ResponseEntity<Void> logout() {
        encerrar.executar(SessaoAtual.exigir());
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie.apagar().toString()).build();
    }

    @Override
    public ResponseEntity<UsuarioResponse> me() {
        UsuarioVisao visao = consultar.executar(SessaoAtual.exigir());
        UsuarioResponse corpo = new UsuarioResponse(
                visao.id(), visao.nome(), visao.email(), UsuarioResponse.RoleEnum.fromValue(visao.papel().name()));
        corpo.setClienteId(visao.clienteId());
        return ResponseEntity.ok(corpo);
    }

    @Override
    public ResponseEntity<TokenResponse> refreshToken(String xRefreshToken, String refreshToken) {
        SessaoEmitida sessao = renovar.executar(opaco(xRefreshToken, refreshToken));
        return comCookie(sessao, cookie.criar(sessao.refreshOpaco()).toString());
    }

    private static String opaco(String header, String cookie) {
        if (header != null && !header.isBlank()) {
            return header.trim();
        }
        return cookie;
    }

    private static ResponseEntity<TokenResponse> comCookie(SessaoEmitida sessao, String setCookie) {
        TokenResponse corpo = new TokenResponse(
                sessao.accessToken(), "Bearer", DuracaoSessao.ACCESS_SEGUNDOS, sessao.refreshOpaco());
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, setCookie).body(corpo);
    }
}
