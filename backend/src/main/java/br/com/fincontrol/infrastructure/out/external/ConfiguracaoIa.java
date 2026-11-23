package br.com.fincontrol.infrastructure.out.external;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import br.com.fincontrol.domain.service.AnaliseFinanceiraPort;

@Configuration
public class ConfiguracaoIa {

    private static final String PROPRIEDADE_PROVEDOR = "spring.ai.model.chat";

    @Bean
    AnaliseFinanceiraPort analiseFinanceiraPort(ObjectProvider<ChatModel> modelos, Environment ambiente) {
        ChatModel modelo = modelos.getIfAvailable();
        String provedor = ambiente.getProperty(PROPRIEDADE_PROVEDOR, "none").trim().toLowerCase(java.util.Locale.ROOT);
        if (modelo == null || "none".equals(provedor)) {
            return new AnaliseDesabilitadaAdapter();
        }
        return new AnaliseIaAdapter(modelo, provedor, nomeDoModelo(ambiente, provedor));
    }

    private static String nomeDoModelo(Environment ambiente, String provedor) {
        return ambiente.getProperty("spring.ai." + provedor + ".chat.model", provedor);
    }
}
