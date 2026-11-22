package br.com.fincontrol.infrastructure.in.web;

import java.net.URI;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validacao(MethodArgumentNotValidException ex, HttpServletRequest req) {
        ProblemDetail problema = base(
                HttpStatus.BAD_REQUEST,
                "Dados inválidos",
                "Revise os campos informados.",
                req,
                ErroCodigo.VALIDACAO);
        problema.setProperty("erros", ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> Map.of("campo", erro.getField(), "mensagem", texto(erro.getDefaultMessage())))
                .toList());
        return problema;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail interno(Exception ex, HttpServletRequest req) {
        return base(
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

    private static ProblemDetail base(
            HttpStatus status, String title, String detail, HttpServletRequest req, ErroCodigo codigo) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detail);
        problema.setTitle(title);
        problema.setType(URI.create("https://fincontrol/errors/" + codigo.name().toLowerCase(Locale.ROOT)));
        problema.setInstance(URI.create(req.getRequestURI()));
        problema.setProperty("codigoErro", codigo.name());
        return problema;
    }
}
