package br.com.fincontrol.infrastructure.in.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import br.com.fincontrol.application.ContasConsulta;
import br.com.fincontrol.application.ContasEscrita;
import br.com.fincontrol.application.ValoresTexto;
import br.com.fincontrol.domain.model.Conta;
import br.com.fincontrol.domain.model.TipoConta;
import br.com.fincontrol.infrastructure.in.web.api.ContasApi;
import br.com.fincontrol.infrastructure.in.web.api.model.ContaRequest;
import br.com.fincontrol.infrastructure.in.web.api.model.ContaResponse;

@RestController
public class ContaController implements ContasApi {

    private final ContasConsulta consulta;
    private final ContasEscrita escrita;

    public ContaController(ContasConsulta consulta, ContasEscrita escrita) {
        this.consulta = consulta;
        this.escrita = escrita;
    }

    @Override
    public ResponseEntity<List<ContaResponse>> listarContas() {
        return ResponseEntity.ok(consulta.listar(SessaoAtual.exigir()).stream().map(ContaController::resposta).toList());
    }

    @Override
    public ResponseEntity<ContaResponse> obterConta(String id) {
        return ResponseEntity.ok(resposta(consulta.obter(SessaoAtual.exigir(), Ids.ler(id))));
    }

    @Override
    public ResponseEntity<ContaResponse> criarConta(ContaRequest contaRequest) {
        Conta criada = escrita.criar(
                SessaoAtual.exigir(),
                contaRequest.getNome(),
                contaRequest.getBanco(),
                tipo(contaRequest),
                ValoresTexto.saldo(contaRequest.getSaldoInicial()),
                contaRequest.getDataSaldo(),
                ValoresTexto.limite(contaRequest.getLimite()));
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta(criada));
    }

    @Override
    public ResponseEntity<ContaResponse> atualizarConta(String id, ContaRequest contaRequest) {
        Conta atualizada = escrita.atualizar(
                SessaoAtual.exigir(),
                Ids.ler(id),
                contaRequest.getNome(),
                contaRequest.getBanco(),
                tipo(contaRequest),
                ValoresTexto.saldo(contaRequest.getSaldoInicial()),
                contaRequest.getDataSaldo(),
                ValoresTexto.limite(contaRequest.getLimite()));
        return ResponseEntity.ok(resposta(atualizada));
    }

    @Override
    public ResponseEntity<Void> excluirConta(String id) {
        escrita.excluir(SessaoAtual.exigir(), Ids.ler(id));
        return ResponseEntity.noContent().build();
    }

    private static TipoConta tipo(ContaRequest pedido) {
        return TipoConta.valueOf(pedido.getTipo().getValue());
    }

    private static ContaResponse resposta(Conta conta) {
        ContaResponse corpo = new ContaResponse(
                Long.toString(conta.id()),
                conta.nome(),
                conta.banco(),
                ContaResponse.TipoEnum.fromValue(conta.tipo().name()),
                ValoresTexto.texto(conta.saldoInicial()),
                conta.dataSaldo());
        corpo.setLimite(ValoresTexto.texto(conta.limite()));
        return corpo;
    }
}
