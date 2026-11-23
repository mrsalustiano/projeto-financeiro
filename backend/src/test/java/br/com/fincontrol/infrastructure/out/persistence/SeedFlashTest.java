package br.com.fincontrol.infrastructure.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;

import br.com.fincontrol.application.ContaSeed;
import br.com.fincontrol.application.GarantirLancamentos;
import br.com.fincontrol.application.GarantirRecorrencias;
import br.com.fincontrol.application.LancamentoSeed;
import br.com.fincontrol.application.PessoaSeed;
import br.com.fincontrol.application.RecorrenciaSeed;
import br.com.fincontrol.application.SeedConfig;
import br.com.fincontrol.domain.model.Categoria;
import br.com.fincontrol.domain.model.Conta;
import br.com.fincontrol.domain.model.Dinheiro;
import br.com.fincontrol.domain.model.Lancamento;
import br.com.fincontrol.domain.model.Natureza;
import br.com.fincontrol.domain.model.Recorrencia;
import br.com.fincontrol.domain.model.Saldo;
import br.com.fincontrol.domain.model.TipoConta;
import br.com.fincontrol.domain.model.TipoDespesa;
import br.com.fincontrol.domain.service.CategoriaRepositorio;
import br.com.fincontrol.domain.service.ContaRepositorio;
import br.com.fincontrol.domain.service.IdempotenciaRepositorio;
import br.com.fincontrol.domain.service.LancamentoRepositorio;
import br.com.fincontrol.domain.service.RecorrenciaRepositorio;
import br.com.fincontrol.domain.service.Relogio;

@ExtendWith(MockitoExtension.class)
class SeedFlashTest {

    private static final Instant AGORA = Instant.parse("2026-11-22T03:00:00Z");

    @Mock private ContaRepositorio contas;
    @Mock private CategoriaRepositorio categorias;
    @Mock private LancamentoRepositorio lancamentos;
    @Mock private RecorrenciaRepositorio recorrencias;
    @Mock private IdempotenciaRepositorio idempotencia;
    @Mock private Relogio relogio;

    @Test
    void configuracaoTrazDepositoEGastosNaFlash() {
        SeedConfig config = new ConfiguracaoSeed().seedConfig(new MockEnvironment());
        PessoaSeed casa = config.pessoas().getFirst();

        assertThat(casa.contas()).extracting(ContaSeed::nome).containsExactly("Nubank", "Flash");
        assertThat(casa.lancamentos()).hasSize(2);
        assertThat(casa.lancamentos()).allSatisfy(item -> assertThat(item.conta()).isEqualTo("Flash"));
        assertThat(casa.lancamentos()).extracting(LancamentoSeed::categoria)
                .containsExactly("Alimentação", "Transporte");
        assertThat(casa.lancamentos()).extracting(item -> item.valor().valor().toPlainString())
                .containsExactly("320.00", "180.00");
        assertThat(casa.recorrencias()).hasSize(1);
        RecorrenciaSeed deposito = casa.recorrencias().getFirst();
        assertThat(deposito.conta()).isEqualTo("Flash");
        assertThat(deposito.categoria()).isEqualTo("Benefícios");
        assertThat(deposito.natureza()).isEqualTo(Natureza.RECEITA);
        assertThat(deposito.valor().valor()).isEqualByComparingTo("1570.00");
        assertThat(deposito.diaDoMes()).isZero();
    }

    @Test
    void garantirLancamentosGravaOsGastosDaFlash() {
        when(contas.listar(42L)).thenReturn(List.of(conta(2L, "Flash")));
        when(categorias.listar(42L)).thenReturn(List.of(
                new Categoria(3L, 42L, "Alimentação", Natureza.DESPESA),
                new Categoria(4L, 42L, "Transporte", Natureza.DESPESA)));
        when(idempotencia.buscar(eq(42L), anyString())).thenReturn(Optional.empty());
        when(lancamentos.inserir(any())).thenReturn(11L, 12L);

        new GarantirLancamentos(contas, categorias, lancamentos, idempotencia)
                .executar(42L, lancamentosSeed());

        ArgumentCaptor<Lancamento> capturado = ArgumentCaptor.forClass(Lancamento.class);
        verify(lancamentos, org.mockito.Mockito.times(2)).inserir(capturado.capture());
        assertThat(capturado.getAllValues()).extracting(Lancamento::contaId).containsOnly(2L);
        assertThat(capturado.getAllValues()).extracting(Lancamento::descricao)
                .containsExactly("Mercado do mês", "Combustível");
        verify(idempotencia, org.mockito.Mockito.times(2)).guardar(eq(42L), anyString(), anyString(), anyLong());
    }

    @Test
    void garantirLancamentosNaoRepeteNemSemVinculo() {
        when(idempotencia.buscar(eq(42L), anyString()))
                .thenReturn(Optional.of(new br.com.fincontrol.domain.model.RegistroIdempotencia("hash", 11L)));
        new GarantirLancamentos(contas, categorias, lancamentos, idempotencia).executar(42L, lancamentosSeed());
        verify(lancamentos, never()).inserir(any());

        when(idempotencia.buscar(eq(42L), anyString())).thenReturn(Optional.empty());
        when(contas.listar(42L)).thenReturn(List.of());
        when(categorias.listar(42L)).thenReturn(List.of());
        new GarantirLancamentos(contas, categorias, lancamentos, idempotencia).executar(42L, lancamentosSeed());
        verify(lancamentos, never()).inserir(any());
    }

    @Test
    void garantirRecorrenciasGravaODepositoDaFlash() {
        when(recorrencias.existeDescricao(42L, "Benefício Flash")).thenReturn(false);
        when(contas.listar(42L)).thenReturn(List.of(conta(2L, "Flash")));
        when(categorias.listar(42L)).thenReturn(List.of(new Categoria(5L, 42L, "Benefícios", Natureza.RECEITA)));
        when(relogio.agora()).thenReturn(AGORA);

        new GarantirRecorrencias(contas, categorias, recorrencias, relogio).executar(42L, recorrenciasSeed());

        ArgumentCaptor<Recorrencia> capturado = ArgumentCaptor.forClass(Recorrencia.class);
        verify(recorrencias).inserir(capturado.capture());
        Recorrencia deposito = capturado.getValue();
        assertThat(deposito.contaId()).isEqualTo(2L);
        assertThat(deposito.categoriaId()).isEqualTo(5L);
        assertThat(deposito.valor().valor()).isEqualByComparingTo("1570.00");
        assertThat(deposito.dia().dia()).isZero();
        assertThat(deposito.ativa()).isTrue();
    }

    @Test
    void garantirRecorrenciasNaoRepeteNemSemVinculo() {
        when(recorrencias.existeDescricao(42L, "Benefício Flash")).thenReturn(true);
        new GarantirRecorrencias(contas, categorias, recorrencias, relogio).executar(42L, recorrenciasSeed());
        verify(recorrencias, never()).inserir(any());

        when(recorrencias.existeDescricao(42L, "Benefício Flash")).thenReturn(false);
        when(contas.listar(42L)).thenReturn(List.of());
        when(categorias.listar(42L)).thenReturn(List.of());
        new GarantirRecorrencias(contas, categorias, recorrencias, relogio).executar(42L, recorrenciasSeed());
        verify(recorrencias, never()).inserir(any());
    }

    private static List<LancamentoSeed> lancamentosSeed() {
        return List.of(
                new LancamentoSeed("Flash", "Alimentação", Natureza.DESPESA, TipoDespesa.VARIAVEL,
                        new Dinheiro(new BigDecimal("320.00")), LocalDate.of(2026, 11, 5), "Mercado do mês"),
                new LancamentoSeed("Flash", "Transporte", Natureza.DESPESA, TipoDespesa.VARIAVEL,
                        new Dinheiro(new BigDecimal("180.00")), LocalDate.of(2026, 11, 8), "Combustível"));
    }

    private static List<RecorrenciaSeed> recorrenciasSeed() {
        return List.of(new RecorrenciaSeed("Flash", "Benefícios", Natureza.RECEITA, null,
                new Dinheiro(new BigDecimal("1570.00")), "Benefício Flash", 0));
    }

    private static Conta conta(long id, String nome) {
        return new Conta(id, 42L, nome, nome, TipoConta.BENEFICIO, Saldo.zero(), LocalDate.of(2026, 1, 1), null);
    }
}
