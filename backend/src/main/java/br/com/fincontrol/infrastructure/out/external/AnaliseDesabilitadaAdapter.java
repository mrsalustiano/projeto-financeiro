package br.com.fincontrol.infrastructure.out.external;

import br.com.fincontrol.domain.model.AgregadoMes;
import br.com.fincontrol.domain.model.AnaliseFinanceira;
import br.com.fincontrol.domain.model.PedidoAnalise;
import br.com.fincontrol.domain.service.AnaliseFinanceiraPort;
import br.com.fincontrol.domain.service.IaDesabilitadaException;

public class AnaliseDesabilitadaAdapter implements AnaliseFinanceiraPort {

    @Override
    public AnaliseFinanceira analisar(PedidoAnalise pedido, AgregadoMes agregado) {
        throw new IaDesabilitadaException();
    }

    @Override
    public String provedor() {
        return "none";
    }

    @Override
    public String modelo() {
        return "none";
    }
}
