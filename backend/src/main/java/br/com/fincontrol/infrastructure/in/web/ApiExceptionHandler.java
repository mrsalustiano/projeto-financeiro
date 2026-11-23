package br.com.fincontrol.infrastructure.in.web;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import br.com.fincontrol.domain.service.AcessoNegadoException;
import br.com.fincontrol.domain.service.ArquivoNaoOfxException;
import br.com.fincontrol.domain.service.EmailEmUsoException;
import br.com.fincontrol.domain.service.EstornoRepetidoException;
import br.com.fincontrol.domain.service.IdempotenciaConflitoException;
import br.com.fincontrol.domain.service.LancamentoEstornadoException;
import br.com.fincontrol.domain.service.LimiteInvalidoException;
import br.com.fincontrol.domain.service.LoginBloqueadoException;
import br.com.fincontrol.domain.service.MesFechadoException;
import br.com.fincontrol.domain.service.NaoAutenticadoException;
import br.com.fincontrol.domain.service.NaoEncontradoException;
import br.com.fincontrol.domain.service.NomeEmUsoException;
import br.com.fincontrol.domain.service.OfxContaDivergenteException;
import br.com.fincontrol.domain.service.OfxSemTransacaoException;
import br.com.fincontrol.domain.service.ParcelaInvalidaException;
import br.com.fincontrol.domain.service.RecorrenciaInativaException;
import br.com.fincontrol.domain.service.TransferenciaInvalidaException;
import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validacao(MethodArgumentNotValidException ex, HttpServletRequest req) {
        ProblemDetail problema = Problemas.de(
                HttpStatus.BAD_REQUEST, "Dados inválidos", "Revise os campos informados.", req, ErroCodigo.VALIDACAO);
        problema.setProperty("erros", ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> Map.of("campo", erro.getField(), "mensagem", texto(erro.getDefaultMessage())))
                .toList());
        return problema;
    }

    @ExceptionHandler(NaoAutenticadoException.class)
    ProblemDetail naoAutenticado(HttpServletRequest req) {
        return Problemas.de(HttpStatus.UNAUTHORIZED, "Não autenticado", "Não autenticado.", req, ErroCodigo.NAO_AUTENTICADO);
    }

    @ExceptionHandler(LoginBloqueadoException.class)
    ProblemDetail loginBloqueado(HttpServletRequest req) {
        return Problemas.de(
                HttpStatus.TOO_MANY_REQUESTS,
                "Login bloqueado",
                "Login bloqueado. Tente novamente mais tarde.",
                req,
                ErroCodigo.LOGIN_BLOQUEADO);
    }

    @ExceptionHandler(EmailEmUsoException.class)
    ProblemDetail emailEmUso(HttpServletRequest req) {
        return Problemas.de(HttpStatus.valueOf(422), "E-mail em uso", "E-mail já cadastrado.", req, ErroCodigo.EMAIL_EM_USO);
    }

    @ExceptionHandler(AcessoNegadoException.class)
    ProblemDetail acessoNegado(HttpServletRequest req) {
        return Problemas.de(HttpStatus.FORBIDDEN, "Acesso negado", "Acesso negado.", req, ErroCodigo.ACESSO_NEGADO);
    }

    @ExceptionHandler(NaoEncontradoException.class)
    ProblemDetail naoEncontrado(HttpServletRequest req) {
        return Problemas.de(HttpStatus.NOT_FOUND, "Não encontrado", "Não encontrado.", req, ErroCodigo.NAO_ENCONTRADO);
    }

    @ExceptionHandler(NomeEmUsoException.class)
    ProblemDetail nomeEmUso(HttpServletRequest req) {
        return Problemas.de(HttpStatus.valueOf(422), "Nome em uso", "Nome já cadastrado.", req, ErroCodigo.NOME_EM_USO);
    }

    @ExceptionHandler(LimiteInvalidoException.class)
    ProblemDetail limiteInvalido(HttpServletRequest req) {
        return Problemas.de(HttpStatus.valueOf(422), "Limite inválido", "Limite inválido para o tipo da conta.", req, ErroCodigo.LIMITE_INVALIDO);
    }

    @ExceptionHandler(MesFechadoException.class)
    ProblemDetail mesFechado(HttpServletRequest req) {
        return Problemas.de(HttpStatus.CONFLICT, "Mês fechado", "Mês fechado para escrita.", req, ErroCodigo.MES_FECHADO);
    }

    @ExceptionHandler(IdempotenciaConflitoException.class)
    ProblemDetail idempotencia(HttpServletRequest req) {
        return Problemas.de(
                HttpStatus.CONFLICT,
                "Idempotência em conflito",
                "Chave de idempotência já usada com outro corpo.",
                req,
                ErroCodigo.IDEMPOTENCIA_CONFLITO);
    }

    @ExceptionHandler(LancamentoEstornadoException.class)
    ProblemDetail lancamentoEstornado(HttpServletRequest req) {
        return Problemas.de(
                HttpStatus.valueOf(422),
                "Lançamento estornado",
                "Lançamento estornado não pode ser alterado.",
                req,
                ErroCodigo.LANCAMENTO_ESTORNADO);
    }

    @ExceptionHandler(EstornoRepetidoException.class)
    ProblemDetail estornoRepetido(HttpServletRequest req) {
        return Problemas.de(HttpStatus.valueOf(422), "Estorno repetido", "Lançamento já estornado.", req, ErroCodigo.ESTORNO_REPETIDO);
    }

    @ExceptionHandler(ParcelaInvalidaException.class)
    ProblemDetail parcelaInvalida(HttpServletRequest req) {
        return Problemas.de(
                HttpStatus.valueOf(422),
                "Parcela inválida",
                "Número de parcelas inválido para o valor informado.",
                req,
                ErroCodigo.PARCELA_INVALIDA);
    }

    @ExceptionHandler(ArquivoNaoOfxException.class)
    ProblemDetail arquivoNaoOfx(HttpServletRequest req) {
        return Problemas.de(
                HttpStatus.valueOf(422),
                "Arquivo não é OFX",
                "Envie um arquivo OFX SGML ou XML.",
                req,
                ErroCodigo.OFX_INVALIDO);
    }

    @ExceptionHandler(OfxSemTransacaoException.class)
    ProblemDetail ofxSemTransacao(HttpServletRequest req) {
        return Problemas.de(
                HttpStatus.valueOf(422),
                "OFX sem transação",
                "O arquivo não traz movimento nem rendimento para importar.",
                req,
                ErroCodigo.OFX_SEM_TRANSACAO);
    }

    @ExceptionHandler(OfxContaDivergenteException.class)
    ProblemDetail ofxContaDivergente(HttpServletRequest req) {
        return Problemas.de(
                HttpStatus.valueOf(422),
                "Arquivo de outro banco",
                "O arquivo pertence a outro banco que não o da conta escolhida.",
                req,
                ErroCodigo.OFX_CONTA_DIVERGENTE);
    }

    @ExceptionHandler(TransferenciaInvalidaException.class)
    ProblemDetail transferenciaInvalida(HttpServletRequest req) {
        return Problemas.de(
                HttpStatus.valueOf(422),
                "Transferência inválida",
                "Conta de origem e destino devem ser diferentes.",
                req,
                ErroCodigo.TRANSFERENCIA_INVALIDA);
    }

    @ExceptionHandler(RecorrenciaInativaException.class)
    ProblemDetail recorrenciaInativa(HttpServletRequest req) {
        return Problemas.de(
                HttpStatus.valueOf(422),
                "Recorrência inativa",
                "Recorrência já está no estado pedido.",
                req,
                ErroCodigo.RECORRENCIA_INATIVA);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail argumentoInvalido(HttpServletRequest req) {
        return Problemas.de(HttpStatus.BAD_REQUEST, "Dados inválidos", "Revise os campos informados.", req, ErroCodigo.VALIDACAO);
    }

    @ExceptionHandler({MissingServletRequestPartException.class, MissingServletRequestParameterException.class})
    ProblemDetail parteAusente(HttpServletRequest req) {
        return Problemas.de(HttpStatus.BAD_REQUEST, "Dados inválidos", "Revise os campos informados.", req, ErroCodigo.VALIDACAO);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail interno(Exception ex, HttpServletRequest req) {
        return Problemas.de(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno",
                "Erro interno. Tente novamente ou contate o suporte.",
                req,
                ErroCodigo.ERRO_INTERNO);
    }

    private static String texto(String mensagem) {
        if (mensagem == null || mensagem.isBlank()) {
            return "Valor inválido.";
        }
        return mensagem;
    }
}
