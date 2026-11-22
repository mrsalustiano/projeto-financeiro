package br.com.fincontrol.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fincontrol.domain.model.Acesso;
import br.com.fincontrol.domain.model.Cliente;
import br.com.fincontrol.domain.model.Papel;
import br.com.fincontrol.domain.model.RefreshGuardado;
import br.com.fincontrol.domain.model.Usuario;
import br.com.fincontrol.domain.service.ClienteRepositorio;
import br.com.fincontrol.domain.service.EmailEmUsoException;
import br.com.fincontrol.domain.service.NaoAutenticadoException;
import br.com.fincontrol.domain.service.RefreshOpacoPort;
import br.com.fincontrol.domain.service.RefreshRepositorio;
import br.com.fincontrol.domain.service.Relogio;
import br.com.fincontrol.domain.service.RevogacaoRepositorio;
import br.com.fincontrol.domain.service.SenhaPort;
import br.com.fincontrol.domain.service.UsuarioRepositorio;

@ExtendWith(MockitoExtension.class)
class SessaoUseCasesTest {

    private static final Instant AGORA = Instant.parse("2026-11-22T03:00:00Z");

    @Mock private RefreshRepositorio refreshRepositorio;
    @Mock private RefreshOpacoPort refresh;
    @Mock private UsuarioRepositorio usuarios;
    @Mock private ClienteRepositorio clientes;
    @Mock private RevogacaoRepositorio revogacao;
    @Mock private Relogio relogio;
    @Mock private EmissorDeSessao emissor;
    @Mock private SenhaPort senhas;

    @Test
    void refreshGiraCookieERevogaJtiAnterior() {
        RefreshGuardado guardado = new RefreshGuardado(3L, 7L, AGORA.plusSeconds(10), false, "jti-antigo", AGORA.plusSeconds(900));
        Usuario usuario = admin();
        when(refresh.hash("opaco")).thenReturn("hash");
        when(refreshRepositorio.buscarPorHash("hash")).thenReturn(Optional.of(guardado));
        when(relogio.agora()).thenReturn(AGORA);
        when(usuarios.buscarPorId(7L)).thenReturn(Optional.of(usuario));
        when(clientes.buscarPorId(42L)).thenReturn(Optional.of(new Cliente(42L, "Casa", true)));
        when(emissor.emitir(usuario)).thenReturn(new SessaoEmitida("novo", "novo-opaco"));

        SessaoEmitida sessao = renovar().executar("opaco");

        assertThat(sessao.accessToken()).isEqualTo("novo");
        verify(refreshRepositorio).revogar(3L);
        verify(revogacao).revogar("jti-antigo", guardado.accessExpiraEm());
        verify(clientes).buscarPorId(42L);
    }

    @Test
    void refreshInvalidoNaoEmite() {
        when(refresh.hash("opaco")).thenReturn("hash");
        when(refreshRepositorio.buscarPorHash("hash")).thenReturn(Optional.empty());
        RenovarSessao caso = renovar();

        assertThatThrownBy(() -> caso.executar(null)).isInstanceOf(NaoAutenticadoException.class);
        assertThatThrownBy(() -> caso.executar("opaco")).isInstanceOf(NaoAutenticadoException.class);
        verifyNoInteractions(emissor);
    }

    @Test
    void refreshRevogadoOuExpiradoRecusa() {
        when(refresh.hash("opaco")).thenReturn("hash");
        when(relogio.agora()).thenReturn(AGORA);
        when(refreshRepositorio.buscarPorHash("hash")).thenReturn(
                Optional.of(new RefreshGuardado(1L, 7L, AGORA.plusSeconds(10), true, "jti", AGORA)),
                Optional.of(new RefreshGuardado(1L, 7L, AGORA, false, "jti", AGORA)));
        RenovarSessao caso = renovar();

        assertThatThrownBy(() -> caso.executar("opaco")).isInstanceOf(NaoAutenticadoException.class);
        assertThatThrownBy(() -> caso.executar("opaco")).isInstanceOf(NaoAutenticadoException.class);
    }

    @Test
    void logoutRevogaRefreshEJti() {
        Acesso acesso = new Acesso("7", Papel.ADMIN, 42L, "jti", AGORA);
        when(refresh.hash("opaco")).thenReturn("hash");
        when(refreshRepositorio.buscarPorHash("hash"))
                .thenReturn(Optional.of(new RefreshGuardado(4L, 7L, AGORA, false, "jti", AGORA)));
        EncerrarSessao caso = new EncerrarSessao(refreshRepositorio, refresh, revogacao);

        caso.executar(acesso, "opaco");
        caso.executar(acesso, " ");

        verify(refreshRepositorio).revogar(4L);
        verify(revogacao, org.mockito.Mockito.times(2)).revogar("jti", AGORA);
        assertThatThrownBy(() -> caso.executar(null, "opaco")).isInstanceOf(NaoAutenticadoException.class);
    }

    @Test
    void meDoClienteUsaOIdDoTokenEMasterNaoConsultaCliente() {
        Usuario usuario = admin();
        when(usuarios.buscarPorId(7L)).thenReturn(Optional.of(usuario));
        when(clientes.buscarPorId(42L)).thenReturn(Optional.of(new Cliente(42L, "Casa", true)));
        ConsultarUsuarioAutenticado caso = new ConsultarUsuarioAutenticado(usuarios, clientes);

        UsuarioVisao visao = caso.executar(new Acesso("7", Papel.ADMIN, 42L, "jti", AGORA));

        assertThat(visao.clienteId()).isEqualTo("42");
        verify(clientes).buscarPorId(42L);
        verify(clientes, never()).buscarPorId(99L);
        when(usuarios.buscarPorId(1L)).thenReturn(Optional.of(
                new Usuario(1L, null, "Master", "master@fincontrol.local", "hash", Papel.MASTER, true, 0, null)));
        assertThat(caso.executar(new Acesso("1", Papel.MASTER, null, "jti", AGORA)).clienteId()).isNull();
    }

    @Test
    void meRecusaInativo() {
        when(usuarios.buscarPorId(7L)).thenReturn(Optional.of(
                new Usuario(7L, 42L, "Voce", "voce@fincontrol.local", "hash", Papel.ADMIN, false, 0, null)));
        ConsultarUsuarioAutenticado caso = new ConsultarUsuarioAutenticado(usuarios, clientes);

        assertThatThrownBy(() -> caso.executar(null)).isInstanceOf(NaoAutenticadoException.class);
        assertThatThrownBy(() -> caso.executar(new Acesso("7", Papel.ADMIN, 42L, "jti", AGORA)))
                .isInstanceOf(NaoAutenticadoException.class);
    }

    @Test
    void masterCriaAdminSemPortaDeLancamento() {
        when(usuarios.emailExiste("novo@fincontrol.local")).thenReturn(false);
        when(senhas.hash("segredo123")).thenReturn("hash");
        when(clientes.inserir("Casa nova", true)).thenReturn(9L);
        when(usuarios.inserir(org.mockito.ArgumentMatchers.any())).thenReturn(11L);
        CriarCliente caso = new CriarCliente(clientes, usuarios, senhas);

        ClienteCriado criado = caso.executar(" Casa nova ", " Ana ", " Novo@fincontrol.local ", "segredo123");

        assertThat(criado.clienteId()).isEqualTo(9L);
        assertThat(criado.administradorId()).isEqualTo(11L);
        assertThat(Arrays.stream(CriarCliente.class.getDeclaredFields())
                .noneMatch(campo -> campo.getType().getSimpleName().toLowerCase().contains("lancamento"))).isTrue();
    }

    @Test
    void emailRepetidoNaoCriaCliente() {
        when(usuarios.emailExiste("novo@fincontrol.local")).thenReturn(true);
        CriarCliente caso = new CriarCliente(clientes, usuarios, senhas);

        assertThatThrownBy(() -> caso.executar("Casa", "Ana", "novo@fincontrol.local", "segredo123"))
                .isInstanceOf(EmailEmUsoException.class);
        verifyNoInteractions(clientes);
    }

    private RenovarSessao renovar() {
        return new RenovarSessao(refreshRepositorio, refresh, usuarios, clientes, revogacao, relogio, emissor);
    }

    private static Usuario admin() {
        return new Usuario(7L, 42L, "Voce", "voce@fincontrol.local", "hash", Papel.ADMIN, true, 0, null);
    }
}
