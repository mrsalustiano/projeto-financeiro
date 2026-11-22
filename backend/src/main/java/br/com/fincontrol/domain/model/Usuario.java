package br.com.fincontrol.domain.model;

import java.time.Instant;

public record Usuario(
        long id,
        Long clienteId,
        String nome,
        String email,
        String senhaHash,
        Papel papel,
        boolean ativo,
        int falhasLogin,
        Instant bloqueadoAte) {

    public boolean bloqueadoEm(Instant agora) {
        return bloqueadoAte != null && bloqueadoAte.isAfter(agora);
    }

    public Usuario zerarFalhas() {
        return new Usuario(id, clienteId, nome, email, senhaHash, papel, ativo, 0, null);
    }

    public Usuario registrarFalha(Instant agora) {
        int proximas = falhasLogin + 1;
        if (proximas >= DuracaoSessao.LIMITE_FALHAS) {
            return comFalhas(0, agora.plus(DuracaoSessao.BLOQUEIO));
        }
        return comFalhas(proximas, null);
    }

    private Usuario comFalhas(int falhas, Instant bloqueio) {
        return new Usuario(id, clienteId, nome, email, senhaHash, papel, ativo, falhas, bloqueio);
    }
}
