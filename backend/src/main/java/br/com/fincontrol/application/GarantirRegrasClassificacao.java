package br.com.fincontrol.application;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fincontrol.domain.model.RegraClassificacao;
import br.com.fincontrol.domain.service.RegraClassificacaoRepositorio;

@Service
public class GarantirRegrasClassificacao {

    private final RegraClassificacaoRepositorio regras;

    public GarantirRegrasClassificacao(RegraClassificacaoRepositorio regras) {
        this.regras = regras;
    }

    @Transactional
    public void executar(long clienteId, List<RegraClassificacaoSeed> previstas) {
        for (RegraClassificacaoSeed prevista : previstas) {
            if (regras.existe(clienteId, prevista.termo(), prevista.diaInicio(), prevista.diaFim())) {
                continue;
            }
            regras.inserir(new RegraClassificacao(
                    0L,
                    clienteId,
                    prevista.termo(),
                    prevista.diaInicio(),
                    prevista.diaFim(),
                    prevista.valorMinimo(),
                    prevista.valorMaximo(),
                    prevista.categoria()));
        }
    }
}
