package br.com.fincontrol.infrastructure.out.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ArmazenamentoLocalTest {

    @TempDir
    private Path diretorio;

    @Test
    void gravaLeERemove() {
        ArmazenamentoLocal armazenamento = new ArmazenamentoLocal(diretorio.resolve("comprovantes").toString());
        byte[] conteudo = new byte[] {(byte) 0x89, 'P', 'N', 'G'};

        armazenamento.guardar("chave-1", conteudo);

        assertThat(armazenamento.ler("chave-1")).isEqualTo(conteudo);
        armazenamento.remover("chave-1");
        assertThatThrownBy(() -> armazenamento.ler("chave-1")).isInstanceOf(RuntimeException.class);
        armazenamento.remover("chave-1");
    }

    @Test
    void recusaChaveComCaminho() {
        ArmazenamentoLocal armazenamento = new ArmazenamentoLocal(diretorio.toString());

        assertThatThrownBy(() -> armazenamento.guardar("../fora", new byte[] {1}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> armazenamento.guardar("a/b", new byte[] {1}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> armazenamento.guardar("a\\b", new byte[] {1}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> armazenamento.guardar(" ", new byte[] {1}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> armazenamento.guardar(null, new byte[] {1}))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
