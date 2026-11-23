package br.com.fincontrol.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fincontrol.domain.model.Acesso;
import br.com.fincontrol.domain.model.AcaoAuditoria;
import br.com.fincontrol.domain.model.Comprovante;
import br.com.fincontrol.domain.model.Lancamento;
import br.com.fincontrol.domain.model.TipoComprovante;
import br.com.fincontrol.domain.service.ArmazenamentoPort;
import br.com.fincontrol.domain.service.ComprovanteGrandeException;
import br.com.fincontrol.domain.service.ComprovanteRepositorio;
import br.com.fincontrol.domain.service.ComprovanteTipoInvalidoException;
import br.com.fincontrol.domain.service.EscopoTenant;
import br.com.fincontrol.domain.service.FechamentoMesRepositorio;
import br.com.fincontrol.domain.service.LancamentoRepositorio;
import br.com.fincontrol.domain.service.MesFechadoException;
import br.com.fincontrol.domain.service.NaoEncontradoException;
import br.com.fincontrol.domain.service.Relogio;

@Service
public class Comprovantes {

    private final ComprovanteRepositorio comprovantes;
    private final LancamentoRepositorio lancamentos;
    private final FechamentoMesRepositorio fechamentos;
    private final ArmazenamentoPort armazenamento;
    private final Relogio relogio;

    public Comprovantes(
            ComprovanteRepositorio comprovantes,
            LancamentoRepositorio lancamentos,
            FechamentoMesRepositorio fechamentos,
            ArmazenamentoPort armazenamento,
            Relogio relogio) {
        this.comprovantes = comprovantes;
        this.lancamentos = lancamentos;
        this.fechamentos = fechamentos;
        this.armazenamento = armazenamento;
        this.relogio = relogio;
    }

    public List<Comprovante> listar(Acesso acesso, long lancamentoId) {
        long clienteId = EscopoTenant.leitura(acesso);
        exigirLancamento(clienteId, lancamentoId);
        return comprovantes.listar(clienteId, lancamentoId);
    }

    public ComprovanteBaixado baixar(Acesso acesso, long id) {
        long clienteId = EscopoTenant.leitura(acesso);
        Comprovante comprovante = comprovantes.buscar(clienteId, id).orElseThrow(NaoEncontradoException::new);
        return new ComprovanteBaixado(comprovante, armazenamento.ler(comprovante.storageKey()));
    }

    @Transactional
    @Auditavel(acao = AcaoAuditoria.CREATE, recurso = "comprovante")
    public Comprovante anexar(Acesso acesso, long lancamentoId, String nomeArquivo, byte[] conteudo) {
        long clienteId = EscopoTenant.escrita(acesso);
        Lancamento lancamento = exigirLancamento(clienteId, lancamentoId);
        exigirMesAberto(clienteId, lancamento);
        exigirTamanho(conteudo);
        TipoComprovante tipo = TipoComprovante.detectar(conteudo).orElseThrow(ComprovanteTipoInvalidoException::new);
        String chave = UUID.randomUUID().toString();
        Comprovante novo = new Comprovante(0L, clienteId, lancamentoId, nomeArquivo, tipo, conteudo.length, chave,
                relogio.agora(), acesso.usuarioId());
        armazenamento.guardar(chave, conteudo);
        try {
            long id = comprovantes.inserir(novo);
            return new Comprovante(id, clienteId, lancamentoId, novo.nomeArquivo(), tipo, novo.tamanho(), chave,
                    novo.criadoEm(), novo.criadoPor());
        } catch (RuntimeException ex) {
            armazenamento.remover(chave);
            throw ex;
        }
    }

    @Transactional
    @Auditavel(acao = AcaoAuditoria.DELETE, recurso = "comprovante")
    public void remover(Acesso acesso, long id) {
        long clienteId = EscopoTenant.escrita(acesso);
        Comprovante comprovante = comprovantes.buscar(clienteId, id).orElseThrow(NaoEncontradoException::new);
        ContextoAuditoria.antes(comprovante);
        if (!comprovantes.excluir(clienteId, id)) {
            throw new NaoEncontradoException();
        }
        armazenamento.remover(comprovante.storageKey());
    }

    private Lancamento exigirLancamento(long clienteId, long lancamentoId) {
        return lancamentos.buscar(clienteId, lancamentoId).orElseThrow(NaoEncontradoException::new);
    }

    private void exigirMesAberto(long clienteId, Lancamento lancamento) {
        if (fechamentos.buscar(clienteId, lancamento.data().getYear(), lancamento.data().getMonthValue()).isPresent()) {
            throw new MesFechadoException();
        }
    }

    private static void exigirTamanho(byte[] conteudo) {
        if (conteudo == null || conteudo.length == 0) {
            throw new IllegalArgumentException("Arquivo obrigatório.");
        }
        if (conteudo.length > Comprovante.TAMANHO_MAXIMO) {
            throw new ComprovanteGrandeException();
        }
    }
}
