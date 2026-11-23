package br.com.fincontrol.infrastructure.in.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import br.com.fincontrol.application.ComandoLancamento;
import br.com.fincontrol.application.LancamentosConsulta;
import br.com.fincontrol.application.LancamentosEscrita;
import br.com.fincontrol.application.ValoresTexto;
import br.com.fincontrol.domain.model.Dinheiro;
import br.com.fincontrol.domain.model.FiltroLancamento;
import br.com.fincontrol.domain.model.Lancamento;
import br.com.fincontrol.domain.model.Natureza;
import br.com.fincontrol.domain.model.SaldoConta;
import br.com.fincontrol.domain.model.TipoDespesa;
import br.com.fincontrol.infrastructure.in.web.api.LancamentosApi;
import br.com.fincontrol.infrastructure.in.web.api.model.EstornoRequest;
import br.com.fincontrol.infrastructure.in.web.api.model.LancamentoCriadoResponse;
import br.com.fincontrol.infrastructure.in.web.api.model.LancamentoRequest;
import br.com.fincontrol.infrastructure.in.web.api.model.LancamentoResponse;
import br.com.fincontrol.infrastructure.in.web.api.model.SaldoContaResponse;

@RestController
public class LancamentoController implements LancamentosApi {

    private final LancamentosConsulta consulta;
    private final LancamentosEscrita escrita;

    public LancamentoController(LancamentosConsulta consulta, LancamentosEscrita escrita) {
        this.consulta = consulta;
        this.escrita = escrita;
    }

    @Override
    public ResponseEntity<List<LancamentoResponse>> listarLancamentos(String contaId, LocalDate de, LocalDate ate) {
        FiltroLancamento filtro = new FiltroLancamento(contaId == null ? null : Ids.ler(contaId), de, ate);
        return ResponseEntity.ok(consulta.listar(SessaoAtual.exigir(), filtro).stream()
                .map(LancamentoController::resposta)
                .toList());
    }

    @Override
    public ResponseEntity<LancamentoCriadoResponse> criarLancamento(LancamentoRequest pedido, String idempotencyKey) {
        List<Lancamento> criados = escrita.criar(SessaoAtual.exigir(), idempotencyKey, comando(pedido));
        LancamentoCriadoResponse corpo = new LancamentoCriadoResponse(criados.stream()
                .map(LancamentoController::resposta)
                .toList());
        corpo.setGrupoParcela(criados.getFirst().grupoParcela());
        return ResponseEntity.status(HttpStatus.CREATED).body(corpo);
    }

    @Override
    public ResponseEntity<LancamentoResponse> obterLancamento(String id) {
        return ResponseEntity.ok(resposta(consulta.obter(SessaoAtual.exigir(), Ids.ler(id))));
    }

    @Override
    public ResponseEntity<LancamentoResponse> atualizarLancamento(String id, LancamentoRequest pedido) {
        Lancamento atualizado = escrita.editar(SessaoAtual.exigir(), Ids.ler(id), comando(pedido));
        return ResponseEntity.ok(resposta(atualizado));
    }

    @Override
    public ResponseEntity<LancamentoResponse> estornarLancamento(String id, EstornoRequest pedido) {
        Lancamento estornado = escrita.estornar(SessaoAtual.exigir(), Ids.ler(id), pedido.getMotivo());
        return ResponseEntity.ok(resposta(estornado));
    }

    @Override
    public ResponseEntity<Void> excluirLancamento(String id) {
        escrita.excluir(SessaoAtual.exigir(), Ids.ler(id));
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<SaldoContaResponse> obterSaldoConta(String id) {
        long contaId = Ids.ler(id);
        SaldoConta saldo = consulta.saldo(SessaoAtual.exigir(), contaId);
        return ResponseEntity.ok(new SaldoContaResponse(
                Long.toString(contaId),
                saldo.saldoInicial().toPlainString(),
                saldo.creditos().toPlainString(),
                saldo.debitos().toPlainString(),
                saldo.saldo().toPlainString()));
    }

    private static ComandoLancamento comando(LancamentoRequest pedido) {
        return new ComandoLancamento(
                Ids.ler(pedido.getContaId()),
                Ids.ler(pedido.getCategoriaId()),
                Natureza.valueOf(pedido.getNatureza().getValue()),
                pedido.getTipoDespesa() == null ? null : TipoDespesa.valueOf(pedido.getTipoDespesa().getValue()),
                new Dinheiro(new BigDecimal(pedido.getValor())),
                pedido.getData(),
                pedido.getDescricao(),
                pedido.getParcelas() == null ? 1 : pedido.getParcelas());
    }

    private static LancamentoResponse resposta(Lancamento lancamento) {
        LancamentoResponse corpo = new LancamentoResponse(
                Long.toString(lancamento.id()),
                Long.toString(lancamento.contaId()),
                lancamento.categoriaId() == null ? null : Long.toString(lancamento.categoriaId()),
                LancamentoResponse.NaturezaEnum.fromValue(lancamento.natureza().name()),
                ValoresTexto.texto(lancamento.valor()),
                lancamento.data(),
                lancamento.descricao(),
                lancamento.estornado());
        if (lancamento.tipoDespesa() != null) {
            corpo.setTipoDespesa(LancamentoResponse.TipoDespesaEnum.fromValue(lancamento.tipoDespesa().name()));
        }
        corpo.setGrupoParcela(lancamento.grupoParcela());
        corpo.setNumeroParcela(lancamento.numeroParcela());
        corpo.setTotalParcelas(lancamento.totalParcelas());
        corpo.setEstornoMotivo(lancamento.estornoMotivo());
        if (lancamento.estornadoEm() != null) {
            corpo.setEstornadoEm(OffsetDateTime.ofInstant(lancamento.estornadoEm(), ZoneOffset.UTC));
        }
        return corpo;
    }
}
