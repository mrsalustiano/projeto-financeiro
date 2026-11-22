package br.com.fincontrol.infrastructure.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

class ApiExceptionHandlerTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new ControladorDeTeste())
                .setControllerAdvice(new ApiExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void validacaoDevolve400() throws Exception {
        mvc.perform(post("/teste").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigoErro").value("VALIDACAO"))
                .andExpect(jsonPath("$.erros[0].campo").value("nome"));
    }

    @Test
    void mensagemEmBrancoViraTextoPadrao() throws Exception {
        Method method = ControladorDeTeste.class.getDeclaredMethod("criar", Pedido.class);
        BeanPropertyBindingResult result = new BeanPropertyBindingResult(new Pedido(" "), "pedido");
        result.addError(new FieldError("pedido", "nome", " "));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(new MethodParameter(method, 0), result);

        ProblemDetail problema = new ApiExceptionHandler().validacao(ex, new MockHttpServletRequest("POST", "/teste"));

        @SuppressWarnings("unchecked")
        List<Map<String, String>> erros = (List<Map<String, String>>) problema.getProperties().get("erros");
        assertThat(erros.getFirst().get("mensagem")).isEqualTo("Valor inválido.");
    }

    @Test
    void erroInternoNaoVazaDetalhe() throws Exception {
        mvc.perform(get("/teste/falha"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigoErro").value("ERRO_INTERNO"))
                .andExpect(jsonPath("$.detail").value("Erro interno. Tente novamente ou contate o suporte."));
    }

    @RestController
    static class ControladorDeTeste {

        @PostMapping("/teste")
        void criar(@Valid @RequestBody Pedido pedido) {
        }

        @GetMapping("/teste/falha")
        void falha() {
            throw new IllegalStateException("segredo interno");
        }
    }

    record Pedido(@NotBlank String nome) {
    }
}
