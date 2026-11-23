package br.com.fincontrol.infrastructure.out.external;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import br.com.fincontrol.domain.model.Dinheiro;
import br.com.fincontrol.domain.model.ExtratoOfx;
import br.com.fincontrol.domain.model.MovimentoOfx;
import br.com.fincontrol.domain.model.Natureza;
import br.com.fincontrol.domain.service.ArquivoNaoOfxException;
import br.com.fincontrol.domain.service.OfxPort;

@Component
public class OfxParser implements OfxPort {

    private static final Pattern TAG = Pattern.compile("<([A-Za-z0-9_.]+)>([^<]*)");
    private static final Set<String> CAMPOS_MOVIMENTO = Set.of(
            "TRNTYPE", "DTPOSTED", "TRNAMT", "FITID", "NAME", "MEMO", "CHECKNUM", "REFNUM");
    private static final int TAMANHO_DATA = 8;

    @Override
    public ExtratoOfx ler(byte[] arquivo) {
        if (arquivo == null || arquivo.length == 0) {
            throw new ArquivoNaoOfxException();
        }
        List<Token> tokens = tokens(decodificar(arquivo));
        if (tokens.stream().noneMatch(token -> token.tag().equals("OFX"))) {
            throw new ArquivoNaoOfxException();
        }
        return new Leitor(tokens).extrato();
    }

    static LocalDate data(String dtPosted) {
        if (dtPosted == null) {
            return null;
        }
        String digitos = dtPosted.replaceAll("\\D", "");
        if (digitos.length() < TAMANHO_DATA) {
            return null;
        }
        try {
            return LocalDate.parse(digitos.substring(0, TAMANHO_DATA), DateTimeFormatter.BASIC_ISO_DATE);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    static BigDecimal valor(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(texto.trim().replace(",", "."));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    static String descricao(Map<String, String> campos) {
        String memo = campos.get("MEMO");
        if (memo != null && !memo.isBlank()) {
            return memo;
        }
        String nome = campos.get("NAME");
        return nome == null ? "" : nome;
    }

    static boolean ehRendimento(String nome) {
        return nome != null && nome.toUpperCase(Locale.ROOT).contains("RENDIMENTO");
    }

    static boolean ehCampoDeMovimento(String tag) {
        return CAMPOS_MOVIMENTO.contains(tag);
    }

    static Optional<MovimentoOfx> movimento(Map<String, String> campos, List<String> avisos) {
        LocalDate data = data(campos.get("DTPOSTED"));
        if (data == null) {
            avisos.add("Movimento sem data válida ignorado.");
            return Optional.empty();
        }
        BigDecimal valor = valor(campos.get("TRNAMT"));
        if (valor == null || valor.signum() == 0) {
            avisos.add("Movimento de " + data + " sem valor válido ignorado.");
            return Optional.empty();
        }
        String fitid = campos.get("FITID");
        if (fitid == null || fitid.isBlank()) {
            avisos.add("Movimento de " + data + " sem FITID ignorado.");
            return Optional.empty();
        }
        Natureza natureza = valor.signum() > 0 ? Natureza.RECEITA : Natureza.DESPESA;
        return Optional.of(new MovimentoOfx(fitid.trim(), data, new Dinheiro(valor.abs()), natureza,
                descricao(campos)));
    }

    private static List<Token> tokens(String texto) {
        List<Token> lista = new ArrayList<>();
        Matcher matcher = TAG.matcher(texto);
        while (matcher.find()) {
            lista.add(new Token(matcher.group(1), matcher.group(2).trim()));
        }
        return lista;
    }

    private static String decodificar(byte[] arquivo) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(arquivo))
                    .toString();
        } catch (CharacterCodingException ex) {
            return new String(arquivo, Charset.forName("windows-1252"));
        }
    }

    record Token(String tag, String valor) {
    }

    private static final class Leitor {

        private final List<Token> tokens;
        private final List<MovimentoOfx> movimentos = new ArrayList<>();
        private final List<String> avisos = new ArrayList<>();
        private Map<String, String> atual = new LinkedHashMap<>();
        private boolean emMovimento;
        private boolean emSaldo;
        private boolean emRendimento;
        private String ultimoNome;
        private BigDecimal saldo;
        private BigDecimal rendimento;

        private Leitor(List<Token> tokens) {
            this.tokens = tokens;
        }

        private ExtratoOfx extrato() {
            for (Token token : tokens) {
                consumir(token);
            }
            fecharMovimento();
            return new ExtratoOfx(
                    primeiro("ORG"),
                    primeiro("FID"),
                    primeiro("BANKID"),
                    primeiro("ACCTID"),
                    primeiro("CURDEF"),
                    data(primeiro("DTSTART")),
                    data(primeiro("DTEND")),
                    movimentos,
                    rendimento,
                    saldo,
                    avisos);
        }

        private void consumir(Token token) {
            if (token.tag().equals("STMTTRN")) {
                iniciarMovimento();
                return;
            }
            if (emMovimento) {
                consumirMovimento(token);
                return;
            }
            consumirSecao(token);
        }

        private void consumirMovimento(Token token) {
            if (ehCampoDeMovimento(token.tag())) {
                atual.put(token.tag(), token.valor());
                return;
            }
            fecharMovimento();
            consumirSecao(token);
        }

        private void consumirSecao(Token token) {
            marcarSecao(token.tag());
            if (token.tag().equals("NAME")) {
                ultimoNome = token.valor();
            }
            if (token.tag().equals("BALAMT")) {
                registrarSaldo(token.valor());
            }
        }

        private void marcarSecao(String tag) {
            switch (tag) {
                case "LEDGERBAL" -> emSaldo = true;
                case "BALLIST" -> {
                    emSaldo = false;
                    emRendimento = true;
                }
                case "STMTRS", "STMTTRNRS" -> {
                    emSaldo = false;
                    emRendimento = false;
                }
                default -> {
                }
            }
        }

        private void registrarSaldo(String texto) {
            if (emSaldo && saldo == null) {
                saldo = valor(texto);
            } else if (emRendimento && rendimento == null && ehRendimento(ultimoNome)) {
                rendimento = valor(texto);
            }
        }

        private void iniciarMovimento() {
            fecharMovimento();
            atual = new LinkedHashMap<>();
            emMovimento = true;
        }

        private void fecharMovimento() {
            if (!emMovimento) {
                return;
            }
            emMovimento = false;
            if (!atual.isEmpty()) {
                movimento(atual, avisos).ifPresent(movimentos::add);
            }
            atual = new LinkedHashMap<>();
        }

        private String primeiro(String tag) {
            return tokens.stream()
                    .filter(token -> token.tag().equals(tag))
                    .map(Token::valor)
                    .filter(valor -> !valor.isBlank())
                    .findFirst()
                    .orElse(null);
        }
    }
}
