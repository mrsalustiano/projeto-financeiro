package br.com.fincontrol.infrastructure.in.web;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import br.com.fincontrol.domain.service.EmailEmUsoException;
import br.com.fincontrol.domain.service.LoginBloqueadoException;
import br.com.fincontrol.domain.service.NaoAutenticadoException;
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
