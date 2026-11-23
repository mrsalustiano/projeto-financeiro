package br.com.fincontrol.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import br.com.fincontrol.domain.model.Categoria;
import br.com.fincontrol.domain.model.Conta;
import br.com.fincontrol.domain.model.Dinheiro;
import br.com.fincontrol.domain.model.Natureza;
import br.com.fincontrol.domain.model.Papel;
import br.com.fincontrol.domain.model.Saldo;
import br.com.fincontrol.domain.model.TipoConta;
import br.com.fincontrol.domain.service.AcessoNegadoException;
import br.com.fincontrol.domain.service.CategoriaRepositorio;
import br.com.fincontrol.domain.service.ContaRepositorio;
import br.com.fincontrol.domain.service.LimiteInvalidoException;
import br.com.fincontrol.domain.service.NaoEncontradoException;
import br.com.fincontrol.domain.service.NomeEmUsoException;

@ExtendWith(MockitoExtension.class)
class ContasUseCasesTest {

    private static final Instant AGORA = Instant.parse("2026-11-22T03:00:00Z");
    private static final Acesso CASA = new Acesso("7", Papel.ADMIN, 42L, "jti", AGORA);

    @Mock private ContaRepositorio contas;
    @Mock private CategoriaRepositorio categorias;

    @Test
    void flashNaoAlteraNubankEOutroClienteSome() {
        when(contas.buscar(42L, 8L)).thenReturn(Optional.of(flash()));
        ContasEscrita escrita = new ContasEscrita(contas);

        Conta atualizada = escrita.atualizar(CASA, 8L, "Flash", "Flash", TipoConta.BENEFICIO, new Saldo(new BigDecimal("10")), LocalDate.of(2026, 11, 22), null);

        assertThat(atualizada.id()).isEqualTo(8L);
        verify(contas).atualizar(any());
        verify(contas, never()).atualizar(org.mockito.ArgumentMatchers.argThat(conta -> conta.id() == 7L));
        when(contas.buscar(42L, 99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> new ContasConsulta(contas).obter(CASA, 99L)).isInstanceOf(NaoEncontradoException.class);
    }

    @Test
    void leitorNaoCriaELimiteDeBeneficioFalha() {
        assertThatThrownBy(() -> new ContasEscrita(contas).criar(
                new Acesso("3", Papel.LEITOR, 42L, "jti", AGORA),
                "Extra", "Flash", TipoConta.BENEFICIO, Saldo.zero(), LocalDate.of(2026, 11, 22), null))
                .isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> new ContasEscrita(contas).criar(
                CASA, "Extra", "Flash", TipoConta.BENEFICIO, Saldo.zero(), LocalDate.of(2026, 11, 22), new Dinheiro(new BigDecimal("1"))))
                .isInstanceOf(LimiteInvalidoException.class);
        when(contas.existeNome(42L, "Nubank")).thenReturn(true);
        assertThatThrownBy(() -> new ContasEscrita(contas).criar(
                CASA, "Nubank", "Nubank", TipoConta.CORRENTE, Saldo.zero(), LocalDate.of(2026, 11, 22), null))
                .isInstanceOf(NomeEmUsoException.class);
    }

    @Test
    void categoriaPadraoENomeRepetido() {
        when(categorias.existeNome(eq(42L), any())).thenReturn(false);
        new GarantirCategoriasPadrao(categorias).executar(42L);
        verify(categorias).inserir(42L, "Benefícios", Natureza.RECEITA);
        when(categorias.existeNome(42L, "Salário")).thenReturn(true);
        assertThatThrownBy(() -> new CategoriasEscrita(categorias).criar(CASA, " Salário ", Natureza.RECEITA))
                .isInstanceOf(NomeEmUsoException.class);
        when(categorias.buscar(42L, 1L)).thenReturn(Optional.of(new Categoria(1L, 42L, "Salário", Natureza.RECEITA)));
        Categoria atual = new CategoriasEscrita(categorias).atualizar(CASA, 1L, "Salário", Natureza.DESPESA);
        assertThat(atual.natureza()).isEqualTo(Natureza.DESPESA);
        when(categorias.excluir(42L, 9L)).thenReturn(false);
        assertThatThrownBy(() -> new CategoriasEscrita(categorias).excluir(CASA, 9L)).isInstanceOf(NaoEncontradoException.class);
    }

    @Test
    void valoresTexto() {
        assertThat(ValoresTexto.texto(Saldo.zero())).isEqualTo("0.00");
        assertThat(ValoresTexto.texto(new Dinheiro(new BigDecimal("10")))).isEqualTo("10.00");
        assertThat(ValoresTexto.texto((Dinheiro) null)).isNull();
        assertThat(ValoresTexto.limite(" ")).isNull();
        assertThat(ValoresTexto.limite("12.50").valor()).isEqualByComparingTo("12.50");
        assertThat(ValoresTexto.saldo("1").valor()).isEqualByComparingTo("1.00");
        assertThatThrownBy(() -> ValoresTexto.limite("0")).isInstanceOf(LimiteInvalidoException.class);
    }

    @Test
    void criaContaECategoriaENaoDuplicaPadrao() {
        when(contas.existeNome(42L, "Reserva")).thenReturn(false);
        when(contas.inserir(any())).thenReturn(15L);
        Conta criada = new ContasEscrita(contas).criar(
                CASA, "Reserva", "Nubank", TipoConta.POUPANCA, Saldo.zero(), LocalDate.of(2026, 11, 22), null);
        assertThat(criada.id()).isEqualTo(15L);
        when(contas.excluir(42L, 15L)).thenReturn(true);
        when(contas.buscar(42L, 15L)).thenReturn(Optional.of(criada));
        new ContasEscrita(contas).excluir(CASA, 15L);
        when(contas.listar(42L)).thenReturn(List.of(criada));
        assertThat(new ContasConsulta(contas).listar(CASA)).hasSize(1);

        when(categorias.existeNome(42L, "Lazer")).thenReturn(false);
        when(categorias.inserir(42L, "Lazer", Natureza.DESPESA)).thenReturn(3L);
        assertThat(new CategoriasEscrita(categorias).criar(CASA, " Lazer ", Natureza.DESPESA).id()).isEqualTo(3L);
        when(categorias.buscar(42L, 3L)).thenReturn(Optional.of(new Categoria(3L, 42L, "Lazer", Natureza.DESPESA)));
        when(categorias.existeNome(42L, "Viagem")).thenReturn(false);
        assertThat(new CategoriasEscrita(categorias).atualizar(CASA, 3L, "Viagem", Natureza.DESPESA).nome()).isEqualTo("Viagem");
        when(categorias.excluir(42L, 3L)).thenReturn(true);
        new CategoriasEscrita(categorias).excluir(CASA, 3L);
        assertThat(new CategoriasConsulta(categorias).listar(CASA)).isEmpty();

        when(contas.existeNome(42L, "Flash")).thenReturn(true);
        new GarantirContas(contas).executar(42L, List.of(
                new ContaSeed("Flash", "Flash", TipoConta.BENEFICIO, Saldo.zero(), LocalDate.of(2026, 1, 1))));
        verify(contas, never()).inserir(org.mockito.ArgumentMatchers.argThat(conta -> conta.nome().equals("Flash") && conta.id() == 0L));
        when(contas.existeNome(42L, "Poupanca")).thenReturn(false);
        new GarantirContas(contas).executar(42L, List.of(
                new ContaSeed("Poupanca", "Nubank", TipoConta.POUPANCA, Saldo.zero(), LocalDate.of(2026, 1, 1))));
        verify(contas).inserir(org.mockito.ArgumentMatchers.argThat(conta -> conta.nome().equals("Poupanca")));
        new PessoaSeed("A", "a@fincontrol.local", "Casa", Papel.ADMIN, true, true, null, null);
    }

    private static Conta flash() {
        return new Conta(8L, 42L, "Flash", "Flash", TipoConta.BENEFICIO, Saldo.zero(), LocalDate.of(2026, 1, 1), null);
    }
}
