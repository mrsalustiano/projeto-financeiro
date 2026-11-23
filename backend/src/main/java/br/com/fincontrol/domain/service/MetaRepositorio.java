package br.com.fincontrol.domain.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import br.com.fincontrol.domain.model.Aporte;
import br.com.fincontrol.domain.model.Meta;

public interface MetaRepositorio {

    long inserir(Meta meta);

    Optional<Meta> buscar(long clienteId, long id);

    List<Meta> listar(long clienteId);

    boolean existeNome(long clienteId, String nome);

    long inserirAporte(Aporte aporte);

    List<Aporte> listarAportes(long clienteId, long metaId);

    BigDecimal totalAportado(long clienteId, long metaId);
}
