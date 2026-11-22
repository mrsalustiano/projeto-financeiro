package br.com.fincontrol.application;

import java.util.List;

import org.springframework.stereotype.Service;

import br.com.fincontrol.domain.model.Acesso;
import br.com.fincontrol.domain.model.Categoria;
import br.com.fincontrol.domain.service.CategoriaRepositorio;
import br.com.fincontrol.domain.service.EscopoTenant;

@Service
public class CategoriasConsulta {

    private final CategoriaRepositorio categorias;

    public CategoriasConsulta(CategoriaRepositorio categorias) {
        this.categorias = categorias;
    }

    public List<Categoria> listar(Acesso acesso) {
        return categorias.listar(EscopoTenant.leitura(acesso));
    }
}
