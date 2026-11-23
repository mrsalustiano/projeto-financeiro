package br.com.fincontrol.infrastructure.in.web;

import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import br.com.fincontrol.application.Recorrencias;
import br.com.fincontrol.domain.service.Relogio;

@Component
public class JobRecorrencia {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final Recorrencias recorrencias;
    private final Relogio relogio;

    public JobRecorrencia(Recorrencias recorrencias, Relogio relogio) {
        this.recorrencias = recorrencias;
        this.relogio = relogio;
    }

    @Scheduled(cron = "${fincontrol.recorrencia.cron:0 10 0 * * *}", zone = "America/Sao_Paulo")
    public void executar() {
        executar(hoje());
    }

    public int executar(LocalDate hoje) {
        return recorrencias.executarDoDia(hoje);
    }

    private LocalDate hoje() {
        return LocalDate.ofInstant(relogio.agora(), FUSO);
    }
}
