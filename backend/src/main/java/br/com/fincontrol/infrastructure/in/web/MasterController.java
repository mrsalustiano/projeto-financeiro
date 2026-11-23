package br.com.fincontrol.infrastructure.in.web;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import br.com.fincontrol.application.Auditorias;
import br.com.fincontrol.application.ClienteCriado;
import br.com.fincontrol.application.CriarCliente;
import br.com.fincontrol.domain.model.AcaoAuditoria;
import br.com.fincontrol.domain.model.FiltroAuditoria;
import br.com.fincontrol.domain.model.PaginaAuditoria;
import br.com.fincontrol.domain.model.RegistroAuditoria;
import br.com.fincontrol.infrastructure.in.web.api.MasterApi;
import br.com.fincontrol.infrastructure.in.web.api.model.AuditoriaResponse;
import br.com.fincontrol.infrastructure.in.web.api.model.ClienteCriadoResponse;
import br.com.fincontrol.infrastructure.in.web.api.model.NovoClienteRequest;
import br.com.fincontrol.infrastructure.in.web.api.model.PaginaAuditoriaResponse;

@RestController
public class MasterController implements MasterApi {

    private final CriarCliente criarCliente;
    private final Auditorias auditorias;

    public MasterController(CriarCliente criarCliente, Auditorias auditorias) {
        this.criarCliente = criarCliente;
        this.auditorias = auditorias;
    }

    @Override
    public ResponseEntity<ClienteCriadoResponse> criarCliente(NovoClienteRequest novoClienteRequest) {
        ClienteCriado criado = criarCliente.executar(
                novoClienteRequest.getNomeCliente(),
                novoClienteRequest.getNomeAdministrador(),
                novoClienteRequest.getEmailAdministrador(),
                novoClienteRequest.getSenhaAdministrador());
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(new ClienteCriadoResponse(
                Long.toString(criado.clienteId()), Long.toString(criado.administradorId())));
    }

    @Override
    public ResponseEntity<PaginaAuditoriaResponse> listarAuditoria(
            String clienteId, String recurso, String acao, LocalDate de, LocalDate ate, String cursor, Integer limite) {
        PaginaAuditoria pagina = auditorias.consultar(
                SessaoAtual.exigir(),
                new FiltroAuditoria(numero(clienteId), recurso, acao(acao), de, ate),
                cursor,
                limite);
        List<AuditoriaResponse> itens = pagina.itens().stream().map(MasterController::resposta).toList();
        PaginaAuditoriaResponse corpo = new PaginaAuditoriaResponse(itens);
        corpo.setProximoCursor(pagina.proximoCursor() == null ? null : Long.toString(pagina.proximoCursor()));
        return ResponseEntity.ok(corpo);
    }

    private static AuditoriaResponse resposta(RegistroAuditoria registro) {
        AuditoriaResponse corpo = new AuditoriaResponse(
                Long.toString(registro.id()),
                AuditoriaResponse.AcaoEnum.fromValue(registro.acao().name()),
                registro.recurso(),
                registro.criadoEm().atOffset(ZoneOffset.UTC));
        corpo.setClienteId(texto(registro.clienteId()));
        corpo.setUsuarioId(texto(registro.usuarioId()));
        corpo.setUsuarioEmail(registro.usuarioEmail());
        corpo.setUsuarioPerfil(registro.usuarioPerfil());
        corpo.setRecursoId(texto(registro.recursoId()));
        corpo.setDadosAntes(registro.dadosAntes());
        corpo.setDadosDepois(registro.dadosDepois());
        return corpo;
    }

    private static String texto(Long valor) {
        return valor == null ? null : Long.toString(valor);
    }

    private static Long numero(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(valor.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Identificador inválido.", ex);
        }
    }

    private static AcaoAuditoria acao(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return AcaoAuditoria.valueOf(valor.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Ação inválida.", ex);
        }
    }
}
