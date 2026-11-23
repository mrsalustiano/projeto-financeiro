package br.com.fincontrol.infrastructure.in.web;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.com.fincontrol.application.ImportacaoOfx;
import br.com.fincontrol.application.ResultadoImportacao;
import br.com.fincontrol.application.SugestaoOfx;
import br.com.fincontrol.domain.model.Conta;
import br.com.fincontrol.infrastructure.in.web.api.OfxApi;
import br.com.fincontrol.infrastructure.in.web.api.model.ContaResumo;
import br.com.fincontrol.infrastructure.in.web.api.model.ImportacaoOfxResponse;
import br.com.fincontrol.infrastructure.in.web.api.model.SugestaoOfxResponse;

@RestController
public class OfxController implements OfxApi {

    private final ImportacaoOfx importacao;

    public OfxController(ImportacaoOfx importacao) {
        this.importacao = importacao;
    }

    @Override
    public ResponseEntity<SugestaoOfxResponse> sugerirContasOfx(MultipartFile arquivo) {
        SugestaoOfx sugestao = importacao.sugerir(SessaoAtual.exigir(), bytes(arquivo));
        SugestaoOfxResponse corpo = new SugestaoOfxResponse(
                sugestao.banco(),
                sugestao.movimentos(),
                sugestao.contasSugeridas().stream().map(OfxController::resumo).toList());
        corpo.setIdentificador(sugestao.identificador());
        corpo.setMoeda(sugestao.moeda());
        corpo.setInicio(sugestao.inicio());
        corpo.setFim(sugestao.fim());
        return ResponseEntity.ok(corpo);
    }

    @Override
    public ResponseEntity<ImportacaoOfxResponse> importarOfx(String contaId, MultipartFile arquivo) {
        ResultadoImportacao resultado = importacao.importar(
                SessaoAtual.exigir(), Ids.ler(contaId), bytes(arquivo));
        ImportacaoOfxResponse corpo = new ImportacaoOfxResponse(
                resultado.importados(),
                resultado.duplicados(),
                resultado.semCategoria(),
                resultado.avisos(),
                Long.toString(resultado.contaId()));
        corpo.setSaldoInformado(texto(resultado.saldoInformado()));
        corpo.setMoeda(resultado.moeda());
        corpo.setBanco(resultado.banco());
        corpo.setIdentificadorConta(resultado.identificadorConta());
        corpo.setRendimento(texto(resultado.rendimento()));
        return ResponseEntity.status(HttpStatus.CREATED).body(corpo);
    }

    private static ContaResumo resumo(Conta conta) {
        return new ContaResumo(
                Long.toString(conta.id()),
                conta.nome(),
                conta.banco(),
                ContaResumo.TipoEnum.fromValue(conta.tipo().name()));
    }

    private static String texto(BigDecimal valor) {
        return valor == null ? null : valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static byte[] bytes(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Arquivo obrigatório.");
        }
        try {
            return arquivo.getBytes();
        } catch (IOException ex) {
            throw new IllegalArgumentException("Arquivo ilegível.", ex);
        }
    }
}
