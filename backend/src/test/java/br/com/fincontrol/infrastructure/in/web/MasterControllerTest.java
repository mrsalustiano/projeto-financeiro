package br.com.fincontrol.infrastructure.in.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import br.com.fincontrol.application.ClienteCriado;
import br.com.fincontrol.application.CriarCliente;
import br.com.fincontrol.domain.service.EmailEmUsoException;

@ExtendWith(MockitoExtension.class)
class MasterControllerTest {

    @Mock private CriarCliente criarCliente;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new MasterController(criarCliente))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void criaERecusaEmailRepetido() throws Exception {
        when(criarCliente.executar("Casa", "Ana", "ana@fincontrol.local", "segredo123"))
                .thenReturn(new ClienteCriado(9L, 11L))
                .thenThrow(new EmailEmUsoException());
        String corpo = """
                {"nomeCliente":"Casa","nomeAdministrador":"Ana","emailAdministrador":"ana@fincontrol.local","senhaAdministrador":"segredo123"}
                """;

        mvc.perform(post("/api/v1/master/clientes").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clienteId").value("9"))
                .andExpect(jsonPath("$.administradorId").value("11"))
                .andExpect(jsonPath("$.senha").doesNotExist());
        mvc.perform(post("/api/v1/master/clientes").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigoErro").value("EMAIL_EM_USO"));
    }
}
