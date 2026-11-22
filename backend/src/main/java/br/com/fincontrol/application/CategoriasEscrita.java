package br.com.fincontrol.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fincontrol.domain.model.Acesso;
import br.com.fincontrol.domain.model.Categoria;
import br.com.fincontrol.domain.model.Natureza;
import br.com.fincontrol.domain.service.CategoriaRepositorio;
import br.com.fincontrol.domain.service.EscopoTenant;
import br.com.fincontrol.domain.service.NaoEncontradoException;
import br.com.fincontrol.domain.service.NomeEmUsoException;

@Service
public class CategoriasEscrita {

    private final CategoriaRepositorio categorias;

    public CategoriasEscrita(CategoriaRepositorio categorias) {
        this.categorias = categorias;
    }

    @Transactional
    public Categoria criar(Acesso acesso, String nome, Natureza natureza) {
        long clienteId = EscopoTenant.escrita(acesso);
        String limpo = nome.trim();
        exigirNomeLivre(clienteId, limpo);
        long id = categorias.inserir(clienteId, limpo, natureza);
        return new Categoria(id, clienteId, limpo, natureza);
    }

    @Transactional
    public Categoria atualizar(Acesso acesso, long id, String nome, Natureza natureza) {
        long clienteId = EscopoTenant.escrita(acesso);
        Categoria atual = categorias.buscar(clienteId, id).orElseThrow(NaoEncontradoException::new);
        String limpo = nome.trim();
        if (!atual.nome().equals(limpo)) {
            exigirNomeLivre(clienteId, limpo);
        }
        categorias.atualizar(clienteId, id, limpo, natureza);
        return new Categoria(id, clienteId, limpo, natureza);
    }

    @Transactional
    public void excluir(Acesso acesso, long id) {
        long clienteId = EscopoTenant.escrita(acesso);
        if (!categorias.excluir(clienteId, id)) {
            throw new NaoEncontradoException();
        }
    }

    private void exigirNomeLivre(long clienteId, String nome) {
        if (categorias.existeNome(clienteId, nome)) {
            throw new NomeEmUsoException();
        }
    }
}
