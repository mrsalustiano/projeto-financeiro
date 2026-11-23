package br.com.fincontrol.domain.service;

import br.com.fincontrol.domain.model.AgregadoMes;
import br.com.fincontrol.domain.model.AnaliseFinanceira;
import br.com.fincontrol.domain.model.PedidoAnalise;

public interface AnaliseFinanceiraPort {

    AnaliseFinanceira analisar(PedidoAnalise pedido, AgregadoMes agregado);

    String provedor();

    String modelo();
}
