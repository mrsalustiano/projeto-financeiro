package br.com.fincontrol.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fincontrol.domain.model.Acesso;
import br.com.fincontrol.domain.model.Categoria;
import br.com.fincontrol.domain.model.Conta;
import br.com.fincontrol.domain.model.Dinheiro;
import br.com.fincontrol.domain.model.FechamentoMes;
import br.com.fincontrol.domain.model.FiltroLancamento;
import br.com.fincontrol.domain.model.Lancamento;
import br.com.fincontrol.domain.model.Natureza;
import br.com.fincontrol.domain.model.Papel;
import br.com.fincontrol.domain.model.RegistroIdempotencia;
import br.com.fincontrol.domain.model.Saldo;
import br.com.fincontrol.domain.model.SaldoConta;
import br.com.fincontrol.domain.model.TipoConta;
import br.com.fincontrol.domain.model.TipoDespesa;
import br.com.fincontrol.domain.service.AcessoNegadoException;
import br.com.fincontrol.domain.service.CategoriaRepositorio;
import br.com.fincontrol.domain.service.ContaRepositorio;
import br.com.fincontrol.domain.service.FechamentoMesRepositorio;
import br.com.fincontrol.domain.service.IdempotenciaConflitoException;
import br.com.fincontrol.domain.service.IdempotenciaRepositorio;
import br.com.fincontrol.domain.service.LancamentoEstornadoException;
import br.com.fincontrol.domain.service.LancamentoRepositorio;
import br.com.fincontrol.domain.service.MesFechadoException;
import br.com.fincontrol.domain.service.NaoEncontradoException;
import br.com.fincontrol.domain.service.Relogio;

@ExtendWith(MockitoExtension.class)
class LancamentosUseCasesTest {

    private static final Instant AGORA = Instant.parse("2026-11-22T03:00:00Z");
    private static final Acesso CASA = new Acesso("7", Papel.ADMIN, 42L, "jti", AGORA);
    private static final Acesso LEITOR = new Acesso("3", Papel.LEITOR, 42L, "jti", AGORA);

    @Mock private LancamentoRepositorio lancamentos;
    @Mock private ContaRepositorio contas;
    @Mock private CategoriaRepositorio categorias;
    @Mock private FechamentoMesRepositorio fechamentos;
    @Mock private IdempotenciaRepositorio idempotencia;
    @Mock private Relogio relogio;

    @Test
    void criaParceladoEGuardaIdempotencia() {
        when(contas.buscar(42L, 1L)).thenReturn(Optional.of(conta()));
        when(categorias.buscar(42L, 2L)).thenReturn(Optional.of(categoria()));
        when(fechamentos.buscar(42L, 2026, 11)).thenReturn(Optional.empty());
        when(lancamentos.inserirParcela(eq(42L), any())).thenReturn(9L);
        when(lancamentos.inserir(any())).thenReturn(11L, 12L, 13L);
        when(idempotencia.buscar(42L, "k")).thenReturn(Optional.empty());

        List<Lancamento> criados = escrita().criar(CASA, "k", comando(3));

        assertThat(criados).hasSize(3);
        assertThat(criados.getFirst().grupoParcela()).isNotBlank();
        assertThat(criados).extracting(Lancamento::numeroParcela).containsExactly(1, 2, 3);
        assertThat(criados.getFirst().valor().valor()).isEqualByComparingTo("33.34");
        verify(idempotencia).guardar(eq(42L), eq("k"), anyString(), eq(11L));
    }

    @Test
    void idempotenciaDevolveOMesmoRecurso() {
        ComandoLancamento comando = comando(1);
        when(idempotencia.buscar(42L, "k"))
                .thenReturn(Optional.of(new RegistroIdempotencia(Idempotencia.hash(comando), 11L)));
        when(lancamentos.buscar(42L, 11L)).thenReturn(Optional.of(lancamento(11L, null, null)));

        List<Lancamento> repetido = escrita().criar(CASA, "k", comando);

        assertThat(repetido).hasSize(1);
        assertThat(repetido.getFirst().id()).isEqualTo(11L);
        verify(lancamentos, never()).inserir(any());
    }

    @Test
    void idempotenciaComOutroCorpoConflita() {
        when(idempotencia.buscar(42L, "k"))
                .thenReturn(Optional.of(new RegistroIdempotencia("outro-hash", 11L)));

        assertThatThrownBy(() -> escrita().criar(CASA, "k", comando(1)))
                .isInstanceOf(IdempotenciaConflitoException.class);
    }

    @Test
    void mesFechadoBloqueiaCriacao() {
        when(fechamentos.buscar(42L, 2026, 11)).thenReturn(Optional.of(fechamento()));

        assertThatThrownBy(() -> escrita().criar(CASA, null, comando(1)))
                .isInstanceOf(MesFechadoException.class);
    }

    @Test
    void contaOuCategoriaDeOutroClienteNaoExiste() {
        when(fechamentos.buscar(42L, 2026, 11)).thenReturn(Optional.empty());
        when(contas.buscar(42L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> escrita().criar(CASA, null, comando(1)))
                .isInstanceOf(NaoEncontradoException.class);
    }

    @Test
    void editaEEstorna() {
        when(lancamentos.buscar(42L, 11L)).thenReturn(Optional.of(lancamento(11L, null, null)));
        when(fechamentos.buscar(42L, 2026, 11)).thenReturn(Optional.empty());
        when(contas.buscar(42L, 1L)).thenReturn(Optional.of(conta()));
        when(categorias.buscar(42L, 2L)).thenReturn(Optional.of(categoria()));

        Lancamento editado = escrita().editar(CASA, 11L, comando(1));

        assertThat(editado.valor().valor()).isEqualByComparingTo("100.00");
        verify(lancamentos).atualizar(editado);
        when(relogio.agora()).thenReturn(AGORA);
        Lancamento estornado = escrita().estornar(CASA, 11L, "engano");
        assertThat(estornado.estornado()).isTrue();
        assertThat(estornado.estornoMotivo()).isEqualTo("engano");
    }

    @Test
    void estornadoNaoEditaNemExclui() {
        Lancamento estornado = lancamento(11L, null, null).estornar("engano", "7", AGORA);
        when(lancamentos.buscar(42L, 11L)).thenReturn(Optional.of(estornado));

        assertThatThrownBy(() -> escrita().editar(CASA, 11L, comando(1)))
                .isInstanceOf(LancamentoEstornadoException.class);
        assertThatThrownBy(() -> escrita().excluir(CASA, 11L))
                .isInstanceOf(LancamentoEstornadoException.class);
    }

    @Test
    void excluiEInexistenteSome() {
        when(lancamentos.buscar(42L, 11L)).thenReturn(Optional.of(lancamento(11L, null, null)));
        when(fechamentos.buscar(42L, 2026, 11)).thenReturn(Optional.empty());
        when(lancamentos.excluir(42L, 11L)).thenReturn(true);
        escrita().excluir(CASA, 11L);
        verify(lancamentos).excluir(42L, 11L);

        when(lancamentos.buscar(42L, 99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> escrita().excluir(CASA, 99L)).isInstanceOf(NaoEncontradoException.class);
        assertThatThrownBy(() -> escrita().estornar(CASA, 99L, "x")).isInstanceOf(NaoEncontradoException.class);
        assertThatThrownBy(() -> escrita().editar(CASA, 99L, comando(1))).isInstanceOf(NaoEncontradoException.class);
    }

    @Test
    void consultaListaObterESaldo() {
        when(lancamentos.listar(eq(42L), any())).thenReturn(List.of(lancamento(1L, null, null)));
        when(lancamentos.buscar(42L, 1L)).thenReturn(Optional.of(lancamento(1L, null, null)));
        when(contas.buscar(42L, 1L)).thenReturn(Optional.of(conta()));
        when(lancamentos.saldo(42L, 1L)).thenReturn(Optional.of(
                new SaldoConta(new BigDecimal("100"), new BigDecimal("50.50"), new BigDecimal("30.25"))));
        LancamentosConsulta consulta = new LancamentosConsulta(lancamentos, contas);

        assertThat(consulta.listar(CASA, FiltroLancamento.vazio())).hasSize(1);
        assertThat(consulta.obter(CASA, 1L).id()).isEqualTo(1L);
        assertThat(consulta.saldo(CASA, 1L).saldo()).isEqualByComparingTo("120.25");
    }

    @Test
    void saldoDeContaInexistenteFalha() {
        when(contas.buscar(42L, 1L)).thenReturn(Optional.empty());
        when(contas.buscar(42L, 2L)).thenReturn(Optional.of(conta()));
        when(lancamentos.saldo(42L, 2L)).thenReturn(Optional.empty());
        LancamentosConsulta consulta = new LancamentosConsulta(lancamentos, contas);

        assertThatThrownBy(() -> consulta.saldo(CASA, 1L)).isInstanceOf(NaoEncontradoException.class);
        assertThatThrownBy(() -> consulta.saldo(CASA, 2L)).isInstanceOf(NaoEncontradoException.class);
    }

    @Test
    void fechamentoCriaReabreELista() {
        when(fechamentos.buscar(42L, 2026, 11)).thenReturn(Optional.of(fechamento()));
        assertThat(new FechamentosMes(fechamentos, relogio).fechar(CASA, 2026, 11).id()).isEqualTo(1L);

        when(fechamentos.buscar(42L, 2026, 12)).thenReturn(Optional.empty());
        when(relogio.agora()).thenReturn(AGORA);
        when(fechamentos.inserir(any())).thenReturn(5L);
        FechamentoMes novo = new FechamentosMes(fechamentos, relogio).fechar(CASA, 2026, 12);
        assertThat(novo.id()).isEqualTo(5L);
        assertThat(novo.fechadoPor()).isEqualTo("7");

        when(fechamentos.excluir(42L, 2026, 12)).thenReturn(true);
        new FechamentosMes(fechamentos, relogio).reabrir(CASA, 2026, 12);
        when(fechamentos.excluir(42L, 2026, 1)).thenReturn(false);
        assertThatThrownBy(() -> new FechamentosMes(fechamentos, relogio).reabrir(CASA, 2026, 1))
                .isInstanceOf(NaoEncontradoException.class);

        when(fechamentos.listar(42L)).thenReturn(List.of(fechamento()));
        assertThat(new FechamentosMes(fechamentos, relogio).listar(CASA)).hasSize(1);
    }

    @Test
    void leitorNaoEscreve() {
        assertThatThrownBy(() -> escrita().criar(LEITOR, null, comando(1))).isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> escrita().editar(LEITOR, 1L, comando(1))).isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> escrita().estornar(LEITOR, 1L, "x")).isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> escrita().excluir(LEITOR, 1L)).isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> new FechamentosMes(fechamentos, relogio).fechar(LEITOR, 2026, 11))
                .isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> new FechamentosMes(fechamentos, relogio).reabrir(LEITOR, 2026, 11))
                .isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    void hashMudaComOConteudo() {
        String primeiro = Idempotencia.hash(comando(1));
        String segundo = Idempotencia.hash(new ComandoLancamento(1L, 2L, Natureza.DESPESA, TipoDespesa.VARIAVEL,
                new Dinheiro(new BigDecimal("100")), LocalDate.of(2026, 11, 10), "Outra", 1));

        assertThat(primeiro).hasSize(64).isNotEqualTo(segundo);
        assertThat(Idempotencia.sha256("abc")).isEqualTo(
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    private LancamentosEscrita escrita() {
        return new LancamentosEscrita(lancamentos, contas, categorias, fechamentos, idempotencia, relogio);
    }

    private static ComandoLancamento comando(int parcelas) {
        return new ComandoLancamento(1L, 2L, Natureza.DESPESA, TipoDespesa.VARIAVEL,
                new Dinheiro(new BigDecimal("100.00")), LocalDate.of(2026, 11, 10), "Compra", parcelas);
    }

    private static Lancamento lancamento(long id, Long parcelaId, String grupo) {
        return new Lancamento(id, 42L, 1L, 2L, Natureza.DESPESA, TipoDespesa.VARIAVEL,
                new Dinheiro(new BigDecimal("100.00")), LocalDate.of(2026, 11, 10), "Compra",
                parcelaId, grupo, parcelaId == null ? null : 1, parcelaId == null ? null : 3, null, null, null);
    }

    private static Conta conta() {
        return new Conta(1L, 42L, "Nubank", "Nubank", TipoConta.CORRENTE, Saldo.zero(), LocalDate.of(2026, 1, 1), null);
    }

    private static Categoria categoria() {
        return new Categoria(2L, 42L, "Mercado", Natureza.DESPESA);
    }

    private static FechamentoMes fechamento() {
        return new FechamentoMes(1L, 42L, 2026, 11, AGORA, "7");
    }
}
