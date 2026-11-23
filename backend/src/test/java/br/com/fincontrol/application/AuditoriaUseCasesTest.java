package br.com.fincontrol.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fincontrol.domain.model.AcaoAuditoria;
import br.com.fincontrol.domain.model.Acesso;
import br.com.fincontrol.domain.model.FiltroAuditoria;
import br.com.fincontrol.domain.model.PaginaAuditoria;
import br.com.fincontrol.domain.model.Papel;
import br.com.fincontrol.domain.model.RegistroAuditoria;
import br.com.fincontrol.domain.model.Usuario;
import br.com.fincontrol.domain.service.AcessoNegadoException;
import br.com.fincontrol.domain.service.AuditoriaRepositorio;
import br.com.fincontrol.domain.service.Relogio;
import br.com.fincontrol.domain.service.UsuarioRepositorio;

@ExtendWith(MockitoExtension.class)
class AuditoriaUseCasesTest {

    private static final Instant AGORA = Instant.parse("2026-11-23T12:00:00Z");
    private static final Acesso MASTER = new Acesso("1", Papel.MASTER, null, "jti", AGORA);
    private static final Acesso ADMIN = new Acesso("7", Papel.ADMIN, 42L, "jti", AGORA);

    @Mock private AuditoriaRepositorio repositorio;
    @Mock private UsuarioRepositorio usuarios;
    @Mock private Relogio relogio;

    @Test
    void masterConsultaComCursor() {
        RegistroAuditoria registro = registro(9L);
        when(repositorio.listar(any(), isNull(), eq(1))).thenReturn(List.of(registro));
        Auditorias casos = new Auditorias(repositorio);

        PaginaAuditoria pagina = casos.consultar(MASTER, FiltroAuditoria.vazio(), null, 1);

        assertThat(pagina.itens()).hasSize(1);
        assertThat(pagina.proximoCursor()).isEqualTo(9L);
    }

    @Test
    void paginaCheiaSemSobraNaoTemCursor() {
        when(repositorio.listar(any(), isNull(), eq(20))).thenReturn(List.of(registro(9L)));
        Auditorias casos = new Auditorias(repositorio);

        assertThat(casos.consultar(MASTER, FiltroAuditoria.vazio(), null, null).proximoCursor()).isNull();
    }

    @Test
    void ultimaPaginaSemCursor() {
        when(repositorio.listar(any(), eq(50L), eq(100))).thenReturn(List.of(registro(4L)));
        Auditorias casos = new Auditorias(repositorio);

        PaginaAuditoria pagina = casos.consultar(MASTER, FiltroAuditoria.vazio(), "50", 100);

        assertThat(pagina.proximoCursor()).isNull();
    }

    @Test
    void recusaQuemNaoEMaster() {
        Auditorias casos = new Auditorias(repositorio);

        assertThatThrownBy(() -> casos.consultar(ADMIN, FiltroAuditoria.vazio(), null, null))
                .isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> casos.consultar(null, FiltroAuditoria.vazio(), null, null))
                .isInstanceOf(AcessoNegadoException.class);
        verify(repositorio, never()).listar(any(), any(), anyInt());
    }

    @Test
    void recusaCursorELimiteInvalidos() {
        Auditorias casos = new Auditorias(repositorio);

        assertThatThrownBy(() -> casos.consultar(MASTER, FiltroAuditoria.vazio(), "abc", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> casos.consultar(MASTER, FiltroAuditoria.vazio(), "0", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> casos.consultar(MASTER, FiltroAuditoria.vazio(), null, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> casos.consultar(MASTER, FiltroAuditoria.vazio(), null, 101))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void registraComSnapshotDeEmailEPerfil() {
        when(usuarios.buscarPorId(7L)).thenReturn(Optional.of(new Usuario(7L, 42L, "Voce", "voce@fincontrol.local",
                "hash", Papel.ADMIN, true, 0, null)));
        when(relogio.agora()).thenReturn(AGORA);
        when(repositorio.inserir(any())).thenReturn(11L);
        RegistrarAuditoria casos = new RegistrarAuditoria(repositorio, usuarios, relogio);

        long id = casos.registrar(ADMIN, AcaoAuditoria.CREATE, "lancamento", 44L, null, "{\"id\":44}");

        ArgumentCaptor<RegistroAuditoria> capturado = ArgumentCaptor.forClass(RegistroAuditoria.class);
        verify(repositorio).inserir(capturado.capture());
        assertThat(id).isEqualTo(11L);
        assertThat(capturado.getValue().clienteId()).isEqualTo(42L);
        assertThat(capturado.getValue().usuarioId()).isEqualTo(7L);
        assertThat(capturado.getValue().usuarioEmail()).isEqualTo("voce@fincontrol.local");
        assertThat(capturado.getValue().usuarioPerfil()).isEqualTo("ADMIN");
        assertThat(capturado.getValue().criadoEm()).isEqualTo(AGORA);
    }

    @Test
    void registraSemSessao() {
        when(relogio.agora()).thenReturn(AGORA);
        when(repositorio.inserir(any())).thenReturn(12L);
        RegistrarAuditoria casos = new RegistrarAuditoria(repositorio, usuarios, relogio);

        casos.registrar(null, AcaoAuditoria.CREATE, "cliente", 9L, null, "{}");

        ArgumentCaptor<RegistroAuditoria> capturado = ArgumentCaptor.forClass(RegistroAuditoria.class);
        verify(repositorio).inserir(capturado.capture());
        assertThat(capturado.getValue().clienteId()).isNull();
        assertThat(capturado.getValue().usuarioId()).isNull();
        assertThat(capturado.getValue().usuarioEmail()).isNull();
        assertThat(capturado.getValue().usuarioPerfil()).isNull();
    }

    @Test
    void registraComUsuarioNaoNumerico() {
        when(relogio.agora()).thenReturn(AGORA);
        when(repositorio.inserir(any())).thenReturn(13L);
        RegistrarAuditoria casos = new RegistrarAuditoria(repositorio, usuarios, relogio);
        Acesso estranho = new Acesso("abc", Papel.ADMIN, 42L, "jti", AGORA);

        casos.registrar(estranho, AcaoAuditoria.UPDATE, "conta", 1L, "{}", "{}");

        ArgumentCaptor<RegistroAuditoria> capturado = ArgumentCaptor.forClass(RegistroAuditoria.class);
        verify(repositorio).inserir(capturado.capture());
        assertThat(capturado.getValue().usuarioId()).isNull();
        assertThat(capturado.getValue().usuarioEmail()).isNull();
    }

    private static RegistroAuditoria registro(long id) {
        return new RegistroAuditoria(id, 42L, 7L, "voce@fincontrol.local", "ADMIN", AcaoAuditoria.CREATE,
                "lancamento", 44L, null, "{}", AGORA);
    }
}
