package br.com.fincontrol.infrastructure.in.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import br.com.fincontrol.application.CategoriasConsulta;
import br.com.fincontrol.application.CategoriasEscrita;
import br.com.fincontrol.domain.model.Categoria;
import br.com.fincontrol.domain.model.Natureza;
import br.com.fincontrol.infrastructure.in.web.api.CategoriasApi;
import br.com.fincontrol.infrastructure.in.web.api.model.CategoriaRequest;
import br.com.fincontrol.infrastructure.in.web.api.model.CategoriaResponse;

@RestController
public class CategoriaController implements CategoriasApi {

    private final CategoriasConsulta consulta;
    private final CategoriasEscrita escrita;

    public CategoriaController(CategoriasConsulta consulta, CategoriasEscrita escrita) {
        this.consulta = consulta;
        this.escrita = escrita;
    }

    @Override
    public ResponseEntity<List<CategoriaResponse>> listarCategorias() {
        return ResponseEntity.ok(consulta.listar(SessaoAtual.exigir()).stream().map(CategoriaController::resposta).toList());
    }

    @Override
    public ResponseEntity<CategoriaResponse> criarCategoria(CategoriaRequest categoriaRequest) {
        Categoria criada = escrita.criar(SessaoAtual.exigir(), categoriaRequest.getNome(), natureza(categoriaRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta(criada));
    }

    @Override
    public ResponseEntity<CategoriaResponse> atualizarCategoria(String id, CategoriaRequest categoriaRequest) {
        Categoria atualizada = escrita.atualizar(
                SessaoAtual.exigir(), Ids.ler(id), categoriaRequest.getNome(), natureza(categoriaRequest));
        return ResponseEntity.ok(resposta(atualizada));
    }

    @Override
    public ResponseEntity<Void> excluirCategoria(String id) {
        escrita.excluir(SessaoAtual.exigir(), Ids.ler(id));
        return ResponseEntity.noContent().build();
    }

    private static Natureza natureza(CategoriaRequest pedido) {
        return Natureza.valueOf(pedido.getNatureza().getValue());
    }

    private static CategoriaResponse resposta(Categoria categoria) {
        return new CategoriaResponse(
                Long.toString(categoria.id()),
                categoria.nome(),
                CategoriaResponse.NaturezaEnum.fromValue(categoria.natureza().name()));
    }
}
