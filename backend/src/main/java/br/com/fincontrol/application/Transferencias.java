package br.com.fincontrol.application;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fincontrol.domain.model.Acesso;
import br.com.fincontrol.domain.model.Transferencia;
import br.com.fincontrol.domain.service.ContaRepositorio;
import br.com.fincontrol.domain.service.EscopoTenant;
import br.com.fincontrol.domain.service.FechamentoMesRepositorio;
import br.com.fincontrol.domain.service.MesFechadoException;
import br.com.fincontrol.domain.service.NaoEncontradoException;
import br.com.fincontrol.domain.service.TransferenciaInvalidaException;
import br.com.fincontrol.domain.service.TransferenciaRepositorio;

@Service
public class Transferencias {

    private final TransferenciaRepositorio transferencias;
    private final ContaRepositorio contas;
    private final FechamentoMesRepositorio fechamentos;

    public Transferencias(
            TransferenciaRepositorio transferencias,
            ContaRepositorio contas,
            FechamentoMesRepositorio fechamentos) {
        this.transferencias = transferencias;
        this.contas = contas;
        this.fechamentos = fechamentos;
    }

    @Transactional
    public Transferencia criar(Acesso acesso, ComandoTransferencia comando) {
        long clienteId = EscopoTenant.escrita(acesso);
        if (comando.contaOrigemId() == comando.contaDestinoId()) {
            throw new TransferenciaInvalidaException();
        }
        exigirConta(clienteId, comando.contaOrigemId());
        exigirConta(clienteId, comando.contaDestinoId());
        exigirMesAberto(clienteId, comando.data());
        Transferencia nova = new Transferencia(
                0L,
                clienteId,
                comando.contaOrigemId(),
                comando.contaDestinoId(),
                comando.valor(),
                comando.data(),
                comando.descricao());
        long id = transferencias.inserir(nova);
        return new Transferencia(id, clienteId, nova.contaOrigemId(), nova.contaDestinoId(), nova.valor(),
                nova.data(), nova.descricao());
    }

    public List<Transferencia> listar(Acesso acesso) {
        return transferencias.listar(EscopoTenant.leitura(acesso));
    }

    private void exigirConta(long clienteId, long contaId) {
        if (contas.buscar(clienteId, contaId).isEmpty()) {
            throw new NaoEncontradoException();
        }
    }

    private void exigirMesAberto(long clienteId, java.time.LocalDate data) {
        if (fechamentos.buscar(clienteId, data.getYear(), data.getMonthValue()).isPresent()) {
            throw new MesFechadoException();
        }
    }
}
