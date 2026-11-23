package br.com.fincontrol.application;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fincontrol.domain.model.Conta;
import br.com.fincontrol.domain.service.ContaRepositorio;

@Service
public class GarantirContas {

    private final ContaRepositorio contas;

    public GarantirContas(ContaRepositorio contas) {
        this.contas = contas;
    }

    @Transactional
    public void executar(long clienteId, List<ContaSeed> previstas) {
        for (ContaSeed prevista : previstas) {
            if (!contas.existeNome(clienteId, prevista.nome())) {
                contas.inserir(new Conta(
                        0L,
                        clienteId,
                        prevista.nome(),
                        prevista.banco(),
                        prevista.tipo(),
                        prevista.saldoInicial(),
                        prevista.dataSaldo(),
                        null));
            }
        }
    }
}
