package br.com.fincontrol;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class ContratoColecaoTest {

    private static final Map<String, Set<String>> EXIGIDOS = Map.of(
            "login", Set.of("200", "400", "401", "429"),
            "logout", Set.of("204", "401"),
            "refreshToken", Set.of("200", "401"),
            "me", Set.of("200", "401"),
            "criarCliente", Set.of("201", "400", "401", "403"));

    @Test
    void postmanEBrunoCobremCadaOperationId() throws Exception {
        String contratos = Files.readString(Path.of("src/main/resources/openapi/auth.yaml"))
                + Files.readString(Path.of("src/main/resources/openapi/master.yaml"));
        Set<String> ids = operationIds(contratos);
        List<String> nomes = new ArrayList<>();
        nomes.addAll(nomesPostman());
        nomes.addAll(nomesBruno());

        assertThat(ids).containsExactlyInAnyOrderElementsOf(EXIGIDOS.keySet());
        EXIGIDOS.forEach((id, status) -> status.forEach(codigo ->
                assertThat(nomes).anyMatch(nome -> nome.startsWith(id + "_") && nome.contains("_" + codigo))));
    }

    private static Set<String> operationIds(String yaml) {
        Matcher matcher = Pattern.compile("operationId:\\s*(\\w+)").matcher(yaml);
        Set<String> ids = new java.util.HashSet<>();
        while (matcher.find()) {
            ids.add(matcher.group(1));
        }
        return ids;
    }

    private static List<String> nomesPostman() throws Exception {
        String json = Files.readString(Path.of("..", "contracts", "postman", "FinControl.postman_collection.json"));
        Matcher matcher = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        List<String> nomes = new ArrayList<>();
        while (matcher.find()) {
            nomes.add(matcher.group(1));
        }
        return nomes;
    }

    private static List<String> nomesBruno() throws Exception {
        Path pasta = Path.of("..", "contracts", "bruno", "fincontrol", "auth");
        try (Stream<Path> arquivos = Files.list(pasta)) {
            return arquivos.map(path -> path.getFileName().toString().replace(".bru", "")).toList();
        }
    }
}
