package br.com.fincontrol.infrastructure.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import br.com.fincontrol.application.CategoriasConsulta;
import br.com.fincontrol.application.CategoriasEscrita;
import br.com.fincontrol.application.ContasConsulta;
import br.com.fincontrol.application.ContasEscrita;
import br.com.fincontrol.domain.model.Acesso;
import br.com.fincontrol.domain.model.Categoria;
import br.com.fincontrol.domain.model.Conta;
import br.com.fincontrol.domain.model.Natureza;
import br.com.fincontrol.domain.model.Papel;
import br.com.fincontrol.domain.model.Saldo;
import br.com.fincontrol.domain.model.TipoConta;
import br.com.fincontrol.domain.service.AcessoNegadoException;
import br.com.fincontrol.domain.service.LimiteInvalidoException;
import br.com.fincontrol.domain.service.NaoEncontradoException;
import br.com.fincontrol.domain.service.NomeEmUsoException;

@ExtendWith(MockitoExtension.class)
class CatalogoWebTest {

    @Mock private CategoriasConsulta categoriasConsulta;
    @Mock private CategoriasEscrita categoriasEscrita;
    @Mock private ContasConsulta contasConsulta;
    @Mock private ContasEscrita contasEscrita;

    private MockMvc categorias;
    private MockMvc contas;

    @BeforeEach
    void setUp() {
        Acesso acesso = new Acesso("7", Papel.ADMIN, 42L, "jti", Instant.parse("2026-11-22T03:00:00Z"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(acesso, null));
        categorias = MockMvcBuilders.standaloneSetup(new CategoriaController(categoriasConsulta, categoriasEscrita))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
        contas = MockMvcBuilders.standaloneSetup(new ContaController(contasConsulta, contasEscrita))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void categoriasCobreSucessoEErros() throws Exception {
        when(categoriasConsulta.listar(any())).thenReturn(List.of(new Categoria(1L, 42L, "Salário", Natureza.RECEITA)));
        when(categoriasEscrita.criar(any(), eq("Lazer"), eq(Natureza.DESPESA)))
                .thenReturn(new Categoria(2L, 42L, "Lazer", Natureza.DESPESA));
        when(categoriasEscrita.atualizar(any(), eq(1L), eq("Salário"), eq(Natureza.RECEITA)))
                .thenReturn(new Categoria(1L, 42L, "Salário", Natureza.RECEITA));
        doThrow(new NaoEncontradoException()).when(categoriasEscrita).excluir(any(), eq(9L));
        when(categoriasEscrita.criar(any(), eq("Salário"), eq(Natureza.RECEITA))).thenThrow(new NomeEmUsoException());

        categorias.perform(get("/api/v1/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Salário"));
        categorias.perform(post("/api/v1/categorias").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Lazer\",\"natureza\":\"DESPESA\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("2"));
        categorias.perform(put("/api/v1/categorias/1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Salário\",\"natureza\":\"RECEITA\"}"))
                .andExpect(status().isOk());
        categorias.perform(delete("/api/v1/categorias/abc")).andExpect(status().isNotFound());
        categorias.perform(delete("/api/v1/categorias/9")).andExpect(status().isNotFound());
        categorias.perform(post("/api/v1/categorias").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Salário\",\"natureza\":\"RECEITA\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigoErro").value("NOME_EM_USO"));
    }

    @Test
    void contasCobreFlashELimite() throws Exception {
        Conta flash = new Conta(8L, 42L, "Flash", "Flash", TipoConta.BENEFICIO, Saldo.zero(), LocalDate.of(2026, 1, 1), null);
        when(contasConsulta.listar(any())).thenReturn(List.of(flash));
        when(contasConsulta.obter(any(), eq(8L))).thenReturn(flash);
        when(contasEscrita.criar(any(), eq("Flash"), eq("Flash"), eq(TipoConta.BENEFICIO), any(), any(), eq(null)))
                .thenReturn(flash);
        when(contasEscrita.atualizar(any(), eq(8L), eq("Flash"), eq("Flash"), eq(TipoConta.BENEFICIO), any(), any(), eq(null)))
                .thenReturn(flash);
        when(contasEscrita.criar(any(), eq("Cartao"), eq("Nubank"), eq(TipoConta.CARTAO_CREDITO), any(), any(), eq(null)))
                .thenThrow(new LimiteInvalidoException());
        doThrow(new AcessoNegadoException()).when(contasEscrita).excluir(any(), eq(7L));

        contas.perform(get("/api/v1/contas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("BENEFICIO"))
                .andExpect(jsonPath("$[0].limite").doesNotExist());
        contas.perform(get("/api/v1/contas/8")).andExpect(status().isOk()).andExpect(jsonPath("$.banco").value("Flash"));
        String corpo = """
                {"nome":"Flash","banco":"Flash","tipo":"BENEFICIO","saldoInicial":"0","dataSaldo":"2026-01-01"}
                """;
        contas.perform(post("/api/v1/contas").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Flash"));
        contas.perform(put("/api/v1/contas/8").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk());
        contas.perform(delete("/api/v1/contas/7")).andExpect(status().isForbidden());
        String cartao = """
                {"nome":"Cartao","banco":"Nubank","tipo":"CARTAO_CREDITO","saldoInicial":"0","dataSaldo":"2026-01-01"}
                """;
        contas.perform(post("/api/v1/contas").contentType(MediaType.APPLICATION_JSON).content(cartao))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigoErro").value("LIMITE_INVALIDO"));
    }

    @Test
    void semSessaoNega() throws Exception {
        SecurityContextHolder.clearContext();
        categorias.perform(get("/api/v1/categorias")).andExpect(status().isUnauthorized());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("texto", null));
        categorias.perform(get("/api/v1/categorias")).andExpect(status().isUnauthorized());
    }
}
