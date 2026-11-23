package br.com.fincontrol.infrastructure.in.web;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import br.com.fincontrol.application.OrcamentoComAlerta;
import br.com.fincontrol.application.Orcamentos;
import br.com.fincontrol.application.ValoresTexto;
import br.com.fincontrol.domain.model.AlertaOrcamento;
import br.com.fincontrol.domain.model.Dinheiro;
import br.com.fincontrol.domain.model.Orcamento;
import br.com.fincontrol.infrastructure.in.web.api.OrcamentosApi;
import br.com.fincontrol.infrastructure.in.web.api.model.OrcamentoRequest;
import br.com.fincontrol.infrastructure.in.web.api.model.OrcamentoResponse;

@RestController
public class OrcamentoController implements OrcamentosApi {

    private final Orcamentos orcamentos;

    public OrcamentoController(Orcamentos orcamentos) {
        this.orcamentos = orcamentos;
    }

    @Override
    public ResponseEntity<List<OrcamentoResponse>> listarOrcamentos(String ano, String mes) {
        return ResponseEntity.ok(orcamentos.listar(SessaoAtual.exigir(), Periodos.ano(ano), Periodos.mes(mes)).stream()
                .map(OrcamentoController::resposta)
                .toList());
    }

    @Override
    public ResponseEntity<OrcamentoResponse> definirOrcamento(
            String ano, String mes, String categoriaId, OrcamentoRequest pedido) {
        OrcamentoComAlerta definido = orcamentos.definir(
                SessaoAtual.exigir(),
                Ids.ler(categoriaId),
                Periodos.ano(ano),
                Periodos.mes(mes),
                new Dinheiro(new BigDecimal(pedido.getLimite())));
        return ResponseEntity.ok(resposta(definido));
    }

    @Override
    public ResponseEntity<Void> excluirOrcamento(String ano, String mes, String categoriaId) {
        orcamentos.excluir(SessaoAtual.exigir(), Ids.ler(categoriaId), Periodos.ano(ano), Periodos.mes(mes));
        return ResponseEntity.noContent().build();
    }

    private static OrcamentoResponse resposta(OrcamentoComAlerta item) {
        Orcamento orcamento = item.orcamento();
        AlertaOrcamento alerta = item.alerta();
        return new OrcamentoResponse(
                Long.toString(orcamento.categoriaId()),
                orcamento.ano(),
                orcamento.mes(),
                ValoresTexto.texto(orcamento.limite()),
                alerta.gasto().toPlainString(),
                alerta.percentual().toPlainString(),
                alerta.alerta(),
                alerta.estourou());
    }
}
