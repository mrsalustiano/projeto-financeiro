package br.com.fincontrol.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fincontrol.domain.model.Cliente;
import br.com.fincontrol.domain.model.Papel;
import br.com.fincontrol.domain.model.Usuario;
import br.com.fincontrol.domain.service.ClienteRepositorio;
import br.com.fincontrol.domain.service.LoginBloqueadoException;
import br.com.fincontrol.domain.service.NaoAutenticadoException;
import br.com.fincontrol.domain.service.Relogio;
import br.com.fincontrol.domain.service.SenhaPort;
import br.com.fincontrol.domain.service.UsuarioRepositorio;

@ExtendWith(MockitoExtension.class)
class AutenticarUsuarioTest {

    private static final Instant AGORA = Instant.parse("2026-11-22T03:00:00Z");

    @Mock private UsuarioRepositorio usuarios;
    @Mock private ClienteRepositorio clientes;
    @Mock private SenhaPort senhas;
    @Mock private Relogio relogio;
    @Mock private EmissorDeSessao emissor;

    private AutenticarUsuario autenticar;

    @BeforeEach
    void setUp() {
        autenticar = new AutenticarUsuario(usuarios, clientes, senhas, relogio, emissor);
    }

    @Test
    void emailDesconhecidoNaoRevelaENaoConsultaCliente() {
        when(usuarios.buscarPorEmail("sumido@fincontrol.local")).thenReturn(Optional.empty());
        when(senhas.hashFicticio()).thenReturn("ficticio");

        assertThatThrownBy(() -> autenticar.executar("  Sumido@fincontrol.local ", "x"))
                .isInstanceOf(NaoAutenticadoException.class);
        verify(senhas).confere("x", "ficticio");
        verifyNoInteractions(clientes, emissor);
    }

    @Test
    void clienteCertoPedeSoOProprioId() {
        Usuario usuario = admin(0);
        when(usuarios.buscarPorEmail("voce@fincontrol.local")).thenReturn(Optional.of(usuario));
        when(senhas.confere("certa", "hash")).thenReturn(true);
        when(relogio.agora()).thenReturn(AGORA);
        when(clientes.buscarPorId(42L)).thenReturn(Optional.of(new Cliente(42L, "Casa", true)));
        when(emissor.emitir(org.mockito.ArgumentMatchers.any())).thenReturn(new SessaoEmitida("jwt", "opaco"));

        autenticar.executar("voce@fincontrol.local", "certa");

        verify(clientes).buscarPorId(42L);
        verify(clientes, never()).buscarPorId(99L);
        verify(usuarios).salvarEstado(org.mockito.ArgumentMatchers.argThat(salvo -> salvo.falhasLogin() == 0));
    }

    @Test
    void masterNaoConsultaCliente() {
        Usuario master = new Usuario(1L, null, "Master", "master@fincontrol.local", "hash", Papel.MASTER, true, 0, null);
        when(usuarios.buscarPorEmail("master@fincontrol.local")).thenReturn(Optional.of(master));
        when(senhas.confere("certa", "hash")).thenReturn(true);
        when(relogio.agora()).thenReturn(AGORA);
        when(emissor.emitir(master.zerarFalhas())).thenReturn(new SessaoEmitida("jwt", "opaco"));

        autenticar.executar("master@fincontrol.local", "certa");

        verifyNoInteractions(clientes);
    }

    @Test
    void senhaErradaEQuintaFalhaBloqueiam() {
        when(relogio.agora()).thenReturn(AGORA);
        when(clientes.buscarPorId(42L)).thenReturn(Optional.of(new Cliente(42L, "Casa", true)));
        when(senhas.confere("errada", "hash")).thenReturn(false);
        when(usuarios.buscarPorEmail("voce@fincontrol.local")).thenReturn(Optional.of(admin(0)), Optional.of(admin(4)));

        assertThatThrownBy(() -> autenticar.executar("voce@fincontrol.local", "errada"))
                .isInstanceOf(NaoAutenticadoException.class);
        assertThatThrownBy(() -> autenticar.executar("voce@fincontrol.local", "errada"))
                .isInstanceOf(LoginBloqueadoException.class);
        verifyNoInteractions(emissor);
    }

    @Test
    void inativoNaoGravaFalha() {
        Usuario inativo = new Usuario(7L, 42L, "Voce", "voce@fincontrol.local", "hash", Papel.ADMIN, false, 0, null);
        when(usuarios.buscarPorEmail("voce@fincontrol.local")).thenReturn(Optional.of(inativo));
        when(senhas.confere("certa", "hash")).thenReturn(true);
        when(relogio.agora()).thenReturn(AGORA);
        when(clientes.buscarPorId(42L)).thenReturn(Optional.of(new Cliente(42L, "Casa", true)));

        assertThatThrownBy(() -> autenticar.executar("voce@fincontrol.local", "certa"))
                .isInstanceOf(NaoAutenticadoException.class);
        verify(usuarios, never()).salvarEstado(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void clienteInativoOuAusenteRecusa() {
        when(senhas.confere("certa", "hash")).thenReturn(true);
        when(relogio.agora()).thenReturn(AGORA);
        when(usuarios.buscarPorEmail("voce@fincontrol.local")).thenReturn(
                Optional.of(admin(0)),
                Optional.of(admin(0)),
                Optional.of(new Usuario(7L, null, "Voce", "voce@fincontrol.local", "hash", Papel.ADMIN, true, 0, null)));
        when(clientes.buscarPorId(42L)).thenReturn(Optional.of(new Cliente(42L, "Casa", false)), Optional.empty());

        assertThatThrownBy(() -> autenticar.executar("voce@fincontrol.local", "certa")).isInstanceOf(NaoAutenticadoException.class);
        assertThatThrownBy(() -> autenticar.executar("voce@fincontrol.local", "certa")).isInstanceOf(NaoAutenticadoException.class);
        assertThatThrownBy(() -> autenticar.executar("voce@fincontrol.local", "certa")).isInstanceOf(NaoAutenticadoException.class);
    }

    private static Usuario admin(int falhas) {
        return new Usuario(7L, 42L, "Voce", "voce@fincontrol.local", "hash", Papel.ADMIN, true, falhas, null);
    }
}
