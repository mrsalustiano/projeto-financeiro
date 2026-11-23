package br.com.fincontrol.domain.model;

import java.time.Duration;

public final class DuracaoSessao {

    public static final int ACCESS_SEGUNDOS = 900;
    public static final Duration REFRESH = Duration.ofDays(7);
    public static final int LIMITE_FALHAS = 5;
    public static final Duration BLOQUEIO = Duration.ofMinutes(15);

    private DuracaoSessao() {
    }
}
