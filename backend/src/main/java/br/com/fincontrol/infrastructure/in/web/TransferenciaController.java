package br.com.fincontrol.infrastructure.in.web;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import br.com.fincontrol.application.ComandoTransferencia;
import br.com.fincontrol.application.Transferencias;
import br.com.fincontrol.application.ValoresTexto;
import br.com.fincontrol.domain.model.Dinheiro;
import br.com.fincontrol.domain.model.Transferencia;
import br.com.fincontrol.infrastructure.in.web.api.TransferenciasApi;
import br.com.fincontrol.infrastructure.in.web.api.model.TransferenciaRequest;
import br.com.fincontrol.infrastructure.in.web.api.model.TransferenciaResponse;

@RestController
public class TransferenciaController implements TransferenciasApi {

    private final Transferencias transferencias;

    public TransferenciaController(Transferencias transferencias) {
        this.transferencias = transferencias;
    }

    @Override
    public ResponseEntity<List<TransferenciaResponse>> listarTransferencias() {
        return ResponseEntity.ok(transferencias.listar(SessaoAtual.exigir()).stream()
                .map(TransferenciaController::resposta)
                .toList());
    }

    @Override
    public ResponseEntity<TransferenciaResponse> criarTransferencia(TransferenciaRequest pedido) {
        ComandoTransferencia comando = new ComandoTransferencia(
                Ids.ler(pedido.getContaOrigemId()),
                Ids.ler(pedido.getContaDestinoId()),
                new Dinheiro(new BigDecimal(pedido.getValor())),
                pedido.getData(),
                pedido.getDescricao());
        Transferencia criada = transferencias.criar(SessaoAtual.exigir(), comando);
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta(criada));
    }

    private static TransferenciaResponse resposta(Transferencia transferencia) {
        TransferenciaResponse corpo = new TransferenciaResponse(
                Long.toString(transferencia.id()),
                Long.toString(transferencia.contaOrigemId()),
                Long.toString(transferencia.contaDestinoId()),
                ValoresTexto.texto(transferencia.valor()),
                transferencia.data());
        corpo.setDescricao(transferencia.descricao());
        return corpo;
    }
}
