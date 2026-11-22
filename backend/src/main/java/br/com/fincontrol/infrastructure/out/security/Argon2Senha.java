package br.com.fincontrol.infrastructure.out.security;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.com.fincontrol.domain.service.SenhaPort;

public final class Argon2Senha implements SenhaPort {

    private final PasswordEncoder encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    private final String ficticio = encoder.encode("fincontrol-usuario-ausente");

    @Override
    public String hash(String senha) {
        return encoder.encode(senha);
    }

    @Override
    public boolean confere(String senha, String hash) {
        return encoder.matches(senha, hash);
    }

    @Override
    public String hashFicticio() {
        return ficticio;
    }
}
