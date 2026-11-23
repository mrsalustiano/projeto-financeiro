package br.com.fincontrol.infrastructure.in.web;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import br.com.fincontrol.application.FechamentosMes;
import br.com.fincontrol.domain.model.FechamentoMes;
import br.com.fincontrol.infrastructure.in.web.api.MesesApi;
import br.com.fincontrol.infrastructure.in.web.api.model.FechamentoMesResponse;

@RestController
public class MesController implements MesesApi {

    private final FechamentosMes fechamentos;

    public MesController(FechamentosMes fechamentos) {
        this.fechamentos = fechamentos;
    }

    @Override
    public ResponseEntity<List<FechamentoMesResponse>> listarMeses() {
        return ResponseEntity.ok(fechamentos.listar(SessaoAtual.exigir()).stream()
                .map(MesController::resposta)
                .toList());
    }

    @Override
    public ResponseEntity<FechamentoMesResponse> fecharMes(String ano, String mes) {
        FechamentoMes fechamento = fechamentos.fechar(SessaoAtual.exigir(), Periodos.ano(ano), Periodos.mes(mes));
        return ResponseEntity.ok(resposta(fechamento));
    }

    @Override
    public ResponseEntity<Void> reabrirMes(String ano, String mes) {
        fechamentos.reabrir(SessaoAtual.exigir(), Periodos.ano(ano), Periodos.mes(mes));
        return ResponseEntity.noContent().build();
    }

    private static FechamentoMesResponse resposta(FechamentoMes fechamento) {
        return new FechamentoMesResponse(
                fechamento.ano(),
                fechamento.mes(),
                OffsetDateTime.ofInstant(fechamento.fechadoEm(), ZoneOffset.UTC),
                fechamento.fechadoPor());
    }
}
