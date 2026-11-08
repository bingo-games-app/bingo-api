package com.bingo.games.app.bingo.auth;

import com.bingo.games.app.bingo.participant.ParticipantRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            GuestTokenService tokens,
                                            ParticipantRepository participants) throws Exception {
        http
                // A API usa token no header (não cookie)
                .csrf(csrf -> csrf.disable())

                // Sem sessão: cada requisição se autentica sozinha pelo token.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Rotas públicas só as de registro de conta, login e join.
                // Restante somente autenticado
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/games/join").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/games/by-code/*",
                                "/api/games/*",
                                "/api/games/*/events").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated())

                // Respostas 401 e 403 no formato Error do contrato.
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, e) ->
                                ApiErrors.write(response, 401, "UNAUTHORIZED", "Token ausente ou inválido"))
                        .accessDeniedHandler((request, response, e) ->
                                ApiErrors.write(response, 403, "FORBIDDEN", "Sem permissão para este recurso")))

                // Nosso filtro roda antes do filtro padrão de login por usuário e senha.
                .addFilterBefore(new GuestTokenAuthenticationFilter(tokens, participants),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}