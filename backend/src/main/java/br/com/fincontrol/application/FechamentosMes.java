package br.com.fincontrol.application;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fincontrol.domain.model.Acesso;
import br.com.fincontrol.domain.model.FechamentoMes;
import br.com.fincontrol.domain.service.EscopoTenant;
import br.com.fincontrol.domain.service.FechamentoMesRepositorio;
import br.com.fincontrol.domain.service.NaoEncontradoException;
import br.com.fincontrol.domain.service.Relogio;

@Service
public class FechamentosMes {

    private final FechamentoMesRepositorio fechamentos;
    private final Relogio relogio;

    public FechamentosMes(FechamentoMesRepositorio fechamentos, Relogio relogio) {
        this.fechamentos = fechamentos;
        this.relogio = relogio;
    }

    public List<FechamentoMes> listar(Acesso acesso) {
        return fechamentos.listar(EscopoTenant.leitura(acesso));
    }

    @Transactional
    public FechamentoMes fechar(Acesso acesso, int ano, int mes) {
        long clienteId = EscopoTenant.escrita(acesso);
        Optional<FechamentoMes> existente = fechamentos.buscar(clienteId, ano, mes);
        if (existente.isPresent()) {
            return existente.get();
        }
        FechamentoMes novo = new FechamentoMes(0L, clienteId, ano, mes, relogio.agora(), acesso.usuarioId());
        long id = fechamentos.inserir(novo);
        return new FechamentoMes(id, clienteId, ano, mes, novo.fechadoEm(), novo.fechadoPor());
    }

    @Transactional
    public void reabrir(Acesso acesso, int ano, int mes) {
        long clienteId = EscopoTenant.escrita(acesso);
        if (!fechamentos.excluir(clienteId, ano, mes)) {
            throw new NaoEncontradoException();
        }
    }
}
