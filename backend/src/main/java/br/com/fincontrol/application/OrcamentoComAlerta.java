package br.com.fincontrol.application;

import br.com.fincontrol.domain.model.AlertaOrcamento;
import br.com.fincontrol.domain.model.Orcamento;

public record OrcamentoComAlerta(Orcamento orcamento, AlertaOrcamento alerta) {
}
