package br.com.fincontrol.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fincontrol.domain.model.Acesso;
import br.com.fincontrol.domain.model.Aporte;
import br.com.fincontrol.domain.model.Dinheiro;
import br.com.fincontrol.domain.model.Meta;
import br.com.fincontrol.domain.service.EscopoTenant;
import br.com.fincontrol.domain.service.MetaRepositorio;
import br.com.fincontrol.domain.service.NaoEncontradoException;
import br.com.fincontrol.domain.service.NomeEmUsoException;
import br.com.fincontrol.domain.service.Relogio;

@Service
public class Metas {

    private final MetaRepositorio metas;
    private final Relogio relogio;

    public Metas(MetaRepositorio metas, Relogio relogio) {
        this.metas = metas;
        this.relogio = relogio;
    }

    @Transactional
    public MetaComProgresso criar(Acesso acesso, String nome, Dinheiro valorAlvo, LocalDate dataAlvo) {
        long clienteId = EscopoTenant.escrita(acesso);
        if (metas.existeNome(clienteId, nome == null ? "" : nome.trim())) {
            throw new NomeEmUsoException();
        }
        Instant agora = relogio.agora();
        Meta nova = new Meta(0L, clienteId, nome, valorAlvo, dataAlvo, agora);
        long id = metas.inserir(nova);
        return comProgresso(new Meta(id, clienteId, nova.nome(), valorAlvo, dataAlvo, agora));
    }

    public List<MetaComProgresso> listar(Acesso acesso) {
        long clienteId = EscopoTenant.leitura(acesso);
        List<MetaComProgresso> lista = new ArrayList<>();
        for (Meta meta : metas.listar(clienteId)) {
            lista.add(comProgresso(meta));
        }
        return lista;
    }

    public MetaComProgresso obter(Acesso acesso, long id) {
        long clienteId = EscopoTenant.leitura(acesso);
        return comProgresso(metas.buscar(clienteId, id).orElseThrow(NaoEncontradoException::new));
    }

    @Transactional
    public Aporte aportar(Acesso acesso, long id, Dinheiro valor, LocalDate data) {
        long clienteId = EscopoTenant.escrita(acesso);
        metas.buscar(clienteId, id).orElseThrow(NaoEncontradoException::new);
        Aporte aporte = new Aporte(0L, clienteId, id, valor, data);
        long aporteId = metas.inserirAporte(aporte);
        return new Aporte(aporteId, clienteId, id, valor, data);
    }

    public List<Aporte> listarAportes(Acesso acesso, long id) {
        long clienteId = EscopoTenant.leitura(acesso);
        metas.buscar(clienteId, id).orElseThrow(NaoEncontradoException::new);
        return metas.listarAportes(clienteId, id);
    }

    private MetaComProgresso comProgresso(Meta meta) {
        return new MetaComProgresso(meta, meta.progresso(metas.totalAportado(meta.clienteId(), meta.id())));
    }
}
