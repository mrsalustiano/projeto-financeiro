package br.com.fincontrol.infrastructure.out.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import br.com.fincontrol.domain.service.ArmazenamentoPort;

@Component
public class ArmazenamentoLocal implements ArmazenamentoPort {

    private final Path diretorio;

    public ArmazenamentoLocal(@Value("${fincontrol.armazenamento.diretorio:./dados/comprovantes}") String diretorio) {
        this.diretorio = Path.of(diretorio).toAbsolutePath().normalize();
    }

    @Override
    public void guardar(String chave, byte[] conteudo) {
        Path destino = caminho(chave);
        try {
            Files.createDirectories(diretorio);
            Path temporario = Files.createTempFile(diretorio, "comprovante-", ".tmp");
            Files.write(temporario, conteudo);
            Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new UncheckedIOException("Falha ao gravar comprovante.", ex);
        }
    }

    @Override
    public byte[] ler(String chave) {
        try {
            return Files.readAllBytes(caminho(chave));
        } catch (IOException ex) {
            throw new UncheckedIOException("Falha ao ler comprovante.", ex);
        }
    }

    @Override
    public void remover(String chave) {
        try {
            Files.deleteIfExists(caminho(chave));
        } catch (IOException ex) {
            throw new UncheckedIOException("Falha ao remover comprovante.", ex);
        }
    }

    private Path caminho(String chave) {
        if (chave == null || chave.isBlank() || chave.contains("/") || chave.contains("\\") || chave.contains("..")) {
            throw new IllegalArgumentException("Chave de armazenamento inválida.");
        }
        Path caminho = diretorio.resolve(chave).normalize();
        if (!caminho.startsWith(diretorio)) {
            throw new IllegalArgumentException("Chave de armazenamento inválida.");
        }
        return caminho;
    }
}
