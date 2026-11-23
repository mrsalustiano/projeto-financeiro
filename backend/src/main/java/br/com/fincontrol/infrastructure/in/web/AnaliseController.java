package br.com.fincontrol.infrastructure.in.web;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import br.com.fincontrol.application.AnalisesFinanceiras;
import br.com.fincontrol.domain.model.AnaliseFinanceira;
import br.com.fincontrol.domain.model.PedidoAnalise;
import br.com.fincontrol.domain.model.TipoAnalise;
import br.com.fincontrol.infrastructure.in.web.api.AnaliseApi;
import br.com.fincontrol.infrastructure.in.web.api.model.AnaliseRequest;
import br.com.fincontrol.infrastructure.in.web.api.model.AnaliseResponse;

@RestController
public class AnaliseController implements AnaliseApi {

    private final AnalisesFinanceiras casos;

    public AnaliseController(AnalisesFinanceiras casos) {
        this.casos = casos;
    }

    @Override
    public ResponseEntity<AnaliseResponse> analisarFinancas(AnaliseRequest analiseRequest) {
        PedidoAnalise pedido = new PedidoAnalise(
                mes(analiseRequest.getMes()),
                tipo(analiseRequest.getTipo()),
                analiseRequest.getPergunta());
        AnaliseFinanceira analise = casos.analisar(SessaoAtual.exigir(), pedido);
        return ResponseEntity.ok(new AnaliseResponse(
                pedido.mes().toString(),
                AnaliseResponse.TipoEnum.fromValue(pedido.tipo().name()),
                AnaliseResponse.ProvedorEnum.fromValue(casos.provedor()),
                casos.modelo(),
                analise.resumo(),
                List.copyOf(analise.insights()),
                List.copyOf(analise.acoes())));
    }

    private static YearMonth mes(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Mês obrigatório.");
        }
        try {
            return YearMonth.parse(valor.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Mês inválido.", ex);
        }
    }

    private static TipoAnalise tipo(AnaliseRequest.TipoEnum valor) {
        if (valor == null) {
            throw new IllegalArgumentException("Tipo de análise obrigatório.");
        }
        return TipoAnalise.valueOf(valor.name());
    }
}
