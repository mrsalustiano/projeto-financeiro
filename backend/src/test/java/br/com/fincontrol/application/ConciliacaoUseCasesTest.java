package br.com.fincontrol.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
import br.com.fincontrol.domain.model.Comprovante;
import br.com.fincontrol.domain.model.Conciliacao;
import br.com.fincontrol.domain.model.Dinheiro;
import br.com.fincontrol.domain.model.FechamentoMes;
import br.com.fincontrol.domain.model.Lancamento;
import br.com.fincontrol.domain.model.Natureza;
import br.com.fincontrol.domain.model.Papel;
import br.com.fincontrol.domain.model.TipoComprovante;
import br.com.fincontrol.domain.model.TipoDespesa;
import br.com.fincontrol.domain.service.AcessoNegadoException;
import br.com.fincontrol.domain.service.ArmazenamentoPort;
import br.com.fincontrol.domain.service.ComprovanteGrandeException;
import br.com.fincontrol.domain.service.ComprovanteRepositorio;
import br.com.fincontrol.domain.service.ComprovanteTipoInvalidoException;
import br.com.fincontrol.domain.service.ConciliacaoInvalidaException;
import br.com.fincontrol.domain.service.ConciliacaoRepetidaException;
import br.com.fincontrol.domain.service.ConciliacaoRepositorio;
import br.com.fincontrol.domain.service.FechamentoMesRepositorio;
import br.com.fincontrol.domain.service.LancamentoRepositorio;
import br.com.fincontrol.domain.service.MesFechadoException;
import br.com.fincontrol.domain.service.NaoEncontradoException;
import br.com.fincontrol.domain.service.Relogio;

@ExtendWith(MockitoExtension.class)
class ConciliacaoUseCasesTest {

    private static final Instant AGORA = Instant.parse("2026-11-23T12:00:00Z");
    private static final LocalDate DIA = LocalDate.of(2026, 11, 10);
    private static final Acesso ADMIN = new Acesso("7", Papel.ADMIN, 42L, "jti", AGORA);
    private static final Acesso LEITOR = new Acesso("9", Papel.LEITOR, 42L, "jti", AGORA);
    private static final Acesso MASTER = new Acesso("1", Papel.MASTER, null, "jti", AGORA);

    @Mock private ConciliacaoRepositorio conciliacoes;
    @Mock private ComprovanteRepositorio comprovantes;
    @Mock private LancamentoRepositorio lancamentos;
    @Mock private FechamentoMesRepositorio fechamentos;
    @Mock private ArmazenamentoPort armazenamento;
    @Mock private Relogio relogio;

    @Test
    void sugereCandidatosParaLeitura() {
        when(lancamentos.buscar(42L, 1L)).thenReturn(Optional.of(lancamento(1L, null)));
        when(lancamentos.sugerirPar(42L, lancamento(1L, null))).thenReturn(List.of(lancamento(2L, "FIT-1")));
        Conciliacoes casos = new Conciliacoes(conciliacoes, lancamentos, fechamentos, relogio);

        assertThat(casos.sugerir(LEITOR, 1L)).hasSize(1);
        assertThatThrownBy(() -> casos.sugerir(MASTER, 1L)).isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    void sugereLancamentoInexistente() {
        when(lancamentos.buscar(42L, 1L)).thenReturn(Optional.empty());
        Conciliacoes casos = new Conciliacoes(conciliacoes, lancamentos, fechamentos, relogio);

        assertThatThrownBy(() -> casos.sugerir(ADMIN, 1L)).isInstanceOf(NaoEncontradoException.class);
    }

    @Test
    void conciliaEGravaOsDoisLados() {
        when(lancamentos.buscar(42L, 1L)).thenReturn(Optional.of(lancamento(1L, null)));
        when(lancamentos.buscar(42L, 2L)).thenReturn(Optional.of(lancamento(2L, "FIT-1")));
        when(fechamentos.buscar(eq(42L), anyInt(), anyInt())).thenReturn(Optional.empty());
        when(conciliacoes.buscarPorLancamento(42L, 1L)).thenReturn(Optional.empty());
        when(conciliacoes.buscarPorLancamento(42L, 2L)).thenReturn(Optional.empty());
        when(relogio.agora()).thenReturn(AGORA);
        when(conciliacoes.inserir(any())).thenReturn(5L);
        Conciliacoes casos = new Conciliacoes(conciliacoes, lancamentos, fechamentos, relogio);

        Conciliacao criada = casos.conciliar(ADMIN, 1L, 2L);

        assertThat(criada.id()).isEqualTo(5L);
        assertThat(criada.lancamentoId()).isEqualTo(1L);
        assertThat(criada.extratoId()).isEqualTo(2L);
        assertThat(criada.conciliadoPor()).isEqualTo("7");
        verify(conciliacoes).inserir(any());
    }

    @Test
    void recusaParInvalidoOuRepetido() {
        when(lancamentos.buscar(42L, 1L)).thenReturn(Optional.of(lancamento(1L, null)));
        when(lancamentos.buscar(42L, 2L)).thenReturn(Optional.of(lancamento(2L, "FIT-1")));
        when(fechamentos.buscar(eq(42L), anyInt(), anyInt())).thenReturn(Optional.empty());
        when(conciliacoes.buscarPorLancamento(42L, 1L)).thenReturn(Optional.of(
                new Conciliacao(9L, 42L, 10L, 1L, 3L, AGORA, "voce")));
        Conciliacoes casos = new Conciliacoes(conciliacoes, lancamentos, fechamentos, relogio);

        assertThatThrownBy(() -> casos.conciliar(ADMIN, 1L, 2L)).isInstanceOf(ConciliacaoRepetidaException.class);
        verify(conciliacoes, never()).inserir(any());
    }

    @Test
    void recusaConciliacaoInvalida() {
        when(lancamentos.buscar(42L, 1L)).thenReturn(Optional.of(lancamento(1L, null)));
        when(lancamentos.buscar(42L, 2L)).thenReturn(Optional.of(lancamento(2L, null)));
        when(fechamentos.buscar(eq(42L), anyInt(), anyInt())).thenReturn(Optional.empty());
        Conciliacoes casos = new Conciliacoes(conciliacoes, lancamentos, fechamentos, relogio);

        assertThatThrownBy(() -> casos.conciliar(ADMIN, 1L, 2L)).isInstanceOf(ConciliacaoInvalidaException.class);
    }

    @Test
    void recusaContasDivergentes() {
        when(lancamentos.buscar(42L, 1L)).thenReturn(Optional.of(lancamento(1L, null)));
        when(lancamentos.buscar(42L, 2L)).thenReturn(Optional.of(lancamento(2L, "FIT-1", 11L, DIA)));
        when(fechamentos.buscar(eq(42L), anyInt(), anyInt())).thenReturn(Optional.empty());
        Conciliacoes casos = new Conciliacoes(conciliacoes, lancamentos, fechamentos, relogio);

        assertThatThrownBy(() -> casos.conciliar(ADMIN, 1L, 2L)).isInstanceOf(ConciliacaoInvalidaException.class);
    }

    @Test
    void recusaPrazoAcimaDaJanela() {
        when(lancamentos.buscar(42L, 1L)).thenReturn(Optional.of(lancamento(1L, null)));
        when(lancamentos.buscar(42L, 2L)).thenReturn(Optional.of(lancamento(2L, "FIT-1", 10L, DIA.plusDays(6))));
        when(fechamentos.buscar(eq(42L), anyInt(), anyInt())).thenReturn(Optional.empty());
        Conciliacoes casos = new Conciliacoes(conciliacoes, lancamentos, fechamentos, relogio);

        assertThatThrownBy(() -> casos.conciliar(ADMIN, 1L, 2L)).isInstanceOf(ConciliacaoInvalidaException.class);
    }

    @Test
    void recusaConciliacaoEmMesFechado() {
        when(lancamentos.buscar(42L, 1L)).thenReturn(Optional.of(lancamento(1L, null)));
        when(lancamentos.buscar(42L, 2L)).thenReturn(Optional.of(lancamento(2L, "FIT-1")));
        when(fechamentos.buscar(42L, 2026, 11)).thenReturn(Optional.of(
                new FechamentoMes(1L, 42L, 2026, 11, AGORA, "voce")));
        Conciliacoes casos = new Conciliacoes(conciliacoes, lancamentos, fechamentos, relogio);

        assertThatThrownBy(() -> casos.conciliar(ADMIN, 1L, 2L)).isInstanceOf(MesFechadoException.class);
    }

    @Test
    void recusaLancamentoInexistenteNaConciliacao() {
        when(lancamentos.buscar(42L, 1L)).thenReturn(Optional.empty());
        Conciliacoes casos = new Conciliacoes(conciliacoes, lancamentos, fechamentos, relogio);

        assertThatThrownBy(() -> casos.conciliar(ADMIN, 1L, 2L)).isInstanceOf(NaoEncontradoException.class);
    }

    @Test
    void desfazConciliacao() {
        Conciliacao atual = new Conciliacao(5L, 42L, 10L, 1L, 2L, AGORA, "voce");
        when(conciliacoes.buscar(42L, 5L)).thenReturn(Optional.of(atual));
        when(lancamentos.buscar(42L, 1L)).thenReturn(Optional.of(lancamento(1L, null)));
        when(fechamentos.buscar(eq(42L), anyInt(), anyInt())).thenReturn(Optional.empty());
        when(conciliacoes.excluir(42L, 5L)).thenReturn(true);
        Conciliacoes casos = new Conciliacoes(conciliacoes, lancamentos, fechamentos, relogio);

        casos.desfazer(ADMIN, 5L);

        verify(conciliacoes).excluir(42L, 5L);
    }

    @Test
    void desfazerInexistenteOuFalha() {
        when(conciliacoes.buscar(42L, 5L)).thenReturn(Optional.empty());
        Conciliacoes casos = new Conciliacoes(conciliacoes, lancamentos, fechamentos, relogio);

        assertThatThrownBy(() -> casos.desfazer(ADMIN, 5L)).isInstanceOf(NaoEncontradoException.class);
    }

    @Test
    void desfazerSemLinhaAfetada() {
        Conciliacao atual = new Conciliacao(5L, 42L, 10L, 1L, 2L, AGORA, "voce");
        when(conciliacoes.buscar(42L, 5L)).thenReturn(Optional.of(atual));
        when(lancamentos.buscar(42L, 1L)).thenReturn(Optional.of(lancamento(1L, null)));
        when(fechamentos.buscar(eq(42L), anyInt(), anyInt())).thenReturn(Optional.empty());
        when(conciliacoes.excluir(42L, 5L)).thenReturn(false);
        Conciliacoes casos = new Conciliacoes(conciliacoes, lancamentos, fechamentos, relogio);

        assertThatThrownBy(() -> casos.desfazer(ADMIN, 5L)).isInstanceOf(NaoEncontradoException.class);
    }

    @Test
    void anexaComprovanteForaDoBanco() {
        when(lancamentos.buscar(42L, 1L)).thenReturn(Optional.of(lancamento(1L, null)));
        when(fechamentos.buscar(eq(42L), anyInt(), anyInt())).thenReturn(Optional.empty());
        when(relogio.agora()).thenReturn(AGORA);
        when(comprovantes.inserir(any())).thenReturn(3L);
        Comprovantes casos = new Comprovantes(comprovantes, lancamentos, fechamentos, armazenamento, relogio);

        Comprovante criado = casos.anexar(ADMIN, 1L, "recibo.png", png());

        assertThat(criado.id()).isEqualTo(3L);
        assertThat(criado.tipo()).isEqualTo(TipoComprovante.PNG);
        assertThat(criado.tamanho()).isEqualTo(png().length);
        verify(armazenamento).guardar(any(), any());
        verify(comprovantes).inserir(any());
    }

    @Test
    void anexoInvalidoNaoGrava() {
        when(lancamentos.buscar(42L, 1L)).thenReturn(Optional.of(lancamento(1L, null)));
        when(fechamentos.buscar(eq(42L), anyInt(), anyInt())).thenReturn(Optional.empty());
        Comprovantes casos = new Comprovantes(comprovantes, lancamentos, fechamentos, armazenamento, relogio);

        assertThatThrownBy(() -> casos.anexar(ADMIN, 1L, "recibo.gif", "GIF89a".getBytes()))
                .isInstanceOf(ComprovanteTipoInvalidoException.class);
        assertThatThrownBy(() -> casos.anexar(ADMIN, 1L, "recibo.png", new byte[0]))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> casos.anexar(ADMIN, 1L, "recibo.png", new byte[(int) Comprovante.TAMANHO_MAXIMO + 1]))
                .isInstanceOf(ComprovanteGrandeException.class);
        verify(armazenamento, never()).guardar(any(), any());
    }

    @Test
    void anexoEmMesFechadoBloqueia() {
        when(lancamentos.buscar(42L, 1L)).thenReturn(Optional.of(lancamento(1L, null)));
        when(fechamentos.buscar(42L, 2026, 11)).thenReturn(Optional.of(
                new FechamentoMes(1L, 42L, 2026, 11, AGORA, "voce")));
        Comprovantes casos = new Comprovantes(comprovantes, lancamentos, fechamentos, armazenamento, relogio);

        assertThatThrownBy(() -> casos.anexar(ADMIN, 1L, "recibo.png", png()))
                .isInstanceOf(MesFechadoException.class);
        verify(armazenamento, never()).guardar(any(), any());
    }

    @Test
    void anexoRemoveArquivoQuandoInsercaoFalha() {
        when(lancamentos.buscar(42L, 1L)).thenReturn(Optional.of(lancamento(1L, null)));
        when(fechamentos.buscar(eq(42L), anyInt(), anyInt())).thenReturn(Optional.empty());
        when(relogio.agora()).thenReturn(AGORA);
        when(comprovantes.inserir(any())).thenThrow(new IllegalStateException("falha"));
        Comprovantes casos = new Comprovantes(comprovantes, lancamentos, fechamentos, armazenamento, relogio);

        assertThatThrownBy(() -> casos.anexar(ADMIN, 1L, "recibo.png", png())).isInstanceOf(IllegalStateException.class);
        verify(armazenamento).remover(any());
    }

    @Test
    void listaBaixaERemoveComprovante() {
        Comprovante comprovante = new Comprovante(3L, 42L, 1L, "recibo.png", TipoComprovante.PNG, 4L, "chave", AGORA,
                "voce");
        when(lancamentos.buscar(42L, 1L)).thenReturn(Optional.of(lancamento(1L, null)));
        when(comprovantes.listar(42L, 1L)).thenReturn(List.of(comprovante));
        when(comprovantes.buscar(42L, 3L)).thenReturn(Optional.of(comprovante));
        when(armazenamento.ler("chave")).thenReturn(png());
        when(comprovantes.excluir(42L, 3L)).thenReturn(true);
        Comprovantes casos = new Comprovantes(comprovantes, lancamentos, fechamentos, armazenamento, relogio);

        assertThat(casos.listar(LEITOR, 1L)).hasSize(1);
        assertThat(casos.baixar(LEITOR, 3L).conteudo()).isEqualTo(png());
        casos.remover(ADMIN, 3L);
        verify(armazenamento).remover("chave");
    }

    @Test
    void listagemEBaixaSemRegistro() {
        when(lancamentos.buscar(42L, 1L)).thenReturn(Optional.empty());
        when(comprovantes.buscar(42L, 3L)).thenReturn(Optional.empty());
        Comprovantes casos = new Comprovantes(comprovantes, lancamentos, fechamentos, armazenamento, relogio);

        assertThatThrownBy(() -> casos.listar(ADMIN, 1L)).isInstanceOf(NaoEncontradoException.class);
        assertThatThrownBy(() -> casos.baixar(ADMIN, 3L)).isInstanceOf(NaoEncontradoException.class);
        assertThatThrownBy(() -> casos.remover(ADMIN, 3L)).isInstanceOf(NaoEncontradoException.class);
    }

    @Test
    void removeSemLinhaAfetada() {
        Comprovante comprovante = new Comprovante(3L, 42L, 1L, "recibo.png", TipoComprovante.PNG, 4L, "chave", AGORA,
                "voce");
        when(comprovantes.buscar(42L, 3L)).thenReturn(Optional.of(comprovante));
        when(comprovantes.excluir(42L, 3L)).thenReturn(false);
        Comprovantes casos = new Comprovantes(comprovantes, lancamentos, fechamentos, armazenamento, relogio);

        assertThatThrownBy(() -> casos.remover(ADMIN, 3L)).isInstanceOf(NaoEncontradoException.class);
        verify(armazenamento, never()).remover(any());
    }

    @Test
    void leitorNaoEscreve() {
        Conciliacoes conciliacoesCasos = new Conciliacoes(conciliacoes, lancamentos, fechamentos, relogio);
        Comprovantes comprovantesCasos = new Comprovantes(comprovantes, lancamentos, fechamentos, armazenamento,
                relogio);

        assertThatThrownBy(() -> conciliacoesCasos.conciliar(LEITOR, 1L, 2L)).isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> conciliacoesCasos.desfazer(LEITOR, 5L)).isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> comprovantesCasos.anexar(LEITOR, 1L, "a.png", png()))
                .isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> comprovantesCasos.remover(LEITOR, 3L)).isInstanceOf(AcessoNegadoException.class);
    }

    private static Lancamento lancamento(long id, String fitid) {
        return lancamento(id, fitid, 10L, DIA);
    }

    private static Lancamento lancamento(long id, String fitid, long contaId, LocalDate data) {
        return new Lancamento(id, 42L, contaId, null, Natureza.DESPESA, TipoDespesa.VARIAVEL,
                new Dinheiro(new BigDecimal("100.00")), data, "Compra", null, null, null, null, null, null, null, fitid);
    }

    private static byte[] png() {
        return new byte[] {(byte) 0x89, 'P', 'N', 'G', 1, 2, 3};
    }
}
