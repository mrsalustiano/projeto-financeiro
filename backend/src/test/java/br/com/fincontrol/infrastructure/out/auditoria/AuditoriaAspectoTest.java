package br.com.fincontrol.infrastructure.out.auditoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import br.com.fincontrol.application.Auditavel;
import br.com.fincontrol.application.ContextoAuditoria;
import br.com.fincontrol.application.RegistrarAuditoria;
import br.com.fincontrol.domain.model.AcaoAuditoria;
import br.com.fincontrol.domain.model.Acesso;
import br.com.fincontrol.domain.model.Dinheiro;
import br.com.fincontrol.domain.model.Lancamento;
import br.com.fincontrol.domain.model.Natureza;
import br.com.fincontrol.domain.model.Papel;
import br.com.fincontrol.domain.model.TipoDespesa;

@ExtendWith(MockitoExtension.class)
class AuditoriaAspectoTest {

    private static final Instant AGORA = Instant.parse("2026-11-23T12:00:00Z");

    @Mock private RegistrarAuditoria registrar;

    private Alvo alvo;

    @BeforeEach
    void setUp() {
        Acesso admin = new Acesso("7", Papel.ADMIN, 42L, "jti", AGORA);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(admin, null));
        AspectJProxyFactory fabrica = new AspectJProxyFactory(new Alvo());
        fabrica.addAspect(new AuditoriaAspecto(registrar));
        alvo = fabrica.getProxy();
    }

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
        ContextoAuditoria.limpar();
    }

    @Test
    void gravaCriacaoComObjeto() {
        alvo.criar();

        verify(registrar).registrar(any(), eq(AcaoAuditoria.CREATE), eq("lancamento"), eq(44L), isNull(),
                org.mockito.ArgumentMatchers.contains("\"descricao\":\"Compra\""));
    }

    @Test
    void gravaAlteracaoComDiff() {
        alvo.editar();

        ArgumentCaptor<String> depois = ArgumentCaptor.forClass(String.class);
        verify(registrar).registrar(any(), eq(AcaoAuditoria.UPDATE), eq("lancamento"), eq(44L), any(), depois.capture());
        assertThat(depois.getValue()).contains("\"valor\"").contains("antes").contains("depois");
        assertThat(depois.getValue()).doesNotContain("\"descricao\"");
    }

    @Test
    void gravaExclusaoComRemovido() {
        alvo.excluir();

        verify(registrar).registrar(any(), eq(AcaoAuditoria.DELETE), eq("lancamento"), eq(44L),
                org.mockito.ArgumentMatchers.contains("\"descricao\":\"Compra\""), isNull());
    }

    @Test
    void gravaListaComPrimeiroId() {
        alvo.criarLista();

        verify(registrar).registrar(any(), eq(AcaoAuditoria.CREATE), eq("lancamento"), eq(44L), isNull(), any());
    }

    @Test
    void falhaNaoGrava() {
        assertThatThrownBy(() -> alvo.falhar()).isInstanceOf(IllegalStateException.class);

        verify(registrar, never()).registrar(any(), any(), any(), any(), any(), any());
    }

    @Test
    void mascaraSenhaNoSnapshot() {
        alvo.criarComSenha();

        ArgumentCaptor<String> depois = ArgumentCaptor.forClass(String.class);
        verify(registrar).registrar(any(), eq(AcaoAuditoria.CREATE), eq("usuario"), eq(1L), isNull(), depois.capture());
        assertThat(depois.getValue()).contains("***").doesNotContain("segredo");
    }

    static class Alvo {

        @Auditavel(acao = AcaoAuditoria.CREATE, recurso = "lancamento")
        public Lancamento criar() {
            return lancamento("10.00");
        }

        @Auditavel(acao = AcaoAuditoria.UPDATE, recurso = "lancamento")
        public Lancamento editar() {
            ContextoAuditoria.antes(lancamento("10.00"));
            return lancamento("12.00");
        }

        @Auditavel(acao = AcaoAuditoria.DELETE, recurso = "lancamento")
        public void excluir() {
            ContextoAuditoria.antes(lancamento("10.00"));
        }

        @Auditavel(acao = AcaoAuditoria.CREATE, recurso = "lancamento")
        public List<Lancamento> criarLista() {
            return List.of(lancamento("10.00"), lancamento("11.00"));
        }

        @Auditavel(acao = AcaoAuditoria.CREATE, recurso = "lancamento")
        public String falhar() {
            throw new IllegalStateException("falhou");
        }

        @Auditavel(acao = AcaoAuditoria.CREATE, recurso = "usuario")
        public UsuarioComSenha criarComSenha() {
            return new UsuarioComSenha(1L, "Ana", "segredo");
        }
    }

    record UsuarioComSenha(long id, String nome, String senha) {
    }

    private static Lancamento lancamento(String valor) {
        return new Lancamento(44L, 42L, 10L, null, Natureza.DESPESA, TipoDespesa.VARIAVEL,
                new Dinheiro(new BigDecimal(valor)), LocalDate.of(2026, 11, 10), "Compra",
                null, null, null, null, null, null, null, null);
    }
}
