package br.com.fincontrol.infrastructure.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import br.com.fincontrol.application.ClienteCriado;
import br.com.fincontrol.application.CriarCliente;
import br.com.fincontrol.infrastructure.in.web.api.MasterApi;
import br.com.fincontrol.infrastructure.in.web.api.model.ClienteCriadoResponse;
import br.com.fincontrol.infrastructure.in.web.api.model.NovoClienteRequest;

@RestController
public class MasterController implements MasterApi {

    private final CriarCliente criarCliente;

    public MasterController(CriarCliente criarCliente) {
        this.criarCliente = criarCliente;
    }

    @Override
    public ResponseEntity<ClienteCriadoResponse> criarCliente(NovoClienteRequest novoClienteRequest) {
        ClienteCriado criado = criarCliente.executar(
                novoClienteRequest.getNomeCliente(),
                novoClienteRequest.getNomeAdministrador(),
                novoClienteRequest.getEmailAdministrador(),
                novoClienteRequest.getSenhaAdministrador());
        return ResponseEntity.status(HttpStatus.CREATED).body(new ClienteCriadoResponse(
                Long.toString(criado.clienteId()), Long.toString(criado.administradorId())));
    }
}
