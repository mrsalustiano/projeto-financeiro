package br.com.fincontrol.application;

import br.com.fincontrol.domain.model.Comprovante;

public record ComprovanteBaixado(Comprovante comprovante, byte[] conteudo) {
}
