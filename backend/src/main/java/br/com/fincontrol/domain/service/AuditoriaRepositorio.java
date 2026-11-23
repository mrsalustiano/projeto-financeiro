package br.com.fincontrol.domain.service;

import java.util.List;

import br.com.fincontrol.domain.model.FiltroAuditoria;
import br.com.fincontrol.domain.model.RegistroAuditoria;

public interface AuditoriaRepositorio {

    long inserir(RegistroAuditoria registro);

    List<RegistroAuditoria> listar(FiltroAuditoria filtro, Long cursor, int limite);
}
