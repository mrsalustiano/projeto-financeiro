package br.com.fincontrol.domain.service;

import br.com.fincontrol.domain.model.Acesso;
import br.com.fincontrol.domain.model.Papel;

public final class EscopoTenant {

    private EscopoTenant() {
    }

    public static long leitura(Acesso acesso) {
        if (acesso == null || acesso.papel() == Papel.MASTER || acesso.clienteId() == null) {
            throw new AcessoNegadoException();
        }
        return acesso.clienteId();
    }

    public static long escrita(Acesso acesso) {
        long clienteId = leitura(acesso);
        if (acesso.papel() == Papel.LEITOR) {
            throw new AcessoNegadoException();
        }
        return clienteId;
    }
}
