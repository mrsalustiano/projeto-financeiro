package br.com.fincontrol.infrastructure.in.web;

import java.math.RoundingMode;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import br.com.fincontrol.application.Conciliacoes;
import br.com.fincontrol.domain.model.Conciliacao;
import br.com.fincontrol.domain.model.Lancamento;
import br.com.fincontrol.infrastructure.in.web.api.ConciliacaoApi;
import br.com.fincontrol.infrastructure.in.web.api.model.ConciliacaoRequest;
import br.com.fincontrol.infrastructure.in.web.api.model.ConciliacaoResponse;
import br.com.fincontrol.infrastructure.in.web.api.model.LancamentoConciliacao;

@RestController
public class ConciliacaoController implements ConciliacaoApi {

    private final Conciliacoes conciliacoes;

    public ConciliacaoController(Conciliacoes conciliacoes) {
        this.conciliacoes = conciliacoes;
    }

    @Override
    public ResponseEntity<List<LancamentoConciliacao>> sugerirConciliacao(String lancamentoId) {
        List<LancamentoConciliacao> corpo = conciliacoes
                .sugerir(SessaoAtual.exigir(), Ids.ler(lancamentoId)).stream()
                .map(ConciliacaoController::candidato)
                .toList();
        return ResponseEntity.ok(corpo);
    }

    @Override
    public ResponseEntity<ConciliacaoResponse> conciliar(ConciliacaoRequest request) {
        Conciliacao conciliacao = conciliacoes.conciliar(
                SessaoAtual.exigir(), Ids.ler(request.getLancamentoId()), Ids.ler(request.getExtratoId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta(conciliacao));
    }

    @Override
    public ResponseEntity<Void> desfazerConciliacao(String id) {
        conciliacoes.desfazer(SessaoAtual.exigir(), Ids.ler(id));
        return ResponseEntity.noContent().build();
    }

    private static ConciliacaoResponse resposta(Conciliacao conciliacao) {
        return new ConciliacaoResponse(
                Long.toString(conciliacao.id()),
                Long.toString(conciliacao.contaId()),
                Long.toString(conciliacao.lancamentoId()),
                Long.toString(conciliacao.extratoId()),
                conciliacao.conciliadoEm().atOffset(ZoneOffset.UTC),
                conciliacao.conciliadoPor());
    }

    private static LancamentoConciliacao candidato(Lancamento lancamento) {
        LancamentoConciliacao corpo = new LancamentoConciliacao(
                Long.toString(lancamento.id()),
                lancamento.data(),
                lancamento.descricao(),
                lancamento.valor().valor().setScale(2, RoundingMode.HALF_UP).toPlainString(),
                LancamentoConciliacao.NaturezaEnum.fromValue(lancamento.natureza().name()));
        corpo.setFitid(lancamento.fitid());
        return corpo;
    }
}
