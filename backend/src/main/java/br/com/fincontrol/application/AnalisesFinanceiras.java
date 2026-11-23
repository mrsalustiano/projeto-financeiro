package br.com.fincontrol.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import br.com.fincontrol.domain.model.Acesso;
import br.com.fincontrol.domain.model.AgregadoMes;
import br.com.fincontrol.domain.model.AnaliseFinanceira;
import br.com.fincontrol.domain.model.Categoria;
import br.com.fincontrol.domain.model.Conta;
import br.com.fincontrol.domain.model.FiltroLancamento;
import br.com.fincontrol.domain.model.Lancamento;
import br.com.fincontrol.domain.model.Natureza;
import br.com.fincontrol.domain.model.PedidoAnalise;
import br.com.fincontrol.domain.service.AnaliseFinanceiraPort;
import br.com.fincontrol.domain.service.CategoriaRepositorio;
import br.com.fincontrol.domain.service.ContaRepositorio;
import br.com.fincontrol.domain.service.EscopoTenant;
import br.com.fincontrol.domain.service.LancamentoRepositorio;

@Service
public class AnalisesFinanceiras {

    private static final int CATEGORIAS_NO_PROMPT = 8;
    private static final int CONTAS_NO_PROMPT = 8;

    private final LancamentoRepositorio lancamentos;
    private final ContaRepositorio contas;
    private final CategoriaRepositorio categorias;
    private final AnaliseFinanceiraPort porta;

    public AnalisesFinanceiras(
            LancamentoRepositorio lancamentos,
            ContaRepositorio contas,
            CategoriaRepositorio categorias,
            AnaliseFinanceiraPort porta) {
        this.lancamentos = lancamentos;
        this.contas = contas;
        this.categorias = categorias;
        this.porta = porta;
    }

    public AnaliseFinanceira analisar(Acesso acesso, PedidoAnalise pedido) {
        long clienteId = EscopoTenant.leitura(acesso);
        AgregadoMes agregado = agregar(clienteId, pedido.mes());
        return porta.analisar(pedido, agregado);
    }

    public String provedor() {
        return porta.provedor();
    }

    public String modelo() {
        return porta.modelo();
    }

    public AgregadoMes agregar(long clienteId, YearMonth mes) {
        LocalDate de = mes.atDay(1);
        LocalDate ate = mes.atEndOfMonth();
        List<Lancamento> doMes = lancamentos.listar(clienteId, new FiltroLancamento(null, de, ate)).stream()
                .filter(lancamento -> !lancamento.estornado())
                .toList();
        return new AgregadoMes(
                mes,
                total(doMes, Natureza.RECEITA),
                total(doMes, Natureza.DESPESA),
                doMes.size(),
                porCategoria(clienteId, doMes),
                saldos(clienteId));
    }

    private static BigDecimal total(List<Lancamento> doMes, Natureza natureza) {
        return doMes.stream()
                .filter(lancamento -> lancamento.natureza() == natureza)
                .map(lancamento -> lancamento.valor().valor())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private List<AgregadoMes.LinhaCategoria> porCategoria(long clienteId, List<Lancamento> doMes) {
        Map<Long, String> nomes = categorias.listar(clienteId).stream()
                .collect(Collectors.toMap(Categoria::id, Categoria::nome, (a, b) -> a));
        Map<String, BigDecimal> somas = doMes.stream()
                .filter(lancamento -> lancamento.natureza() == Natureza.DESPESA)
                .collect(Collectors.groupingBy(
                        lancamento -> nome(nomes, lancamento.categoriaId()),
                        Collectors.reducing(BigDecimal.ZERO, lancamento -> lancamento.valor().valor(), BigDecimal::add)));
        return somas.entrySet().stream()
                .map(entrada -> new AgregadoMes.LinhaCategoria(entrada.getKey(), entrada.getValue()))
                .sorted(Comparator.comparing(AgregadoMes.LinhaCategoria::valor).reversed())
                .limit(CATEGORIAS_NO_PROMPT)
                .toList();
    }

    private List<AgregadoMes.LinhaConta> saldos(long clienteId) {
        return contas.listar(clienteId).stream()
                .map(conta -> new AgregadoMes.LinhaConta(conta.nome(), conta.tipo(), saldo(clienteId, conta)))
                .sorted(Comparator.comparing(AgregadoMes.LinhaConta::nome))
                .limit(CONTAS_NO_PROMPT)
                .toList();
    }

    private BigDecimal saldo(long clienteId, Conta conta) {
        return lancamentos.saldo(clienteId, conta.id())
                .map(saldo -> saldo.saldo())
                .orElseGet(() -> conta.saldoInicial().valor());
    }

    private static String nome(Map<Long, String> nomes, Long categoriaId) {
        if (categoriaId == null) {
            return "Sem categoria";
        }
        return nomes.getOrDefault(categoriaId, "Sem categoria");
    }
}
