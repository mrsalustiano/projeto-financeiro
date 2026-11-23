package br.com.fincontrol.domain.service;

import br.com.fincontrol.domain.model.ExtratoOfx;

public interface OfxPort {

    ExtratoOfx ler(byte[] arquivo);
}
