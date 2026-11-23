package br.com.fincontrol.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class AuditoriaTest {

    private static final Instant AGORA = Instant.parse("2026-11-23T12:00:00Z");

    @Test
    void mascaraCamposSensiveis() {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("email", "voce@fincontrol.local");
        dados.put("senha", "segredo");
        dados.put("senhaHash", "$argon2id$v=19$m=65536");
        dados.put("refreshToken", "abc");
        dados.put("authorization", "Bearer xyz");
        dados.put("perfil", Map.of("nome", "Ana", "token", "t1"));
        dados.put("itens", List.of(Map.of("senha", "s1", "nome", "n1")));

        Map<String, Object> mascarado = Mascaramento.mascarar(dados);

        assertThat(mascarado.get("email")).isEqualTo("voce@fincontrol.local");
        assertThat(mascarado.get("senha")).isEqualTo(Mascaramento.OCULTO);
        assertThat(mascarado.get("senhaHash")).isEqualTo(Mascaramento.OCULTO);
        assertThat(mascarado.get("refreshToken")).isEqualTo(Mascaramento.OCULTO);
        assertThat(mascarado.get("authorization")).isEqualTo(Mascaramento.OCULTO);
        assertThat(mascarado.get("perfil")).isEqualTo(Map.of("nome", "Ana", "token", Mascaramento.OCULTO));
        assertThat(mascarado.get("itens")).isEqualTo(List.of(Map.of("senha", Mascaramento.OCULTO, "nome", "n1")));
        assertThat(Mascaramento.mascarar(null)).isEmpty();
        assertThat(Mascaramento.sensivel(null)).isFalse();
        assertThat(Mascaramento.sensivel("valor")).isFalse();
    }

    @Test
    void calculaDiferenca() {
        Map<String, Object> antes = new LinkedHashMap<>();
        antes.put("descricao", "Compra");
        antes.put("valor", "10.00");
        Map<String, Object> depois = new LinkedHashMap<>();
        depois.put("descricao", "Compra");
        depois.put("valor", "12.00");
        depois.put("categoriaId", "4");

        Map<String, Object> diff = Diferenca.entre(antes, depois);

        assertThat(diff).containsOnlyKeys("valor", "categoriaId");
        assertThat(diff.get("valor")).isEqualTo(Map.of("antes", "10.00", "depois", "12.00"));
        assertThat(Diferenca.entre(null, depois)).isEqualTo(depois);
        assertThat(Diferenca.entre(antes, null)).isEqualTo(antes);
        assertThat(Diferenca.entre(null, null)).isEmpty();
        assertThat(Diferenca.entre(antes, antes)).isEmpty();
    }

    @Test
    void validaRegistro() {
        RegistroAuditoria registro = new RegistroAuditoria(1L, 42L, 3L, " voce@fincontrol.local ", " ADMIN ",
                AcaoAuditoria.CREATE, " lancamento ", 44L, null, "{}", AGORA);

        assertThat(registro.usuarioEmail()).isEqualTo("voce@fincontrol.local");
        assertThat(registro.usuarioPerfil()).isEqualTo("ADMIN");
        assertThat(registro.recurso()).isEqualTo("lancamento");
        assertThat(registro.deCliente(42L)).isTrue();
        assertThat(registro.deCliente(43L)).isFalse();
        assertThat(new RegistroAuditoria(1L, null, null, " ", null, AcaoAuditoria.DELETE, "cliente", null,
                null, null, AGORA).usuarioEmail()).isNull();
        assertThatThrownBy(() -> new RegistroAuditoria(1L, null, null, null, null, null, "x", null, null, null, AGORA))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RegistroAuditoria(1L, null, null, null, null, AcaoAuditoria.CREATE, " ", null,
                null, null, AGORA))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RegistroAuditoria(1L, null, null, null, null, AcaoAuditoria.CREATE, "x", null,
                null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validaFiltroEPagina() {
        assertThat(FiltroAuditoria.vazio().recurso()).isNull();
        assertThat(new FiltroAuditoria(null, "  ", null, null, null).recurso()).isNull();
        assertThat(new FiltroAuditoria(null, " lancamento ", null, null, null).recurso()).isEqualTo("lancamento");
        assertThatThrownBy(() -> new FiltroAuditoria(null, null, null, LocalDate.of(2026, 11, 10),
                LocalDate.of(2026, 11, 1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(PaginaAuditoria.vazia().itens()).isEmpty();
        assertThat(PaginaAuditoria.vazia().proximoCursor()).isNull();
        assertThat(new PaginaAuditoria(null, null).itens()).isEmpty();
    }
}
