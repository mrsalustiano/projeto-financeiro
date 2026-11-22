package br.com.fincontrol.domain.service;

import java.util.Optional;

import br.com.fincontrol.domain.model.Acesso;
import br.com.fincontrol.domain.model.SujeitoToken;
import br.com.fincontrol.domain.model.TokenEmitido;

public interface TokenAcessoPort {

    TokenEmitido emitir(SujeitoToken sujeito);

    Optional<Acesso> ler(String jwt);
}
