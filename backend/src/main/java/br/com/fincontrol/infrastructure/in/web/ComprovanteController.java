package br.com.fincontrol.infrastructure.in.web;

import java.io.IOException;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.com.fincontrol.application.ComprovanteBaixado;
import br.com.fincontrol.application.Comprovantes;
import br.com.fincontrol.domain.model.Comprovante;
import br.com.fincontrol.infrastructure.in.web.api.ComprovantesApi;
import br.com.fincontrol.infrastructure.in.web.api.model.ComprovanteResponse;

@RestController
public class ComprovanteController implements ComprovantesApi {

    private final Comprovantes comprovantes;

    public ComprovanteController(Comprovantes comprovantes) {
        this.comprovantes = comprovantes;
    }

    @Override
    public ResponseEntity<ComprovanteResponse> anexarComprovante(String lancamentoId, MultipartFile arquivo) {
        Comprovante comprovante = comprovantes.anexar(
                SessaoAtual.exigir(), Ids.ler(lancamentoId), nome(arquivo), bytes(arquivo));
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta(comprovante));
    }

    @Override
    public ResponseEntity<List<ComprovanteResponse>> listarComprovantes(String lancamentoId) {
        List<ComprovanteResponse> corpo = comprovantes
                .listar(SessaoAtual.exigir(), Ids.ler(lancamentoId)).stream()
                .map(ComprovanteController::resposta)
                .toList();
        return ResponseEntity.ok(corpo);
    }

    @Override
    public ResponseEntity<Resource> baixarComprovante(String id) {
        ComprovanteBaixado baixado = comprovantes.baixar(SessaoAtual.exigir(), Ids.ler(id));
        Comprovante comprovante = baixado.comprovante();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(comprovante.tipo().contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + comprovante.nomeArquivo() + "\"")
                .body(new ByteArrayResource(baixado.conteudo()));
    }

    @Override
    public ResponseEntity<Void> excluirComprovante(String id) {
        comprovantes.remover(SessaoAtual.exigir(), Ids.ler(id));
        return ResponseEntity.noContent().build();
    }

    private static ComprovanteResponse resposta(Comprovante comprovante) {
        return new ComprovanteResponse(
                Long.toString(comprovante.id()),
                Long.toString(comprovante.lancamentoId()),
                comprovante.nomeArquivo(),
                ComprovanteResponse.TipoEnum.fromValue(comprovante.tipo().name()),
                comprovante.tamanho(),
                comprovante.criadoEm().atOffset(ZoneOffset.UTC),
                comprovante.criadoPor());
    }

    private static String nome(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Arquivo obrigatório.");
        }
        String nome = arquivo.getOriginalFilename();
        return nome == null || nome.isBlank() ? "comprovante" : nome;
    }

    private static byte[] bytes(MultipartFile arquivo) {
        try {
            return arquivo.getBytes();
        } catch (IOException ex) {
            throw new IllegalArgumentException("Arquivo ilegível.", ex);
        }
    }
}
