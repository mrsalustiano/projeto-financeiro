package br.com.fincontrol;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static java.util.Map.entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class ContratoColecaoTest {

    private static final Map<String, Set<String>> EXIGIDOS = Map.ofEntries(
            entry("login", Set.of("200", "400", "401", "429")),
            entry("logout", Set.of("204", "401")),
            entry("refreshToken", Set.of("200", "401")),
            entry("me", Set.of("200", "401")),
            entry("criarCliente", Set.of("201", "400", "401", "403")),
            entry("listarAuditoria", Set.of("200", "400", "401", "403")),
            entry("listarCategorias", Set.of("200", "401", "403")),
            entry("criarCategoria", Set.of("201", "400", "401", "403", "422")),
            entry("atualizarCategoria", Set.of("200", "400", "401", "403", "404", "422")),
            entry("excluirCategoria", Set.of("204", "401", "403", "404")),
            entry("listarContas", Set.of("200", "401", "403")),
            entry("criarConta", Set.of("201", "400", "401", "403", "422")),
            entry("obterConta", Set.of("200", "401", "403", "404")),
            entry("atualizarConta", Set.of("200", "400", "401", "403", "404", "422")),
            entry("excluirConta", Set.of("204", "401", "403", "404")),
            entry("listarLancamentos", Set.of("200", "401", "403")),
            entry("criarLancamento", Set.of("201", "400", "401", "403", "409", "422")),
            entry("obterLancamento", Set.of("200", "401", "403", "404")),
            entry("atualizarLancamento", Set.of("200", "400", "401", "403", "404", "409", "422")),
            entry("estornarLancamento", Set.of("200", "400", "401", "403", "404", "409", "422")),
            entry("excluirLancamento", Set.of("204", "401", "403", "404", "409", "422")),
            entry("obterSaldoConta", Set.of("200", "401", "403", "404")),
            entry("listarMeses", Set.of("200", "401", "403")),
            entry("fecharMes", Set.of("200", "400", "401", "403")),
            entry("reabrirMes", Set.of("204", "400", "401", "403", "404")),
            entry("listarTransferencias", Set.of("200", "401", "403")),
            entry("criarTransferencia", Set.of("201", "400", "401", "403", "404", "409", "422")),
            entry("listarOrcamentos", Set.of("200", "400", "401", "403")),
            entry("definirOrcamento", Set.of("200", "400", "401", "403", "404")),
            entry("excluirOrcamento", Set.of("204", "400", "401", "403", "404")),
            entry("listarMetas", Set.of("200", "401", "403")),
            entry("criarMeta", Set.of("201", "400", "401", "403", "422")),
            entry("obterMeta", Set.of("200", "401", "403", "404")),
            entry("listarAportes", Set.of("200", "401", "403", "404")),
            entry("criarAporte", Set.of("201", "400", "401", "403", "404")),
            entry("listarRecorrencias", Set.of("200", "401", "403")),
            entry("criarRecorrencia", Set.of("201", "400", "401", "403", "404", "422")),
            entry("pausarRecorrencia", Set.of("200", "401", "403", "404", "422")),
            entry("retomarRecorrencia", Set.of("200", "401", "403", "404", "422")),
            entry("sugerirContasOfx", Set.of("200", "400", "401", "403", "422")),
            entry("importarOfx", Set.of("201", "400", "401", "403", "404", "422")),
            entry("sugerirConciliacao", Set.of("200", "400", "401", "403", "404")),
            entry("conciliar", Set.of("201", "400", "401", "403", "404", "409", "422")),
            entry("desfazerConciliacao", Set.of("204", "401", "403", "404")),
            entry("anexarComprovante", Set.of("201", "400", "401", "403", "404", "409", "413", "422")),
            entry("listarComprovantes", Set.of("200", "401", "403", "404")),
            entry("baixarComprovante", Set.of("200", "401", "403", "404")),
            entry("excluirComprovante", Set.of("204", "401", "403", "404")));

    @Test
    void postmanEBrunoCobremCadaOperationId() throws Exception {
        String contratos = Files.readString(Path.of("src/main/resources/openapi/auth.yaml"))
                + Files.readString(Path.of("src/main/resources/openapi/master.yaml"))
                + Files.readString(Path.of("src/main/resources/openapi/categorias.yaml"))
                + Files.readString(Path.of("src/main/resources/openapi/contas.yaml"))
                + Files.readString(Path.of("src/main/resources/openapi/lancamentos.yaml"))
                + Files.readString(Path.of("src/main/resources/openapi/transferencias.yaml"))
                + Files.readString(Path.of("src/main/resources/openapi/orcamentos.yaml"))
                + Files.readString(Path.of("src/main/resources/openapi/metas.yaml"))
                + Files.readString(Path.of("src/main/resources/openapi/recorrencias.yaml"))
                + Files.readString(Path.of("src/main/resources/openapi/ofx.yaml"))
                + Files.readString(Path.of("src/main/resources/openapi/conciliacao.yaml"));
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
        Path pasta = Path.of("..", "contracts", "bruno", "fincontrol");
        try (Stream<Path> arquivos = Files.walk(pasta)) {
            return arquivos
                    .filter(path -> path.toString().endsWith(".bru"))
                    .filter(path -> !path.toString().contains("environments"))
                    .map(path -> path.getFileName().toString().replace(".bru", ""))
                    .toList();
        }
    }
}
