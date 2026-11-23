package br.com.fincontrol.infrastructure.out.external;

import br.com.fincontrol.application.ImportacaoOfx;
import br.com.fincontrol.application.ResultadoImportacao;
import br.com.fincontrol.application.SugestaoOfx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fincontrol.domain.model.Acesso;
import br.com.fincontrol.domain.model.Categoria;
import br.com.fincontrol.domain.model.Conta;
import br.com.fincontrol.domain.model.Lancamento;
import br.com.fincontrol.domain.model.Natureza;
import br.com.fincontrol.domain.model.Papel;
import br.com.fincontrol.domain.model.RegraClassificacao;
import br.com.fincontrol.domain.model.Saldo;
import br.com.fincontrol.domain.model.TipoConta;
import br.com.fincontrol.domain.service.AcessoNegadoException;
import br.com.fincontrol.domain.service.CategoriaRepositorio;
import br.com.fincontrol.domain.service.ContaRepositorio;
import br.com.fincontrol.domain.service.IdempotenciaRepositorio;
import br.com.fincontrol.domain.service.LancamentoRepositorio;
import br.com.fincontrol.domain.service.NaoEncontradoException;
import br.com.fincontrol.domain.service.OfxContaDivergenteException;
import br.com.fincontrol.domain.service.OfxSemTransacaoException;
import br.com.fincontrol.domain.service.RegraClassificacaoRepositorio;

@ExtendWith(MockitoExtension.class)
class ImportacaoOfxTest {

    private static final Path PASTA = Path.of("..", "contracts", "ofx");
    private static final Acesso CASA = new Acesso("7", Papel.ADMIN, 42L, "jti", null);
    private static final Acesso LEITOR = new Acesso("3", Papel.LEITOR, 42L, "jti", null);

    @Mock private ContaRepositorio contas;
    @Mock private CategoriaRepositorio categorias;
    @Mock private LancamentoRepositorio lancamentos;
    @Mock private RegraClassificacaoRepositorio regras;
    @Mock private IdempotenciaRepositorio idempotencia;

    private ImportacaoOfx importacao;

    @BeforeEach
    void setUp() {
        importacao = new ImportacaoOfx(new OfxParser(), contas, categorias, lancamentos, regras, idempotencia);
    }

    @Test
    void importaOfxDoNubankSemTocarNaFlash() throws IOException {
        prepararCasa();
        AtomicLong sequencia = new AtomicLong(100);
        when(lancamentos.inserir(any())).thenAnswer(inv -> sequencia.incrementAndGet());

        ResultadoImportacao resultado = importacao.importar(CASA, 1L, ofxNubank());

        assertThat(resultado.importados()).isEqualTo(38);
        assertThat(resultado.duplicados()).isZero();
        assertThat(resultado.semCategoria()).isEqualTo(37);
        assertThat(resultado.saldoInformado()).isEqualByComparingTo("1234.56");
        assertThat(resultado.rendimento()).isEqualByComparingTo("12.34");
        assertThat(resultado.banco()).isEqualTo("Nubank");
        assertThat(resultado.identificadorConta()).isEqualTo("7224833363");
        assertThat(resultado.avisos()).isEmpty();

        List<Lancamento> gravados = capturar(39);
        assertThat(gravados).allSatisfy(item -> assertThat(item.contaId()).isEqualTo(1L));
        assertThat(gravados).noneMatch(item -> item.contaId() == 2L);
        assertThat(gravados).extracting(Lancamento::descricao)
                .doesNotContain("Mercado do mês", "Combustível", "Benefício Flash");
        verify(contas).atualizarIdentificador(42L, 1L, "7224833363");
    }

    @Test
    void classificaSalarioEMantemCreditoSemRegra() throws IOException {
        prepararCasa();
        AtomicLong sequencia = new AtomicLong(100);
        when(lancamentos.inserir(any())).thenAnswer(inv -> sequencia.incrementAndGet());

        importacao.importar(CASA, 1L, ofxNubank());

        List<Lancamento> gravados = capturar(39);
        Lancamento salario = gravados.stream()
                .filter(item -> item.valor().valor().compareTo(new BigDecimal("4800.00")) == 0)
                .findFirst()
                .orElseThrow();
        Lancamento semRegra = gravados.stream()
                .filter(item -> item.valor().valor().compareTo(new BigDecimal("1200.00")) == 0)
                .findFirst()
                .orElseThrow();

        assertThat(salario.categoriaId()).isEqualTo(10L);
        assertThat(salario.natureza()).isEqualTo(Natureza.RECEITA);
        assertThat(salario.data()).isEqualTo(LocalDate.of(2026, 10, 15));
        assertThat(semRegra.categoriaId()).isNull();
        assertThat(semRegra.natureza()).isEqualTo(Natureza.RECEITA);
    }

    @Test
    void segundaImportacaoNaoCriaItemNovo() throws IOException {
        when(contas.buscar(42L, 1L)).thenReturn(Optional.of(conta(1L, "Nubank", "Nubank", TipoConta.CORRENTE)));
        when(categorias.listar(42L)).thenReturn(List.of(
                new Categoria(10L, 42L, "Salário", Natureza.RECEITA),
                new Categoria(11L, 42L, "Rendimentos", Natureza.RECEITA)));
        when(regras.listar(42L)).thenReturn(List.of());
        when(lancamentos.existeFitid(eq(42L), eq(1L), anyString())).thenReturn(true);
        when(idempotencia.buscar(eq(42L), anyString()))
                .thenReturn(Optional.of(new br.com.fincontrol.domain.model.RegistroIdempotencia("hash", 1L)));

        ResultadoImportacao resultado = importacao.importar(CASA, 1L, ofxNubank());

        assertThat(resultado.importados()).isZero();
        assertThat(resultado.duplicados()).isEqualTo(38);
        assertThat(resultado.rendimento()).isNull();
        verify(lancamentos, never()).inserir(any());
    }

    @Test
    void recusaArquivoDeOutroBanco() throws IOException {
        when(contas.buscar(42L, 3L)).thenReturn(Optional.of(conta(3L, "Banco do Brasil", "Banco do Brasil",
                TipoConta.CORRENTE)));

        assertThatThrownBy(() -> importacao.importar(CASA, 3L, ofxNubank()))
                .isInstanceOf(OfxContaDivergenteException.class);
        verify(lancamentos, never()).inserir(any());
    }

    @Test
    void recusaOfxSemTransacao() {
        when(contas.buscar(42L, 1L)).thenReturn(Optional.of(conta(1L, "Nubank", "Nubank", TipoConta.CORRENTE)));
        byte[] vazio = "<OFX><BANKMSGSRSV1></BANKMSGSRSV1></OFX>".getBytes(java.nio.charset.StandardCharsets.UTF_8);

        assertThatThrownBy(() -> importacao.importar(CASA, 1L, vazio))
                .isInstanceOf(OfxSemTransacaoException.class);
    }

    @Test
    void contaDeOutroClienteNaoExiste() {
        when(contas.buscar(42L, 9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> importacao.importar(CASA, 9L, ofxNubank()))
                .isInstanceOf(NaoEncontradoException.class);
    }

    @Test
    void leitorNaoImporta() {
        assertThatThrownBy(() -> importacao.importar(LEITOR, 1L, ofxNubank()))
                .isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    void sugestaoApontaSoAContaDoMesmoBanco() throws IOException {
        when(contas.listar(42L)).thenReturn(List.of(
                conta(1L, "Nubank", "Nubank", TipoConta.CORRENTE),
                conta(2L, "Flash", "Flash", TipoConta.BENEFICIO),
                conta(3L, "Banco do Brasil", "Banco do Brasil", TipoConta.CORRENTE)));

        SugestaoOfx sugestao = importacao.sugerir(CASA, ofxNubank());

        assertThat(sugestao.banco()).isEqualTo("Nubank");
        assertThat(sugestao.identificador()).isEqualTo("7224833363");
        assertThat(sugestao.movimentos()).isEqualTo(38);
        assertThat(sugestao.contasSugeridas()).extracting(Conta::id).containsExactly(1L);
    }

    @Test
    void sugestaoSemBancoConhecidoNaoApontaConta() throws IOException {
        when(contas.listar(42L)).thenReturn(List.of(conta(1L, "Nubank", "Nubank", TipoConta.CORRENTE)));

        SugestaoOfx sugestao = importacao.sugerir(CASA, ofxBancoDoBrasil());

        assertThat(sugestao.banco()).isEqualTo("Banco do Brasil");
        assertThat(sugestao.contasSugeridas()).isEmpty();
    }

    private void prepararCasa() {
        when(contas.buscar(42L, 1L)).thenReturn(Optional.of(conta(1L, "Nubank", "Nubank", TipoConta.CORRENTE)));
        when(categorias.listar(42L)).thenReturn(List.of(
                new Categoria(10L, 42L, "Salário", Natureza.RECEITA),
                new Categoria(11L, 42L, "Rendimentos", Natureza.RECEITA),
                new Categoria(4L, 42L, "Alimentação", Natureza.DESPESA)));
        when(regras.listar(42L)).thenReturn(List.of(
                new RegraClassificacao(1L, 42L, "ITAU", 15, 15, new BigDecimal("4800.00"),
                        new BigDecimal("4800.00"), "Salário"),
                new RegraClassificacao(2L, 42L, "ITAU", 28, 2, new BigDecimal("3750.00"),
                        new BigDecimal("3850.00"), "Salário")));
        when(lancamentos.existeFitid(eq(42L), anyLong(), anyString())).thenReturn(false);
        when(idempotencia.buscar(eq(42L), anyString())).thenReturn(Optional.empty());
    }

    private List<Lancamento> capturar(int total) {
        ArgumentCaptor<Lancamento> capturado = ArgumentCaptor.forClass(Lancamento.class);
        verify(lancamentos, org.mockito.Mockito.times(total)).inserir(capturado.capture());
        return capturado.getAllValues();
    }

    private static byte[] ofxNubank() throws IOException {
        return Files.readAllBytes(PASTA.resolve("NU_7224833363_01OUT2026_22OUT2026.ofx"));
    }

    private static byte[] ofxBancoDoBrasil() throws IOException {
        return Files.readAllBytes(PASTA.resolve("BB_1234567_01OUT2026_22OUT2026.ofx"));
    }

    private static Conta conta(long id, String nome, String banco, TipoConta tipo) {
        return new Conta(id, 42L, nome, banco, tipo, Saldo.zero(), LocalDate.of(2026, 1, 1), null);
    }
}
