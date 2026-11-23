package br.com.fincontrol.infrastructure.out.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import br.com.fincontrol.domain.model.ExtratoOfx;
import br.com.fincontrol.domain.model.MovimentoOfx;
import br.com.fincontrol.domain.model.Natureza;
import br.com.fincontrol.domain.service.ArquivoNaoOfxException;

class OfxParserTest {

    private static final Path PASTA = Path.of("..", "contracts", "ofx");
    private final OfxParser parser = new OfxParser();

    @Test
    void leOfxSgmlDoNubankComTrintaEOitoMovimentos() throws IOException {
        ExtratoOfx extrato = parser.ler(Files.readAllBytes(PASTA.resolve("NU_7224833363_01OUT2026_22OUT2026.ofx")));

        assertThat(extrato.movimentos()).hasSize(38);
        assertThat(extrato.bankId()).isEqualTo("260");
        assertThat(extrato.acctId()).isEqualTo("7224833363");
        assertThat(extrato.moeda()).isEqualTo("BRL");
        assertThat(extrato.inicio()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(extrato.fim()).isEqualTo(LocalDate.of(2026, 10, 22));
        assertThat(extrato.saldoInformado()).isEqualByComparingTo("1234.56");
        assertThat(extrato.rendimento()).isEqualByComparingTo("12.34");
        assertThat(extrato.avisos()).isEmpty();
        assertThat(extrato.vazio()).isFalse();
    }

    @Test
    void separaCreditoDeSalarioDoCreditoSemRegra() throws IOException {
        ExtratoOfx extrato = parser.ler(Files.readAllBytes(PASTA.resolve("NU_7224833363_01OUT2026_22OUT2026.ofx")));

        MovimentoOfx salario = extrato.movimentos().stream()
                .filter(item -> item.fitid().equals("2026101500037"))
                .findFirst()
                .orElseThrow();
        MovimentoOfx outro = extrato.movimentos().stream()
                .filter(item -> item.fitid().equals("2026101500038"))
                .findFirst()
                .orElseThrow();

        assertThat(salario.data()).isEqualTo(LocalDate.of(2026, 10, 15));
        assertThat(salario.valor().valor()).isEqualByComparingTo("4800.00");
        assertThat(salario.natureza()).isEqualTo(Natureza.RECEITA);
        assertThat(salario.descricao()).contains("ITAU");
        assertThat(outro.valor().valor()).isEqualByComparingTo("1200.00");
        assertThat(outro.descricao()).doesNotContain("ITAU");
    }

    @Test
    void leOfxXmlComTagsFechadas() throws IOException {
        ExtratoOfx extrato = parser.ler(Files.readAllBytes(PASTA.resolve("nubank-xml.ofx")));

        assertThat(extrato.movimentos()).hasSize(2);
        assertThat(extrato.movimentos().getFirst().valor().valor()).isEqualByComparingTo("25.00");
        assertThat(extrato.movimentos().getFirst().natureza()).isEqualTo(Natureza.DESPESA);
        assertThat(extrato.saldoInformado()).isEqualByComparingTo("4775.00");
        assertThat(extrato.rendimento()).isNull();
    }

    @Test
    void recusaArquivoQueNaoEOfx() throws IOException {
        byte[] csv = Files.readAllBytes(PASTA.resolve("extrato.csv"));

        assertThatThrownBy(() -> parser.ler(csv)).isInstanceOf(ArquivoNaoOfxException.class);
        assertThatThrownBy(() -> parser.ler(new byte[0])).isInstanceOf(ArquivoNaoOfxException.class);
        assertThatThrownBy(() -> parser.ler(null)).isInstanceOf(ArquivoNaoOfxException.class);
    }

    @Test
    void avisaMovimentoSemFitidSemValorESemData() {
        ExtratoOfx extrato = parser.ler(sgml("""
                <STMTTRN>
                <DTPOSTED>20261005
                <TRNAMT>-10.00
                <MEMO>Sem fitid
                </STMTTRN>
                <STMTTRN>
                <DTPOSTED>20261006
                <TRNAMT>0.00
                <FITID>ZERO
                <MEMO>Valor zero
                </STMTTRN>
                <STMTTRN>
                <DTPOSTED>ontem
                <TRNAMT>-5.00
                <FITID>SEMDATA
                <MEMO>Data ruim
                </STMTTRN>
                """));

        assertThat(extrato.movimentos()).isEmpty();
        assertThat(extrato.avisos()).hasSize(3);
        assertThat(extrato.vazio()).isTrue();
    }

    @Test
    void decodificaArquivoEmWindows1252() {
        String texto = """
                <OFX><BANKTRANLIST><STMTTRN>
                <DTPOSTED>20261005
                <TRNAMT>-10.00
                <FITID>A1
                <MEMO>Serviço de manutenção
                </STMTTRN></BANKTRANLIST></OFX>
                """;
        byte[] bytes = texto.getBytes(Charset.forName("windows-1252"));

        ExtratoOfx extrato = parser.ler(bytes);

        assertThat(extrato.movimentos()).hasSize(1);
        assertThat(extrato.movimentos().getFirst().descricao()).isEqualTo("Serviço de manutenção");
    }

    @Test
    void usaNomeQuandoNaoHaMemo() {
        ExtratoOfx extrato = parser.ler(sgml("""
                <STMTTRN>
                <DTPOSTED>20261005
                <TRNAMT>-10.00
                <FITID>A2
                <NAME>Compra no debito
                </STMTTRN>
                """));

        assertThat(extrato.movimentos().getFirst().descricao()).isEqualTo("Compra no debito");
    }

    private static byte[] sgml(String corpo) {
        String texto = "<OFX><BANKMSGSRSV1><STMTTRNRS><STMTRS><BANKTRANLIST>" + corpo
                + "</BANKTRANLIST></STMTRS></STMTTRNRS></BANKMSGSRSV1></OFX>";
        return texto.getBytes(StandardCharsets.UTF_8);
    }
}
