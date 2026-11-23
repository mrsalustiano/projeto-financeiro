package br.com.fincontrol.infrastructure.out.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import br.com.fincontrol.domain.service.RefreshOpacoPort;
import br.com.fincontrol.domain.service.Relogio;
import br.com.fincontrol.domain.service.SenhaPort;
import br.com.fincontrol.domain.service.TokenAcessoPort;

@Configuration
public class FabricaJwt {

    @Bean
    Relogio relogio() {
        return new RelogioSistema();
    }

    @Bean
    SenhaPort senha() {
        return new Argon2Senha();
    }

    @Bean
    RefreshOpacoPort refreshOpaco() {
        return new RefreshOpaco();
    }

    @Bean
    TokenAcessoPort tokens(
            @Value("${fincontrol.jwt.private-key-pem:}") String privada,
            @Value("${fincontrol.jwt.public-key-pem:}") String publica,
            Relogio relogio) {
        return new JwtRs256(ChavesJwt.ler(privada, publica), relogio);
    }
}
