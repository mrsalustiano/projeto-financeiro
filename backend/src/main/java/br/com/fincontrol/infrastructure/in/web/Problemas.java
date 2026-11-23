package br.com.fincontrol.infrastructure.in.web;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class Problemas {

    private Problemas() {
    }

    public static ProblemDetail de(
            HttpStatus status, String title, String detail, HttpServletRequest req, ErroCodigo codigo) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detail);
        problema.setTitle(title);
        problema.setType(URI.create("https://fincontrol/errors/" + codigo.name().toLowerCase(Locale.ROOT)));
        problema.setInstance(URI.create(req.getRequestURI()));
        problema.setProperty("codigoErro", codigo.name());
        return problema;
    }

    public static void escrever(HttpServletResponse response, ProblemDetail problema) throws IOException {
        response.setStatus(problema.getStatus());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE + ";charset=UTF-8");
        response.getWriter().write(json(problema));
    }

    private static String json(ProblemDetail problema) {
        StringBuilder corpo = new StringBuilder();
        corpo.append('{');
        texto(corpo, "type", String.valueOf(problema.getType()));
        texto(corpo, "title", problema.getTitle());
        numero(corpo, "status", problema.getStatus());
        texto(corpo, "detail", problema.getDetail());
        texto(corpo, "instance", String.valueOf(problema.getInstance()));
        if (problema.getProperties() != null) {
            problema.getProperties().forEach((chave, valor) -> texto(corpo, chave, String.valueOf(valor)));
        }
        corpo.append('}');
        return corpo.toString();
    }

    private static void texto(StringBuilder corpo, String chave, String valor) {
        if (corpo.length() > 1) {
            corpo.append(',');
        }
        corpo.append('"').append(escapar(chave)).append("\":\"").append(escapar(valor)).append('"');
    }

    private static void numero(StringBuilder corpo, String chave, int valor) {
        if (corpo.length() > 1) {
            corpo.append(',');
        }
        corpo.append('"').append(escapar(chave)).append("\":").append(valor);
    }

    private static String escapar(String valor) {
        return valor.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
