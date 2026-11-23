package br.com.fincontrol.application;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fincontrol.domain.model.Acesso;
import br.com.fincontrol.domain.model.BancoOfx;
import br.com.fincontrol.domain.model.Categoria;
import br.com.fincontrol.domain.model.Conta;
import br.com.fincontrol.domain.model.Dinheiro;
import br.com.fincontrol.domain.model.ExtratoOfx;
import br.com.fincontrol.domain.model.Lancamento;
import br.com.fincontrol.domain.model.MovimentoOfx;
import br.com.fincontrol.domain.model.Natureza;
import br.com.fincontrol.domain.model.RegraClassificacao;
import br.com.fincontrol.domain.model.TipoDespesa;
import br.com.fincontrol.domain.service.CategoriaRepositorio;
import br.com.fincontrol.domain.service.ContaRepositorio;
import br.com.fincontrol.domain.service.EscopoTenant;
import br.com.fincontrol.domain.service.IdempotenciaRepositorio;
import br.com.fincontrol.domain.service.LancamentoRepositorio;
import br.com.fincontrol.domain.service.NaoEncontradoException;
import br.com.fincontrol.domain.service.OfxContaDivergenteException;
import br.com.fincontrol.domain.service.OfxPort;
import br.com.fincontrol.domain.service.OfxSemTransacaoException;
import br.com.fincontrol.domain.service.RegraClassificacaoRepositorio;

@Service
public class ImportacaoOfx {

    private static final String CATEGORIA_RENDIMENTO = "Rendimentos";
    private static final String DESCRICAO_RENDIMENTO = "RENDIMENTO LIQUIDO";

    private final OfxPort ofx;
    private final ContaRepositorio contas;
    private final CategoriaRepositorio categorias;
    private final LancamentoRepositorio lancamentos;
    private final RegraClassificacaoRepositorio regras;
    private final IdempotenciaRepositorio idempotencia;

    public ImportacaoOfx(
            OfxPort ofx,
            ContaRepositorio contas,
            CategoriaRepositorio categorias,
            LancamentoRepositorio lancamentos,
            RegraClassificacaoRepositorio regras,
            IdempotenciaRepositorio idempotencia) {
        this.ofx = ofx;
        this.contas = contas;
        this.categorias = categorias;
        this.lancamentos = lancamentos;
        this.regras = regras;
        this.idempotencia = idempotencia;
    }

    public SugestaoOfx sugerir(Acesso acesso, byte[] arquivo) {
        long clienteId = EscopoTenant.leitura(acesso);
        ExtratoOfx extrato = ofx.ler(arquivo);
        List<Conta> sugeridas = contas.listar(clienteId).stream()
                .filter(conta -> mesmoBanco(conta, extrato))
                .toList();
        return new SugestaoOfx(nomeBanco(extrato), extrato.acctId(), extrato.moeda(), extrato.inicio(),
                extrato.fim(), extrato.movimentos().size(), sugeridas);
    }

    @Transactional
    public ResultadoImportacao importar(Acesso acesso, long contaId, byte[] arquivo) {
        long clienteId = EscopoTenant.escrita(acesso);
        Conta conta = contas.buscar(clienteId, contaId).orElseThrow(NaoEncontradoException::new);
        ExtratoOfx extrato = ofx.ler(arquivo);
        if (extrato.vazio()) {
            throw new OfxSemTransacaoException();
        }
        exigirMesmoBanco(conta, extrato);
        Map<String, Long> porNome = categorias(clienteId);
        List<String> avisos = new ArrayList<>(extrato.avisos());
        Contagem contagem = gravarMovimentos(clienteId, contaId, extrato, porNome);
        BigDecimal rendimento = gravarRendimento(clienteId, contaId, extrato, porNome, avisos);
        atualizarIdentificador(clienteId, contaId, extrato.acctId());
        return new ResultadoImportacao(contagem.importados(), contagem.duplicados(), contagem.semCategoria(),
                avisos, extrato.saldoInformado(), extrato.moeda(), nomeBanco(extrato), contaId,
                extrato.acctId(), rendimento);
    }

    private Contagem gravarMovimentos(
            long clienteId, long contaId, ExtratoOfx extrato, Map<String, Long> porNome) {
        List<RegraClassificacao> regrasDoCliente = regras.listar(clienteId);
        int importados = 0;
        int duplicados = 0;
        int semCategoria = 0;
        for (MovimentoOfx movimento : extrato.movimentos()) {
            if (lancamentos.existeFitid(clienteId, contaId, movimento.fitid())) {
                duplicados++;
                continue;
            }
            Long categoriaId = classificar(regrasDoCliente, porNome, movimento);
            if (categoriaId == null) {
                semCategoria++;
            }
            lancamentos.inserir(novoLancamento(clienteId, contaId, movimento, categoriaId));
            importados++;
        }
        return new Contagem(importados, duplicados, semCategoria);
    }

    private BigDecimal gravarRendimento(
            long clienteId, long contaId, ExtratoOfx extrato, Map<String, Long> porNome, List<String> avisos) {
        BigDecimal rendimento = extrato.rendimento();
        if (rendimento == null || rendimento.signum() <= 0) {
            return null;
        }
        if (extrato.fim() == null) {
            avisos.add("Rendimento sem data final ignorado.");
            return null;
        }
        String chave = "rendimento:" + contaId + ":" + extrato.inicio() + ":" + extrato.fim();
        if (idempotencia.buscar(clienteId, chave).isPresent()) {
            return null;
        }
        Lancamento novo = new Lancamento(0L, clienteId, contaId, porNome.get(CATEGORIA_RENDIMENTO),
                Natureza.RECEITA, null, new Dinheiro(rendimento), extrato.fim(),
                DESCRICAO_RENDIMENTO, null, null, null, null, null, null, null, null);
        long id = lancamentos.inserir(novo);
        idempotencia.guardar(clienteId, chave, Idempotencia.sha256(chave), id);
        return rendimento;
    }

    private static Lancamento novoLancamento(
            long clienteId, long contaId, MovimentoOfx movimento, Long categoriaId) {
        TipoDespesa tipo = movimento.natureza() == Natureza.DESPESA ? TipoDespesa.VARIAVEL : null;
        return new Lancamento(0L, clienteId, contaId, categoriaId, movimento.natureza(), tipo, movimento.valor(),
                movimento.data(), movimento.descricao(), null, null, null, null, null, null, null,
                movimento.fitid());
    }

    private static Long classificar(
            List<RegraClassificacao> regrasDoCliente, Map<String, Long> porNome, MovimentoOfx movimento) {
        for (RegraClassificacao regra : regrasDoCliente) {
            if (regra.classifica(movimento)) {
                return porNome.get(regra.categoria());
            }
        }
        return null;
    }

    private Map<String, Long> categorias(long clienteId) {
        return categorias.listar(clienteId).stream()
                .collect(Collectors.toMap(Categoria::nome, Categoria::id, (primeiro, segundo) -> primeiro));
    }

    private void atualizarIdentificador(long clienteId, long contaId, String identificador) {
        if (identificador != null && !identificador.isBlank()) {
            contas.atualizarIdentificador(clienteId, contaId, identificador.trim());
        }
    }

    private static void exigirMesmoBanco(Conta conta, ExtratoOfx extrato) {
        Optional<BancoOfx> doArquivo = BancoOfx.porCodigo(extrato.bankId());
        Optional<BancoOfx> daConta = BancoOfx.porNome(conta.banco());
        if (doArquivo.isPresent() && daConta.isPresent() && doArquivo.get() != daConta.get()) {
            throw new OfxContaDivergenteException();
        }
    }

    private static boolean mesmoBanco(Conta conta, ExtratoOfx extrato) {
        Optional<BancoOfx> doArquivo = BancoOfx.porCodigo(extrato.bankId());
        Optional<BancoOfx> daConta = BancoOfx.porNome(conta.banco());
        return doArquivo.isPresent() && daConta.isPresent() && doArquivo.get() == daConta.get();
    }

    private static String nomeBanco(ExtratoOfx extrato) {
        return BancoOfx.porCodigo(extrato.bankId())
                .map(BancoOfx::nome)
                .orElseGet(() -> extrato.org() == null ? extrato.bankId() : extrato.org());
    }

    private record Contagem(int importados, int duplicados, int semCategoria) {
    }
}
