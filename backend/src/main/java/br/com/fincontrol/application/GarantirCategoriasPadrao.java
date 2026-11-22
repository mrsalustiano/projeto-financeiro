package br.com.fincontrol.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fincontrol.domain.model.CategoriaPadrao;
import br.com.fincontrol.domain.service.CategoriaRepositorio;

@Service
public class GarantirCategoriasPadrao {

    private final CategoriaRepositorio categorias;

    public GarantirCategoriasPadrao(CategoriaRepositorio categorias) {
        this.categorias = categorias;
    }

    @Transactional
    public void executar(long clienteId) {
        for (CategoriaPadrao padrao : CategoriaPadrao.values()) {
            if (!categorias.existeNome(clienteId, padrao.nome())) {
                categorias.inserir(clienteId, padrao.nome(), padrao.natureza());
            }
        }
    }
}
