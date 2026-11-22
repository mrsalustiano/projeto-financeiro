package br.com.fincontrol.infrastructure.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ContratosHttpTest {

    @Test
    void publicaOContratoDeLogin() throws Exception {
        try (AnnotationConfigWebApplicationContext contexto = new AnnotationConfigWebApplicationContext()) {
            contexto.setServletContext(new MockServletContext());
            contexto.register(WebDeTeste.class);
            contexto.refresh();
            MockMvc mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
            mvc.perform(get("/contratos/auth.yaml"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("operationId: login")));
        }
    }

    @Configuration
    @EnableWebMvc
    static class WebDeTeste extends ContratosHttp {
    }
}
