package br.com.fincontrol.infrastructure.out.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import br.com.fincontrol.domain.service.Relogio;

public final class RelogioSistema implements Relogio {

    @Override
    public Instant agora() {
        return Instant.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
