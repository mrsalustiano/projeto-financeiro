package br.com.fincontrol.infrastructure.in.web;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import br.com.fincontrol.application.MetaComProgresso;
import br.com.fincontrol.application.Metas;
import br.com.fincontrol.application.ValoresTexto;
import br.com.fincontrol.domain.model.Aporte;
import br.com.fincontrol.domain.model.Dinheiro;
import br.com.fincontrol.domain.model.Meta;
import br.com.fincontrol.domain.model.ProgressoMeta;
import br.com.fincontrol.infrastructure.in.web.api.MetasApi;
import br.com.fincontrol.infrastructure.in.web.api.model.AporteRequest;
import br.com.fincontrol.infrastructure.in.web.api.model.AporteResponse;
import br.com.fincontrol.infrastructure.in.web.api.model.MetaRequest;
import br.com.fincontrol.infrastructure.in.web.api.model.MetaResponse;

@RestController
public class MetaController implements MetasApi {

    private final Metas metas;

    public MetaController(Metas metas) {
        this.metas = metas;
    }

    @Override
    public ResponseEntity<List<MetaResponse>> listarMetas() {
        return ResponseEntity.ok(metas.listar(SessaoAtual.exigir()).stream()
                .map(MetaController::resposta)
                .toList());
    }

    @Override
    public ResponseEntity<MetaResponse> criarMeta(MetaRequest pedido) {
        MetaComProgresso criada = metas.criar(
                SessaoAtual.exigir(),
                pedido.getNome(),
                new Dinheiro(new BigDecimal(pedido.getValorAlvo())),
                pedido.getDataAlvo());
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta(criada));
    }

    @Override
    public ResponseEntity<MetaResponse> obterMeta(String id) {
        return ResponseEntity.ok(resposta(metas.obter(SessaoAtual.exigir(), Ids.ler(id))));
    }

    @Override
    public ResponseEntity<List<AporteResponse>> listarAportes(String id) {
        return ResponseEntity.ok(metas.listarAportes(SessaoAtual.exigir(), Ids.ler(id)).stream()
                .map(MetaController::resposta)
                .toList());
    }

    @Override
    public ResponseEntity<AporteResponse> criarAporte(String id, AporteRequest pedido) {
        Aporte aporte = metas.aportar(
                SessaoAtual.exigir(),
                Ids.ler(id),
                new Dinheiro(new BigDecimal(pedido.getValor())),
                pedido.getData());
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta(aporte));
    }

    private static MetaResponse resposta(MetaComProgresso item) {
        Meta meta = item.meta();
        ProgressoMeta progresso = item.progresso();
        MetaResponse corpo = new MetaResponse(
                Long.toString(meta.id()),
                meta.nome(),
                ValoresTexto.texto(meta.valorAlvo()),
                progresso.aportado().toPlainString(),
                progresso.percentual().toPlainString(),
                progresso.alcancada());
        corpo.setDataAlvo(meta.dataAlvo());
        return corpo;
    }

    private static AporteResponse resposta(Aporte aporte) {
        return new AporteResponse(
                Long.toString(aporte.id()),
                Long.toString(aporte.metaId()),
                ValoresTexto.texto(aporte.valor()),
                aporte.data());
    }
}
