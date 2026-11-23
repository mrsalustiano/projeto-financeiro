package br.com.fincontrol.infrastructure.out.external;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.fincontrol.domain.model.AgregadoMes;
import br.com.fincontrol.domain.model.AnaliseFinanceira;
import br.com.fincontrol.domain.model.PedidoAnalise;
import br.com.fincontrol.domain.service.AnaliseFinanceiraPort;
import br.com.fincontrol.domain.service.IaIndisponivelException;
import br.com.fincontrol.domain.service.IaRespostaInvalidaException;

public class AnaliseIaAdapter implements AnaliseFinanceiraPort {

    private static final String RECURSO_PROMPT = "/prompts/analise-financeira.st";
    private static final String REFORCO = """

            A resposta anterior não era JSON válido. Responda apenas com o objeto JSON pedido.
            """;

    private final ChatClient chatClient;
    private final String provedor;
    private final String modelo;
    private final String prompt;
    private final ObjectMapper json;

    public AnaliseIaAdapter(ChatModel chatModel, String provedor, String modelo) {
        this.chatClient = ChatClient.create(chatModel);
        this.provedor = provedor;
        this.modelo = modelo;
        this.prompt = carregar();
        this.json = new ObjectMapper().findAndRegisterModules();
    }

    @Override
    public AnaliseFinanceira analisar(PedidoAnalise pedido, AgregadoMes agregado) {
        String mensagem = montar(pedido, agregado);
        String primeira = chamar(mensagem);
        try {
            return interpretar(primeira);
        } catch (IaRespostaInvalidaException ex) {
            return interpretar(chamar(mensagem + REFORCO));
        }
    }

    @Override
    public String provedor() {
        return provedor;
    }

    @Override
    public String modelo() {
        return modelo;
    }

    private String chamar(String mensagem) {
        try {
            String texto = chatClient.prompt().user(mensagem).call().content();
            if (texto == null || texto.isBlank()) {
                throw new IaRespostaInvalidaException();
            }
            return texto;
        } catch (IaRespostaInvalidaException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new IaIndisponivelException(ex);
        }
    }

    private AnaliseFinanceira interpretar(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IaRespostaInvalidaException();
        }
        try {
            Resposta resposta = json.readValue(recortar(texto), Resposta.class);
            return new AnaliseFinanceira(resposta.resumo(), resposta.insights(), resposta.acoes());
        } catch (IOException | IllegalArgumentException ex) {
            throw new IaRespostaInvalidaException(ex);
        }
    }

    private static String recortar(String texto) {
        int inicio = texto.indexOf('{');
        int fim = texto.lastIndexOf('}');
        if (inicio < 0 || fim <= inicio) {
            throw new IaRespostaInvalidaException();
        }
        return texto.substring(inicio, fim + 1);
    }

    private String montar(PedidoAnalise pedido, AgregadoMes agregado) {
        return prompt
                .replace("{tipo}", pedido.tipo().name())
                .replace("{mes}", pedido.mes().toString())
                .replace("{pergunta}", pedido.pergunta() == null ? "nenhuma" : pedido.pergunta())
                .replace("{agregado}", serializar(agregado));
    }

    private String serializar(AgregadoMes agregado) {
        try {
            return json.writeValueAsString(agregado);
        } catch (IOException ex) {
            throw new IaRespostaInvalidaException(ex);
        }
    }

    private static String carregar() {
        try (InputStream entrada = AnaliseIaAdapter.class.getResourceAsStream(RECURSO_PROMPT)) {
            if (entrada == null) {
                throw new IllegalStateException("Prompt ausente: " + RECURSO_PROMPT);
            }
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    record Resposta(String resumo, List<String> insights, List<String> acoes) {
    }
}
