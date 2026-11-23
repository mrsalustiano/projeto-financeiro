package br.com.fincontrol.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fincontrol.domain.model.Acesso;
import br.com.fincontrol.domain.model.AgregadoMes;
import br.com.fincontrol.domain.model.AnaliseFinanceira;
import br.com.fincontrol.domain.model.Categoria;
import br.com.fincontrol.domain.model.Conta;
import br.com.fincontrol.domain.model.Dinheiro;
import br.com.fincontrol.domain.model.Lancamento;
import br.com.fincontrol.domain.model.Natureza;
import br.com.fincontrol.domain.model.Papel;
import br.com.fincontrol.domain.model.PedidoAnalise;
import br.com.fincontrol.domain.model.Saldo;
import br.com.fincontrol.domain.model.SaldoConta;
import br.com.fincontrol.domain.model.TipoAnalise;
import br.com.fincontrol.domain.model.TipoConta;
import br.com.fincontrol.domain.model.TipoDespesa;
import br.com.fincontrol.domain.service.AcessoNegadoException;
import br.com.fincontrol.domain.service.AnaliseFinanceiraPort;
import br.com.fincontrol.domain.service.CategoriaRepositorio;
import br.com.fincontrol.domain.service.ContaRepositorio;
import br.com.fincontrol.domain.service.LancamentoRepositorio;

@ExtendWith(MockitoExtension.class)
class AnaliseUseCasesTest {

    private static final Instant AGORA = Instant.parse("2026-11-23T12:00:00Z");
    private static final Acesso ADMIN = new Acesso("7", Papel.ADMIN, 42L, "jti", AGORA);
    private static final Acesso MASTER = new Acesso("1", Papel.MASTER, null, "jti", AGORA);
    private static final PedidoAnalise PEDIDO =
            new PedidoAnalise(YearMonth.of(2026, 11), TipoAnalise.PRESUNTIVA, "onde cortar?");

    @Mock private LancamentoRepositorio lancamentos;
    @Mock private ContaRepositorio contas;
    @Mock private CategoriaRepositorio categorias;
    @Mock private AnaliseFinanceiraPort porta;

    @Test
    void agregaMesDoClienteDoToken() {
        when(lancamentos.listar(anyLong(), any())).thenReturn(List.of(
                lancamento(1L, Natureza.RECEITA, "4800.00", 10L, null),
                lancamento(2L, Natureza.DESPESA, "320.00", 11L, null),
                lancamento(3L, Natureza.DESPESA, "180.00", 11L, null),
                lancamento(4L, Natureza.DESPESA, "99.00", null, AGORA),
                lancamento(5L, Natureza.DESPESA, "50.00", null, null)));
        when(categorias.listar(42L)).thenReturn(List.of(
                new Categoria(10L, 42L, "Salário", Natureza.RECEITA),
                new Categoria(11L, 42L, "Alimentação", Natureza.DESPESA)));
        when(contas.listar(42L)).thenReturn(List.of(
                new Conta(1L, 42L, "Nubank", "Nubank", TipoConta.CORRENTE, new Saldo(new BigDecimal("100.00")),
                        LocalDate.of(2026, 1, 1), null),
                new Conta(2L, 42L, "Flash", "Flash", TipoConta.BENEFICIO, Saldo.zero(),
                        LocalDate.of(2026, 1, 1), null)));
        when(lancamentos.saldo(42L, 1L)).thenReturn(Optional.of(
                new SaldoConta(new BigDecimal("100.00"), new BigDecimal("4800.00"), new BigDecimal("320.00"))));
        when(lancamentos.saldo(42L, 2L)).thenReturn(Optional.empty());
        when(porta.analisar(any(), any())).thenReturn(new AnaliseFinanceira("ok", List.of("i"), List.of("a")));
        AnalisesFinanceiras casos = new AnalisesFinanceiras(lancamentos, contas, categorias, porta);

        AnaliseFinanceira analise = casos.analisar(ADMIN, PEDIDO);

        ArgumentCaptor<AgregadoMes> capturado = ArgumentCaptor.forClass(AgregadoMes.class);
        verify(porta).analisar(any(), capturado.capture());
        AgregadoMes agregado = capturado.getValue();
        assertThat(analise.resumo()).isEqualTo("ok");
        assertThat(agregado.mes()).isEqualTo(YearMonth.of(2026, 11));
        assertThat(agregado.receitas()).isEqualByComparingTo("4800.00");
        assertThat(agregado.despesas()).isEqualByComparingTo("550.00");
        assertThat(agregado.lancamentos()).isEqualTo(4);
        assertThat(agregado.saldo()).isEqualByComparingTo("4250.00");
        assertThat(agregado.categorias()).extracting(AgregadoMes.LinhaCategoria::nome)
                .containsExactly("Alimentação", "Sem categoria");
        assertThat(agregado.contas()).extracting(AgregadoMes.LinhaConta::nome).containsExactly("Flash", "Nubank");
        assertThat(agregado.contas().get(1).saldo()).isEqualByComparingTo("4580.00");
        assertThat(agregado.contas().get(0).saldo()).isEqualByComparingTo("0.00");
    }

    @Test
    void recusaMasterESemCliente() {
        AnalisesFinanceiras casos = new AnalisesFinanceiras(lancamentos, contas, categorias, porta);

        assertThatThrownBy(() -> casos.analisar(MASTER, PEDIDO)).isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> casos.analisar(null, PEDIDO)).isInstanceOf(AcessoNegadoException.class);
        verify(porta, never()).analisar(any(), any());
    }

    @Test
    void expoeProvedorEModelo() {
        when(porta.provedor()).thenReturn("ollama");
        when(porta.modelo()).thenReturn("qwen3.5:9b");
        AnalisesFinanceiras casos = new AnalisesFinanceiras(lancamentos, contas, categorias, porta);

        assertThat(casos.provedor()).isEqualTo("ollama");
        assertThat(casos.modelo()).isEqualTo("qwen3.5:9b");
    }

    private static Lancamento lancamento(long id, Natureza natureza, String valor, Long categoriaId, Instant estorno) {
        return new Lancamento(id, 42L, 1L, categoriaId, natureza, TipoDespesa.VARIAVEL,
                new Dinheiro(new BigDecimal(valor)), LocalDate.of(2026, 11, 10), "movimento",
                null, null, null, null, estorno, estorno == null ? null : "motivo", estorno == null ? null : "7", null);
    }
}
