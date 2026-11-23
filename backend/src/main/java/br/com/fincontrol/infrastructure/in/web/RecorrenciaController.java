package br.com.fincontrol.infrastructure.in.web;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import br.com.fincontrol.application.ComandoRecorrencia;
import br.com.fincontrol.application.Recorrencias;
import br.com.fincontrol.application.ValoresTexto;
import br.com.fincontrol.domain.model.Dinheiro;
import br.com.fincontrol.domain.model.Natureza;
import br.com.fincontrol.domain.model.Recorrencia;
import br.com.fincontrol.domain.model.TipoDespesa;
import br.com.fincontrol.infrastructure.in.web.api.RecorrenciasApi;
import br.com.fincontrol.infrastructure.in.web.api.model.RecorrenciaRequest;
import br.com.fincontrol.infrastructure.in.web.api.model.RecorrenciaResponse;

@RestController
public class RecorrenciaController implements RecorrenciasApi {

    private final Recorrencias recorrencias;

    public RecorrenciaController(Recorrencias recorrencias) {
        this.recorrencias = recorrencias;
    }

    @Override
    public ResponseEntity<List<RecorrenciaResponse>> listarRecorrencias() {
        return ResponseEntity.ok(recorrencias.listar(SessaoAtual.exigir()).stream()
                .map(RecorrenciaController::resposta)
                .toList());
    }

    @Override
    public ResponseEntity<RecorrenciaResponse> criarRecorrencia(RecorrenciaRequest pedido) {
        ComandoRecorrencia comando = new ComandoRecorrencia(
                Ids.ler(pedido.getContaId()),
                Ids.ler(pedido.getCategoriaId()),
                Natureza.valueOf(pedido.getNatureza().getValue()),
                pedido.getTipoDespesa() == null ? null : TipoDespesa.valueOf(pedido.getTipoDespesa().getValue()),
                new Dinheiro(new BigDecimal(pedido.getValor())),
                pedido.getDescricao(),
                pedido.getDiaDoMes());
        Recorrencia criada = recorrencias.criar(SessaoAtual.exigir(), comando);
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta(criada));
    }

    @Override
    public ResponseEntity<RecorrenciaResponse> pausarRecorrencia(String id) {
        return ResponseEntity.ok(resposta(recorrencias.pausar(SessaoAtual.exigir(), Ids.ler(id))));
    }

    @Override
    public ResponseEntity<RecorrenciaResponse> retomarRecorrencia(String id) {
        return ResponseEntity.ok(resposta(recorrencias.retomar(SessaoAtual.exigir(), Ids.ler(id))));
    }

    private static RecorrenciaResponse resposta(Recorrencia recorrencia) {
        RecorrenciaResponse corpo = new RecorrenciaResponse(
                Long.toString(recorrencia.id()),
                Long.toString(recorrencia.contaId()),
                Long.toString(recorrencia.categoriaId()),
                RecorrenciaResponse.NaturezaEnum.fromValue(recorrencia.natureza().name()),
                ValoresTexto.texto(recorrencia.valor()),
                recorrencia.descricao(),
                recorrencia.dia().dia(),
                recorrencia.ativa());
        if (recorrencia.tipoDespesa() != null) {
            corpo.setTipoDespesa(RecorrenciaResponse.TipoDespesaEnum.fromValue(recorrencia.tipoDespesa().name()));
        }
        if (recorrencia.pausadaEm() != null) {
            corpo.setPausadaEm(OffsetDateTime.ofInstant(recorrencia.pausadaEm(), ZoneOffset.UTC));
        }
        return corpo;
    }
}
