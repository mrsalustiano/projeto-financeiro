package br.com.fincontrol.infrastructure.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;

import br.com.fincontrol.application.GarantirRegrasClassificacao;
import br.com.fincontrol.application.PessoaSeed;
import br.com.fincontrol.application.RegraClassificacaoSeed;
import br.com.fincontrol.application.SeedConfig;
import br.com.fincontrol.domain.model.RegraClassificacao;
import br.com.fincontrol.domain.service.RegraClassificacaoRepositorio;

@ExtendWith(MockitoExtension.class)
class SeedRegrasTest {

    @Mock private RegraClassificacaoRepositorio regras;

    @Test
    void regraDeSalarioSoNoClienteDoUsuario() {
        SeedConfig config = new ConfiguracaoSeed().seedConfig(new MockEnvironment());
        PessoaSeed usuario = config.pessoas().getFirst();
        PessoaSeed mae = config.pessoas().stream()
                .filter(pessoa -> "Familia".equals(pessoa.nomeCliente()))
                .findFirst()
                .orElseThrow();

        assertThat(usuario.regras()).hasSize(2);
        assertThat(usuario.regras()).extracting(RegraClassificacaoSeed::categoria).containsOnly("Salário");
        assertThat(usuario.regras()).extracting(RegraClassificacaoSeed::termo).containsOnly("ITAU");
        assertThat(usuario.regras()).extracting(RegraClassificacaoSeed::diaInicio).containsExactly(15, 28);
        assertThat(mae.regras()).isEmpty();
    }

    @Test
    void garanteRegraSemDuplicar() {
        when(regras.existe(eq(42L), anyString(), anyInt(), anyInt())).thenReturn(false, true);
        when(regras.inserir(any())).thenReturn(1L);
        GarantirRegrasClassificacao garantir = new GarantirRegrasClassificacao(regras);

        garantir.executar(42L, List.of(
                new RegraClassificacaoSeed("ITAU", 15, 15, new BigDecimal("4800.00"),
                        new BigDecimal("4800.00"), "Salário"),
                new RegraClassificacaoSeed("ITAU", 28, 2, new BigDecimal("3750.00"),
                        new BigDecimal("3850.00"), "Salário")));

        verify(regras, org.mockito.Mockito.times(1)).inserir(any());
    }

    @Test
    void semRegraPrevistaNaoEscreve() {
        new GarantirRegrasClassificacao(regras).executar(42L, List.of());

        verify(regras, never()).inserir(any());
        verify(regras, never()).existe(anyLong(), anyString(), anyInt(), anyInt());
    }

    @Test
    void regraInvalidaNaoEntra() {
        when(regras.existe(anyLong(), anyString(), anyInt(), anyInt())).thenReturn(false);
        GarantirRegrasClassificacao garantir = new GarantirRegrasClassificacao(regras);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> garantir.executar(42L, List.of(
                new RegraClassificacaoSeed(" ", 15, 15, BigDecimal.ONE, BigDecimal.TEN, "Salário"))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void regraCarregaCampos() {
        RegraClassificacao regra = new RegraClassificacao(1L, 42L, "ITAU", 15, 15,
                new BigDecimal("4800.00"), new BigDecimal("4800.00"), "Salário");

        assertThat(regra.clienteId()).isEqualTo(42L);
        assertThat(regra.diaFim()).isEqualTo(15);
        assertThat(regra.valorMaximo()).isEqualByComparingTo("4800.00");
    }
}
