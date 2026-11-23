package br.com.fincontrol.application;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fincontrol.domain.model.Acesso;
import br.com.fincontrol.domain.model.Lancamento;
import br.com.fincontrol.domain.model.ParcelaPlanejada;
import br.com.fincontrol.domain.model.Parcelamento;
import br.com.fincontrol.domain.model.RegistroIdempotencia;
import br.com.fincontrol.domain.service.CategoriaRepositorio;
import br.com.fincontrol.domain.service.ContaRepositorio;
import br.com.fincontrol.domain.service.EscopoTenant;
import br.com.fincontrol.domain.service.EstornoRepetidoException;
import br.com.fincontrol.domain.service.FechamentoMesRepositorio;
import br.com.fincontrol.domain.service.IdempotenciaConflitoException;
import br.com.fincontrol.domain.service.IdempotenciaRepositorio;
import br.com.fincontrol.domain.service.LancamentoEstornadoException;
import br.com.fincontrol.domain.service.LancamentoRepositorio;
import br.com.fincontrol.domain.service.MesFechadoException;
import br.com.fincontrol.domain.service.NaoEncontradoException;
import br.com.fincontrol.domain.service.ParcelaInvalidaException;
import br.com.fincontrol.domain.service.Relogio;

@Service
public class LancamentosEscrita {

    private final LancamentoRepositorio lancamentos;
    private final ContaRepositorio contas;
    private final CategoriaRepositorio categorias;
    private final FechamentoMesRepositorio fechamentos;
    private final IdempotenciaRepositorio idempotencia;
    private final Relogio relogio;

    public LancamentosEscrita(
            LancamentoRepositorio lancamentos,
            ContaRepositorio contas,
            CategoriaRepositorio categorias,
            FechamentoMesRepositorio fechamentos,
            IdempotenciaRepositorio idempotencia,
            Relogio relogio) {
        this.lancamentos = lancamentos;
        this.contas = contas;
        this.categorias = categorias;
        this.fechamentos = fechamentos;
        this.idempotencia = idempotencia;
        this.relogio = relogio;
    }

    @Transactional
    public List<Lancamento> criar(Acesso acesso, String chave, ComandoLancamento comando) {
        long clienteId = EscopoTenant.escrita(acesso);
        String hash = hashDe(chave, comando);
        List<Lancamento> repetido = repetido(clienteId, chave, hash);
        if (repetido != null) {
            return repetido;
        }
        exigirMesAberto(clienteId, comando.data());
        exigirVinculos(clienteId, comando.contaId(), comando.categoriaId());
        List<Lancamento> criados = gravar(clienteId, comando);
        if (hash != null) {
            idempotencia.guardar(clienteId, chave, hash, criados.getFirst().id());
        }
        return criados;
    }

    @Transactional
    public Lancamento editar(Acesso acesso, long id, ComandoLancamento comando) {
        long clienteId = EscopoTenant.escrita(acesso);
        Lancamento atual = lancamentos.buscar(clienteId, id).orElseThrow(NaoEncontradoException::new);
        if (atual.estornado()) {
            throw new LancamentoEstornadoException();
        }
        exigirMesAberto(clienteId, atual.data());
        exigirMesAberto(clienteId, comando.data());
        exigirVinculos(clienteId, comando.contaId(), comando.categoriaId());
        Lancamento novo = atual.editar(comando.contaId(), comando.categoriaId(), comando.natureza(),
                comando.tipoDespesa(), comando.valor(), comando.data(), comando.descricao());
        lancamentos.atualizar(novo);
        return novo;
    }

    @Transactional
    public Lancamento estornar(Acesso acesso, long id, String motivo) {
        long clienteId = EscopoTenant.escrita(acesso);
        Lancamento atual = lancamentos.buscar(clienteId, id).orElseThrow(NaoEncontradoException::new);
        if (atual.estornado()) {
            throw new EstornoRepetidoException();
        }
        exigirMesAberto(clienteId, atual.data());
        Lancamento estornado = atual.estornar(motivo, acesso.usuarioId(), relogio.agora());
        lancamentos.atualizar(estornado);
        return estornado;
    }

    @Transactional
    public void excluir(Acesso acesso, long id) {
        long clienteId = EscopoTenant.escrita(acesso);
        Lancamento atual = lancamentos.buscar(clienteId, id).orElseThrow(NaoEncontradoException::new);
        if (atual.estornado()) {
            throw new LancamentoEstornadoException();
        }
        exigirMesAberto(clienteId, atual.data());
        if (!lancamentos.excluir(clienteId, id)) {
            throw new NaoEncontradoException();
        }
    }

    private List<Lancamento> repetido(long clienteId, String chave, String hash) {
        if (hash == null) {
            return null;
        }
        Optional<RegistroIdempotencia> registro = idempotencia.buscar(clienteId, chave);
        if (registro.isEmpty()) {
            return null;
        }
        if (!registro.get().hashCorpo().equals(hash)) {
            throw new IdempotenciaConflitoException();
        }
        return carregar(clienteId, registro.get().recursoId());
    }

    private List<Lancamento> carregar(long clienteId, long id) {
        Lancamento lancamento = lancamentos.buscar(clienteId, id).orElseThrow(NaoEncontradoException::new);
        if (lancamento.parcelaId() == null) {
            return List.of(lancamento);
        }
        return lancamentos.listarPorParcela(clienteId, lancamento.parcelaId());
    }

    private List<Lancamento> gravar(long clienteId, ComandoLancamento comando) {
        if (comando.parcelas() == 1) {
            return List.of(inserir(clienteId, comando, null, null, null));
        }
        Parcelamento parcelamento = planejar(comando);
        long parcelaId = lancamentos.inserirParcela(clienteId, parcelamento);
        List<Lancamento> criados = new ArrayList<>(parcelamento.total());
        for (ParcelaPlanejada planejada : parcelamento.parcelas()) {
            criados.add(inserir(clienteId, comando, parcelaId, parcelamento.grupo(), planejada));
        }
        return criados;
    }

    private Lancamento inserir(
            long clienteId, ComandoLancamento comando, Long parcelaId, String grupo, ParcelaPlanejada planejada) {
        Lancamento novo = new Lancamento(
                0L,
                clienteId,
                comando.contaId(),
                comando.categoriaId(),
                comando.natureza(),
                comando.tipoDespesa(),
                planejada == null ? comando.valor() : planejada.valor(),
                planejada == null ? comando.data() : planejada.data(),
                comando.descricao(),
                parcelaId,
                grupo,
                planejada == null ? null : planejada.numero(),
                planejada == null ? null : comando.parcelas(),
                null,
                null,
                null);
        return comId(novo, lancamentos.inserir(novo));
    }

    private static Parcelamento planejar(ComandoLancamento comando) {
        try {
            return Parcelamento.planejar(
                    UUID.randomUUID().toString(), comando.valor(), comando.parcelas(), comando.data());
        } catch (IllegalArgumentException ex) {
            throw new ParcelaInvalidaException();
        }
    }

    private void exigirMesAberto(long clienteId, LocalDate data) {
        if (fechamentos.buscar(clienteId, data.getYear(), data.getMonthValue()).isPresent()) {
            throw new MesFechadoException();
        }
    }

    private void exigirVinculos(long clienteId, long contaId, long categoriaId) {
        if (contas.buscar(clienteId, contaId).isEmpty() || categorias.buscar(clienteId, categoriaId).isEmpty()) {
            throw new NaoEncontradoException();
        }
    }

    private static String hashDe(String chave, ComandoLancamento comando) {
        if (chave == null || chave.isBlank()) {
            return null;
        }
        return Idempotencia.hash(comando);
    }

    private static Lancamento comId(Lancamento lancamento, long id) {
        return new Lancamento(id, lancamento.clienteId(), lancamento.contaId(), lancamento.categoriaId(),
                lancamento.natureza(), lancamento.tipoDespesa(), lancamento.valor(), lancamento.data(),
                lancamento.descricao(), lancamento.parcelaId(), lancamento.grupoParcela(),
                lancamento.numeroParcela(), lancamento.totalParcelas(), lancamento.estornadoEm(),
                lancamento.estornoMotivo(), lancamento.estornadoPor(), lancamento.fitid());
    }
}
