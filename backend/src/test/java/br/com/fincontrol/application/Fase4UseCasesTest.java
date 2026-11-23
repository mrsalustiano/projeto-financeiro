package br.com.fincontrol.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fincontrol.domain.model.Acesso;
import br.com.fincontrol.domain.model.Aporte;
import br.com.fincontrol.domain.model.Categoria;
import br.com.fincontrol.domain.model.Conta;
import br.com.fincontrol.domain.model.DiaDoMes;
import br.com.fincontrol.domain.model.Dinheiro;
import br.com.fincontrol.domain.model.FechamentoMes;
import br.com.fincontrol.domain.model.Lancamento;
import br.com.fincontrol.domain.model.Meta;
import br.com.fincontrol.domain.model.Natureza;
import br.com.fincontrol.domain.model.Orcamento;
import br.com.fincontrol.domain.model.Papel;
import br.com.fincontrol.domain.model.Recorrencia;
import br.com.fincontrol.domain.model.Saldo;
import br.com.fincontrol.domain.model.TipoConta;
import br.com.fincontrol.domain.model.TipoDespesa;
import br.com.fincontrol.domain.model.Transferencia;
import br.com.fincontrol.domain.service.AcessoNegadoException;
import br.com.fincontrol.domain.service.CategoriaRepositorio;
import br.com.fincontrol.domain.service.ContaRepositorio;
import br.com.fincontrol.domain.service.FechamentoMesRepositorio;
import br.com.fincontrol.domain.service.LancamentoRepositorio;
import br.com.fincontrol.domain.service.MetaRepositorio;
import br.com.fincontrol.domain.service.MesFechadoException;
import br.com.fincontrol.domain.service.NaoEncontradoException;
import br.com.fincontrol.domain.service.NomeEmUsoException;
import br.com.fincontrol.domain.service.OrcamentoRepositorio;
import br.com.fincontrol.domain.service.RecorrenciaInativaException;
import br.com.fincontrol.domain.service.RecorrenciaRepositorio;
import br.com.fincontrol.domain.service.Relogio;
import br.com.fincontrol.domain.service.TransferenciaInvalidaException;
import br.com.fincontrol.domain.service.TransferenciaRepositorio;

@ExtendWith(MockitoExtension.class)
class Fase4UseCasesTest {

    private static final Instant AGORA = Instant.parse("2026-11-22T03:00:00Z");
    private static final Acesso CASA = new Acesso("7", Papel.ADMIN, 42L, "jti", AGORA);
    private static final Acesso LEITOR = new Acesso("3", Papel.LEITOR, 42L, "jti", AGORA);

    @Mock private TransferenciaRepositorio transferencias;
    @Mock private OrcamentoRepositorio orcamentos;
    @Mock private MetaRepositorio metas;
    @Mock private RecorrenciaRepositorio recorrencias;
    @Mock private ContaRepositorio contas;
    @Mock private CategoriaRepositorio categorias;
    @Mock private LancamentoRepositorio lancamentos;
    @Mock private FechamentoMesRepositorio fechamentos;
    @Mock private Relogio relogio;

    @Test
    void transferenciaEntreContasDoCliente() {
        when(contas.buscar(42L, 1L)).thenReturn(Optional.of(conta(1L, "Nubank")));
        when(contas.buscar(42L, 2L)).thenReturn(Optional.of(conta(2L, "Flash")));
        when(fechamentos.buscar(42L, 2026, 11)).thenReturn(Optional.empty());
        when(transferencias.inserir(any())).thenReturn(9L);

        Transferencia criada = transferencias().criar(CASA, comando(1L, 2L));

        assertThat(criada.id()).isEqualTo(9L);
        assertThat(criada.contaDestinoId()).isEqualTo(2L);
        assertThat(transferencias().listar(CASA)).isEmpty();
    }

    @Test
    void transferenciaRecusaMesmaContaContaInexistenteEMesFechado() {
        assertThatThrownBy(() -> transferencias().criar(CASA, comando(1L, 1L)))
                .isInstanceOf(TransferenciaInvalidaException.class);
        when(contas.buscar(42L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> transferencias().criar(CASA, comando(1L, 2L)))
                .isInstanceOf(NaoEncontradoException.class);
        when(contas.buscar(42L, 1L)).thenReturn(Optional.of(conta(1L, "Nubank")));
        when(contas.buscar(42L, 2L)).thenReturn(Optional.of(conta(2L, "Flash")));
        when(fechamentos.buscar(42L, 2026, 11)).thenReturn(Optional.of(fechamento()));
        assertThatThrownBy(() -> transferencias().criar(CASA, comando(1L, 2L)))
                .isInstanceOf(MesFechadoException.class);
        assertThatThrownBy(() -> transferencias().criar(LEITOR, comando(1L, 2L)))
                .isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    void orcamentoDefineCalculaAlertaEExclui() {
        when(categorias.buscar(42L, 2L)).thenReturn(Optional.of(categoria()));
        when(orcamentos.salvar(any())).thenReturn(5L);
        when(lancamentos.gastoPorCategoria(eq(42L), eq(2L), any(), any())).thenReturn(new BigDecimal("850.00"));

        OrcamentoComAlerta definido = orcamentos().definir(CASA, 2L, 2026, 11, new Dinheiro(new BigDecimal("1000")));

        assertThat(definido.orcamento().id()).isEqualTo(5L);
        assertThat(definido.alerta().percentual()).isEqualByComparingTo("85.00");
        assertThat(definido.alerta().alerta()).isTrue();

        when(orcamentos.listar(42L, 2026, 11)).thenReturn(List.of(
                new Orcamento(5L, 42L, 2L, 2026, 11, new Dinheiro(new BigDecimal("1000")))));
        assertThat(orcamentos().listar(CASA, 2026, 11)).hasSize(1);

        when(orcamentos.excluir(42L, 2L, 2026, 11)).thenReturn(true, false);
        orcamentos().excluir(CASA, 2L, 2026, 11);
        assertThatThrownBy(() -> orcamentos().excluir(CASA, 2L, 2026, 11))
                .isInstanceOf(NaoEncontradoException.class);
    }

    @Test
    void orcamentoDeCategoriaInexistenteFalha() {
        when(categorias.buscar(42L, 2L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> orcamentos().definir(CASA, 2L, 2026, 11, new Dinheiro(BigDecimal.TEN)))
                .isInstanceOf(NaoEncontradoException.class);
    }

    @Test
    void metaCriaAportaECalculaProgresso() {
        when(metas.existeNome(42L, "Viagem")).thenReturn(false);
        when(relogio.agora()).thenReturn(AGORA);
        when(metas.inserir(any())).thenReturn(7L);
        when(metas.totalAportado(42L, 7L)).thenReturn(BigDecimal.ZERO);

        MetaComProgresso criada = metas().criar(CASA, "Viagem", new Dinheiro(new BigDecimal("1000")), null);
        assertThat(criada.meta().id()).isEqualTo(7L);
        assertThat(criada.progresso().percentual()).isEqualByComparingTo("0.00");

        when(metas.buscar(42L, 7L)).thenReturn(Optional.of(criada.meta()));
        when(metas.inserirAporte(any())).thenReturn(11L);
        Aporte aporte = metas().aportar(CASA, 7L, new Dinheiro(new BigDecimal("250")), LocalDate.of(2026, 11, 10));
        assertThat(aporte.id()).isEqualTo(11L);

        when(metas.totalAportado(42L, 7L)).thenReturn(new BigDecimal("250.00"));
        assertThat(metas().obter(CASA, 7L).progresso().percentual()).isEqualByComparingTo("25.00");
        when(metas.listar(42L)).thenReturn(List.of(criada.meta()));
        assertThat(metas().listar(CASA)).hasSize(1);
        when(metas.listarAportes(42L, 7L)).thenReturn(List.of(aporte));
        assertThat(metas().listarAportes(CASA, 7L)).hasSize(1);
    }

    @Test
    void metaRecusaNomeRepetidoEInexistente() {
        when(metas.existeNome(42L, "Viagem")).thenReturn(true);
        assertThatThrownBy(() -> metas().criar(CASA, "Viagem", new Dinheiro(BigDecimal.TEN), null))
                .isInstanceOf(NomeEmUsoException.class);
        when(metas.buscar(42L, 99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> metas().obter(CASA, 99L)).isInstanceOf(NaoEncontradoException.class);
        assertThatThrownBy(() -> metas().aportar(CASA, 99L, new Dinheiro(BigDecimal.TEN), LocalDate.now()))
                .isInstanceOf(NaoEncontradoException.class);
        assertThatThrownBy(() -> metas().listarAportes(CASA, 99L)).isInstanceOf(NaoEncontradoException.class);
    }

    @Test
    void recorrenciaCriaPausaERetoma() {
        when(contas.buscar(42L, 2L)).thenReturn(Optional.of(conta(2L, "Flash")));
        when(categorias.buscar(42L, 3L)).thenReturn(Optional.of(categoria()));
        when(recorrencias.existeDescricao(42L, "Benefício Flash")).thenReturn(false);
        when(relogio.agora()).thenReturn(AGORA);
        when(recorrencias.inserir(any())).thenReturn(4L);

        Recorrencia criada = recorrencias().criar(CASA, comandoRecorrencia());
        assertThat(criada.id()).isEqualTo(4L);
        assertThat(criada.dia().dia()).isZero();

        when(recorrencias.buscar(42L, 4L)).thenReturn(Optional.of(criada));
        Recorrencia pausada = recorrencias().pausar(CASA, 4L);
        assertThat(pausada.ativa()).isFalse();
        when(recorrencias.buscar(42L, 4L)).thenReturn(Optional.of(pausada));
        assertThat(recorrencias().retomar(CASA, 4L).ativa()).isTrue();
        verify(recorrencias, org.mockito.Mockito.times(2)).atualizar(any());
    }

    @Test
    void recorrenciaRecusaEstadoRepetidoEInexistente() {
        Recorrencia pausada = recorrencia().pausar(AGORA);
        when(recorrencias.buscar(42L, 4L)).thenReturn(Optional.of(pausada));
        assertThatThrownBy(() -> recorrencias().pausar(CASA, 4L)).isInstanceOf(RecorrenciaInativaException.class);
        when(recorrencias.buscar(42L, 5L)).thenReturn(Optional.of(recorrencia()));
        assertThatThrownBy(() -> recorrencias().retomar(CASA, 5L)).isInstanceOf(RecorrenciaInativaException.class);
        when(recorrencias.buscar(42L, 99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> recorrencias().pausar(CASA, 99L)).isInstanceOf(NaoEncontradoException.class);
        assertThatThrownBy(() -> recorrencias().retomar(CASA, 99L)).isInstanceOf(NaoEncontradoException.class);
    }

    @Test
    void recorrenciaRecusaVinculoENomeRepetido() {
        when(contas.buscar(42L, 2L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> recorrencias().criar(CASA, comandoRecorrencia()))
                .isInstanceOf(NaoEncontradoException.class);
        when(contas.buscar(42L, 2L)).thenReturn(Optional.of(conta(2L, "Flash")));
        when(categorias.buscar(42L, 3L)).thenReturn(Optional.of(categoria()));
        when(recorrencias.existeDescricao(42L, "Benefício Flash")).thenReturn(true);
        assertThatThrownBy(() -> recorrencias().criar(CASA, comandoRecorrencia()))
                .isInstanceOf(NomeEmUsoException.class);
    }

    @Test
    void jobCriaLancamentoNoUltimoDia() {
        when(recorrencias.listarAtivas()).thenReturn(List.of(recorrencia()));
        when(fechamentos.buscar(42L, 2026, 11)).thenReturn(Optional.empty());
        when(recorrencias.ocorrencia(42L, 4L, 2026, 11)).thenReturn(Optional.empty());
        when(lancamentos.inserir(any())).thenReturn(11L);
        when(lancamentos.buscar(42L, 11L)).thenReturn(Optional.of(lancamento(11L, "1570.00")));

        assertThat(recorrencias().executarDoDia(LocalDate.of(2026, 11, 30))).isEqualTo(1);
        verify(recorrencias).registrarOcorrencia(42L, 4L, 2026, 11, 11L);
    }

    @Test
    void jobNaoDuplicaNoMesmoMes() {
        when(recorrencias.listarAtivas()).thenReturn(List.of(recorrencia()));
        when(fechamentos.buscar(42L, 2026, 11)).thenReturn(Optional.empty());
        when(recorrencias.ocorrencia(42L, 4L, 2026, 11)).thenReturn(Optional.of(11L));
        when(lancamentos.buscar(42L, 11L)).thenReturn(Optional.of(lancamento(11L, "1570.00")));

        assertThat(recorrencias().executarDoDia(LocalDate.of(2026, 11, 30))).isZero();
        verify(lancamentos, never()).inserir(any());
        verify(lancamentos, never()).atualizar(any());
    }

    @Test
    void jobAjustaDepositoDentroDaFaixa() {
        when(recorrencias.listarAtivas()).thenReturn(List.of(recorrencia()));
        when(fechamentos.buscar(42L, 2026, 11)).thenReturn(Optional.empty());
        when(recorrencias.ocorrencia(42L, 4L, 2026, 11)).thenReturn(Optional.of(11L));
        when(lancamentos.buscar(42L, 11L)).thenReturn(Optional.of(lancamento(11L, "1600.00")));

        assertThat(recorrencias().executarDoDia(LocalDate.of(2026, 11, 30))).isZero();
        verify(lancamentos).atualizar(org.mockito.ArgumentMatchers.argThat(
                item -> item.valor().valor().compareTo(new BigDecimal("1570.00")) == 0));
    }

    @Test
    void jobIgnoraMesFechadoEDiaErrado() {
        when(recorrencias.listarAtivas()).thenReturn(List.of(recorrencia()));
        assertThat(recorrencias().executarDoDia(LocalDate.of(2026, 11, 15))).isZero();
        when(fechamentos.buscar(42L, 2026, 11)).thenReturn(Optional.of(fechamento()));
        assertThat(recorrencias().executarDoDia(LocalDate.of(2026, 11, 30))).isZero();
        verify(lancamentos, never()).inserir(any());
    }

    @Test
    void leitorNaoEscreveNasNovasRotas() {
        assertThatThrownBy(() -> transferencias().criar(LEITOR, comando(1L, 2L)))
                .isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> orcamentos().definir(LEITOR, 2L, 2026, 11, new Dinheiro(BigDecimal.TEN)))
                .isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> orcamentos().excluir(LEITOR, 2L, 2026, 11))
                .isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> metas().criar(LEITOR, "x", new Dinheiro(BigDecimal.TEN), null))
                .isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> metas().aportar(LEITOR, 1L, new Dinheiro(BigDecimal.TEN), LocalDate.now()))
                .isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> recorrencias().criar(LEITOR, comandoRecorrencia()))
                .isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> recorrencias().pausar(LEITOR, 1L)).isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> recorrencias().retomar(LEITOR, 1L)).isInstanceOf(AcessoNegadoException.class);
    }

    private Transferencias transferencias() {
        return new Transferencias(transferencias, contas, fechamentos);
    }

    private Orcamentos orcamentos() {
        return new Orcamentos(orcamentos, categorias, lancamentos);
    }

    private Metas metas() {
        return new Metas(metas, relogio);
    }

    private Recorrencias recorrencias() {
        return new Recorrencias(recorrencias, lancamentos, contas, categorias, fechamentos, relogio);
    }

    private static ComandoTransferencia comando(long origem, long destino) {
        return new ComandoTransferencia(origem, destino, new Dinheiro(new BigDecimal("100.00")),
                LocalDate.of(2026, 11, 10), "Reserva");
    }

    private static ComandoRecorrencia comandoRecorrencia() {
        return new ComandoRecorrencia(2L, 3L, Natureza.RECEITA, null,
                new Dinheiro(new BigDecimal("1570.00")), "Benefício Flash", 0);
    }

    private static Conta conta(long id, String nome) {
        return new Conta(id, 42L, nome, nome, TipoConta.CORRENTE, Saldo.zero(), LocalDate.of(2026, 1, 1), null);
    }

    private static Categoria categoria() {
        return new Categoria(3L, 42L, "Benefícios", Natureza.RECEITA);
    }

    private static FechamentoMes fechamento() {
        return new FechamentoMes(1L, 42L, 2026, 11, AGORA, "7");
    }

    private static Recorrencia recorrencia() {
        return new Recorrencia(4L, 42L, 2L, 3L, Natureza.RECEITA, null,
                new Dinheiro(new BigDecimal("1570.00")), "Benefício Flash", DiaDoMes.ultimo(), true, null, AGORA);
    }

    private static Lancamento lancamento(long id, String valor) {
        return new Lancamento(id, 42L, 2L, 3L, Natureza.RECEITA, null,
                new Dinheiro(new BigDecimal(valor)), LocalDate.of(2026, 11, 30), "Benefício Flash",
                null, null, null, null, null, null, null);
    }
}
