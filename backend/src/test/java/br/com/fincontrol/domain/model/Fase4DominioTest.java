package br.com.fincontrol.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;

import org.junit.jupiter.api.Test;

class Fase4DominioTest {

    private static final Instant AGORA = Instant.parse("2026-11-22T03:00:00Z");

    @Test
    void diaDoMesRespeitaMesCurto() {
        assertThat(new DiaDoMes(31).venceEm(YearMonth.of(2026, 2))).isEqualTo(LocalDate.of(2026, 2, 28));
        assertThat(new DiaDoMes(31).venceEm(YearMonth.of(2026, 1))).isEqualTo(LocalDate.of(2026, 1, 31));
        assertThat(DiaDoMes.ultimo().venceEm(YearMonth.of(2026, 2))).isEqualTo(LocalDate.of(2026, 2, 28));
        assertThat(new DiaDoMes(15).venceEm(YearMonth.of(2026, 11))).isEqualTo(LocalDate.of(2026, 11, 15));
        assertThatThrownBy(() -> new DiaDoMes(32)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DiaDoMes(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void orcamentoAlertaEmOitentaPorCento() {
        Orcamento orcamento = new Orcamento(1L, 42L, 2L, 2026, 11, new Dinheiro(new BigDecimal("1000.00")));

        AlertaOrcamento vazio = orcamento.alertar(null);
        assertThat(vazio.percentual()).isEqualByComparingTo("0.00");
        assertThat(vazio.alerta()).isFalse();
        assertThat(vazio.estourou()).isFalse();

        AlertaOrcamento limite = orcamento.alertar(new BigDecimal("800.00"));
        assertThat(limite.percentual()).isEqualByComparingTo("80.00");
        assertThat(limite.alerta()).isTrue();
        assertThat(limite.estourou()).isFalse();

        AlertaOrcamento estourado = orcamento.alertar(new BigDecimal("1000.01"));
        assertThat(estourado.alerta()).isTrue();
        assertThat(estourado.estourou()).isTrue();

        assertThatThrownBy(() -> new Orcamento(1L, 42L, 2L, 1999, 11, new Dinheiro(BigDecimal.ONE)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Orcamento(1L, 42L, 2L, 2026, 13, new Dinheiro(BigDecimal.ONE)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Orcamento(1L, 42L, 2L, 2026, 11, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AlertaOrcamento(null, BigDecimal.ZERO, BigDecimal.ZERO, false, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void metaCalculaProgresso() {
        Meta meta = new Meta(1L, 42L, " Viagem ", new Dinheiro(new BigDecimal("1000.00")), null, AGORA);

        assertThat(meta.nome()).isEqualTo("Viagem");
        assertThat(meta.progresso(null).percentual()).isEqualByComparingTo("0.00");
        assertThat(meta.progresso(new BigDecimal("500.00")).alcancada()).isFalse();
        ProgressoMeta cheia = meta.progresso(new BigDecimal("1000.00"));
        assertThat(cheia.percentual()).isEqualByComparingTo("100.00");
        assertThat(cheia.alcancada()).isTrue();

        assertThatThrownBy(() -> new Meta(1L, 42L, " ", new Dinheiro(BigDecimal.ONE), null, AGORA))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Meta(1L, 42L, "x".repeat(81), new Dinheiro(BigDecimal.ONE), null, AGORA))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Meta(1L, 42L, "x", null, null, AGORA))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Meta(1L, 42L, "x", new Dinheiro(BigDecimal.ONE), null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Aporte(1L, 42L, 1L, null, LocalDate.now()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Aporte(1L, 42L, 1L, new Dinheiro(BigDecimal.ONE), null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProgressoMeta(null, BigDecimal.ZERO, BigDecimal.ZERO, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void recorrenciaPausaERetoma() {
        Recorrencia ativa = new Recorrencia(1L, 42L, 1L, 2L, Natureza.RECEITA, null,
                new Dinheiro(new BigDecimal("1570.00")), "Benefício Flash", DiaDoMes.ultimo(), true, null, AGORA);

        Recorrencia pausada = ativa.pausar(AGORA);
        assertThat(pausada.ativa()).isFalse();
        assertThat(pausada.pausadaEm()).isEqualTo(AGORA);
        assertThat(pausada.retomar().ativa()).isTrue();
        assertThatThrownBy(() -> pausada.pausar(AGORA)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ativa.retomar()).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new Recorrencia(1L, 42L, 1L, 2L, Natureza.DESPESA, null,
                new Dinheiro(BigDecimal.ONE), "x", DiaDoMes.ultimo(), true, null, AGORA))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Recorrencia(1L, 42L, 1L, 2L, Natureza.RECEITA, null,
                new Dinheiro(BigDecimal.ONE), " ", DiaDoMes.ultimo(), true, null, AGORA))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Recorrencia(1L, 42L, 1L, 2L, Natureza.RECEITA, null,
                new Dinheiro(BigDecimal.ONE), "x", DiaDoMes.ultimo(), true, AGORA, AGORA))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void transferenciaRecusaMesmaConta() {
        Transferencia transferencia = new Transferencia(1L, 42L, 1L, 2L, new Dinheiro(new BigDecimal("10")),
                LocalDate.of(2026, 11, 10), "  ");

        assertThat(transferencia.descricao()).isNull();
        assertThatThrownBy(() -> new Transferencia(1L, 42L, 1L, 1L, new Dinheiro(BigDecimal.ONE),
                LocalDate.now(), null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Transferencia(1L, 42L, 1L, 2L, null, LocalDate.now(), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void faixaDoBeneficio() {
        assertThat(FaixaBeneficio.FLASH.aceita(new Dinheiro(new BigDecimal("1520.00")))).isTrue();
        assertThat(FaixaBeneficio.FLASH.aceita(new Dinheiro(new BigDecimal("1620.00")))).isTrue();
        assertThat(FaixaBeneficio.FLASH.aceita(new Dinheiro(new BigDecimal("1519.99")))).isFalse();
        assertThat(FaixaBeneficio.FLASH.aceita(new Dinheiro(new BigDecimal("1620.01")))).isFalse();
        assertThat(FaixaBeneficio.FLASH.aceita(null)).isFalse();
        assertThatThrownBy(() -> new FaixaBeneficio(BigDecimal.TEN, BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
