package br.com.fincontrol.application;

import java.util.List;

import org.springframework.stereotype.Service;

import br.com.fincontrol.domain.model.Acesso;
import br.com.fincontrol.domain.model.Conta;
import br.com.fincontrol.domain.service.ContaRepositorio;
import br.com.fincontrol.domain.service.EscopoTenant;
import br.com.fincontrol.domain.service.NaoEncontradoException;

@Service
public class ContasConsulta {

    private final ContaRepositorio contas;

    public ContasConsulta(ContaRepositorio contas) {
        this.contas = contas;
    }

    public List<Conta> listar(Acesso acesso) {
        return contas.listar(EscopoTenant.leitura(acesso));
    }

    public Conta obter(Acesso acesso, long id) {
        long clienteId = EscopoTenant.leitura(acesso);
        return contas.buscar(clienteId, id).orElseThrow(NaoEncontradoException::new);
    }
}
