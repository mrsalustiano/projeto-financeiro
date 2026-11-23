package br.com.fincontrol.infrastructure.out.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;

import br.com.fincontrol.domain.model.AgregadoMes;
import br.com.fincontrol.domain.model.AnaliseFinanceira;
import br.com.fincontrol.domain.model.PedidoAnalise;
import br.com.fincontrol.domain.model.TipoAnalise;
import br.com.fincontrol.domain.model.TipoConta;
import br.com.fincontrol.domain.service.IaDesabilitadaException;
import br.com.fincontrol.domain.service.IaIndisponivelException;
import br.com.fincontrol.domain.service.IaRespostaInvalidaException;

class AnaliseIaAdapterTest {

    private static final PedidoAnalise PEDIDO =
            new PedidoAnalise(YearMonth.of(2026, 11), TipoAnalise.COMPORTAMENTAL, "onde cortar?");
    private static final AgregadoMes AGREGADO = new AgregadoMes(
            YearMonth.of(2026, 11),
            new BigDecimal("4800.00"),
            new BigDecimal("500.00"),
            3,
            List.of(new AgregadoMes.LinhaCategoria("Alimentação", new BigDecimal("320.00"))),
            List.of(new AgregadoMes.LinhaConta("Flash", TipoConta.BENEFICIO, new BigDecimal("-390.00"))));
    private static final String VALIDO =
            "{\"resumo\":\"Gastos controlados\",\"insights\":[\"Alimentação concentra o gasto\"],\"acoes\":[\"Revisar mercado\"]}";

    @Test
    void interpretaRespostaValida() {
        ChatModel modelo = modelo(VALIDO);
        AnaliseIaAdapter adaptador = new AnaliseIaAdapter(modelo, "ollama", "qwen3.5:9b");

        AnaliseFinanceira analise = adaptador.analisar(PEDIDO, AGREGADO);

        assertThat(analise.resumo()).isEqualTo("Gastos controlados");
        assertThat(analise.insights()).containsExactly("Alimentação concentra o gasto");
        assertThat(analise.acoes()).containsExactly("Revisar mercado");
        assertThat(adaptador.provedor()).isEqualTo("ollama");
        assertThat(adaptador.modelo()).isEqualTo("qwen3.5:9b");
    }

    @Test
    void aceitaJsonEntreCercas() {
        ChatModel modelo = modelo("```json\n" + VALIDO + "\n```");
        AnaliseIaAdapter adaptador = new AnaliseIaAdapter(modelo, "openai", "gpt-4.1-mini");

        assertThat(adaptador.analisar(PEDIDO, AGREGADO).resumo()).isEqualTo("Gastos controlados");
    }

    @Test
    void repeteUmaVezQuandoSchemaInvalido() {
        ChatModel modelo = mock(ChatModel.class);
        when(modelo.getOptions()).thenReturn(ChatOptions.builder().build());
        when(modelo.call(any(Prompt.class)))
                .thenReturn(resposta("nao e json"))
                .thenReturn(resposta(VALIDO));
        AnaliseIaAdapter adaptador = new AnaliseIaAdapter(modelo, "anthropic", "claude-sonnet-4-5");

        assertThat(adaptador.analisar(PEDIDO, AGREGADO).resumo()).isEqualTo("Gastos controlados");
        verify(modelo, times(2)).call(any(Prompt.class));
    }

    @Test
    void falhaQuandoSchemaPersisteInvalido() {
        ChatModel modelo = modelo("{\"resumo\":\"\"}");
        AnaliseIaAdapter adaptador = new AnaliseIaAdapter(modelo, "ollama", "qwen3.5:9b");

        assertThatThrownBy(() -> adaptador.analisar(PEDIDO, AGREGADO))
                .isInstanceOf(IaRespostaInvalidaException.class);
    }

    @Test
    void falhaQuandoProvedorFora() {
        ChatModel modelo = mock(ChatModel.class);
        when(modelo.getOptions()).thenReturn(ChatOptions.builder().build());
        when(modelo.call(any(Prompt.class))).thenThrow(new IllegalStateException("connection refused"));
        AnaliseIaAdapter adaptador = new AnaliseIaAdapter(modelo, "ollama", "qwen3.5:9b");

        assertThatThrownBy(() -> adaptador.analisar(PEDIDO, AGREGADO)).isInstanceOf(IaIndisponivelException.class);
    }

    @Test
    void promptNaoLevaIdentificadorNemDocumento() {
        ChatModel modelo = modelo(VALIDO);
        AnaliseIaAdapter adaptador = new AnaliseIaAdapter(modelo, "ollama", "qwen3.5:9b");

        adaptador.analisar(PEDIDO, AGREGADO);

        ArgumentCaptor<Prompt> capturado = ArgumentCaptor.forClass(Prompt.class);
        verify(modelo).call(capturado.capture());
        String texto = capturado.getValue().getContents();
        assertThat(texto).doesNotContain("ACCTID").doesNotContain("7224833363").doesNotContain("CPF")
                .doesNotContain("FITID").doesNotContain("storage_key").doesNotContain("recibo.png");
        assertThat(texto).contains("COMPORTAMENTAL").contains("2026-11").contains("onde cortar?")
                .contains("Alimentação");
    }

    @Test
    void desabilitadoSempreResponde503() {
        AnaliseDesabilitadaAdapter adaptador = new AnaliseDesabilitadaAdapter();

        assertThatThrownBy(() -> adaptador.analisar(PEDIDO, AGREGADO)).isInstanceOf(IaDesabilitadaException.class);
        assertThat(adaptador.provedor()).isEqualTo("none");
        assertThat(adaptador.modelo()).isEqualTo("none");
    }

    private static ChatModel modelo(String conteudo) {
        ChatModel modelo = mock(ChatModel.class);
        when(modelo.getOptions()).thenReturn(ChatOptions.builder().build());
        when(modelo.call(any(Prompt.class))).thenReturn(resposta(conteudo));
        return modelo;
    }

    private static ChatResponse resposta(String conteudo) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(conteudo))));
    }
}
