package br.com.fincontrol.infrastructure.out.auditoria;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.fincontrol.application.Auditavel;
import br.com.fincontrol.application.ContextoAuditoria;
import br.com.fincontrol.application.RegistrarAuditoria;
import br.com.fincontrol.domain.model.Acesso;
import br.com.fincontrol.domain.model.AcaoAuditoria;
import br.com.fincontrol.domain.model.Diferenca;
import br.com.fincontrol.domain.model.Mascaramento;

@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class AuditoriaAspecto {

    private static final TypeReference<Map<String, Object>> MAPA = new TypeReference<>() {
    };

    private final RegistrarAuditoria registrar;
    private final ObjectMapper json;

    public AuditoriaAspecto(RegistrarAuditoria registrar) {
        this.registrar = registrar;
        this.json = new ObjectMapper().findAndRegisterModules();
    }

    @Around("@annotation(auditavel)")
    public Object auditar(ProceedingJoinPoint junta, Auditavel auditavel) throws Throwable {
        Object resultado;
        try {
            resultado = junta.proceed();
        } catch (Throwable ex) {
            ContextoAuditoria.limpar();
            throw ex;
        }
        Object antes = ContextoAuditoria.consumirAntes();
        registrar.registrar(
                acesso(),
                auditavel.acao(),
                auditavel.recurso(),
                identificador(resultado, antes),
                dadosAntes(auditavel.acao(), antes),
                dadosDepois(auditavel.acao(), antes, resultado));
        return resultado;
    }

    private String dadosAntes(AcaoAuditoria acao, Object antes) {
        if (acao == AcaoAuditoria.CREATE || antes == null) {
            return null;
        }
        return texto(Mascaramento.mascarar(mapa(antes)));
    }

    private String dadosDepois(AcaoAuditoria acao, Object antes, Object resultado) {
        if (acao == AcaoAuditoria.DELETE || resultado == null) {
            return null;
        }
        if (acao == AcaoAuditoria.UPDATE && antes != null) {
            return texto(Mascaramento.mascarar(Diferenca.entre(mapa(antes), mapa(resultado))));
        }
        return texto(mascarado(resultado));
    }

    private Object mascarado(Object valor) {
        if (valor instanceof List<?> lista) {
            return lista.stream().map(this::mapa).map(Mascaramento::mascarar).toList();
        }
        return Mascaramento.mascarar(mapa(valor));
    }

    private Map<String, Object> mapa(Object valor) {
        return json.convertValue(valor, MAPA);
    }

    private String texto(Object valor) {
        try {
            return json.writeValueAsString(valor);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private static Long identificador(Object resultado, Object antes) {
        Long id = id(resultado);
        return id != null ? id : id(antes);
    }

    private static Long id(Object valor) {
        Object alvo = valor instanceof List<?> lista ? (lista.isEmpty() ? null : lista.getFirst()) : valor;
        if (alvo == null) {
            return null;
        }
        try {
            Method metodo = alvo.getClass().getMethod("id");
            return metodo.invoke(alvo) instanceof Number numero ? numero.longValue() : null;
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }

    private static Acesso acesso() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao != null && autenticacao.getPrincipal() instanceof Acesso acesso) {
            return acesso;
        }
        return null;
    }
}
