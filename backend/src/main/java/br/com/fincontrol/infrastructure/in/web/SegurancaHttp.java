package br.com.fincontrol.infrastructure.in.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SegurancaHttp {

    @Bean
    SecurityFilterChain cadeia(
            HttpSecurity http,
            FiltroDeAcesso filtro,
            ProblemaAutenticacao entrada,
            ProblemaAcesso negado) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable);
        http.sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        autorizar(http);
        http.exceptionHandling(ex -> ex.authenticationEntryPoint(entrada).accessDeniedHandler(negado));
        http.addFilterBefore(filtro, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private static void autorizar(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh_token").permitAll()
                .requestMatchers("/api/v1/master/**").hasRole("MASTER")
                .anyRequest().authenticated());
    }
}
