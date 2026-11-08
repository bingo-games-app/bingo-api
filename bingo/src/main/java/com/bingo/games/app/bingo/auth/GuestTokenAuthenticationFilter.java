package com.bingo.games.app.bingo.auth;

import com.bingo.games.app.bingo.participant.ParticipantRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class GuestTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final GuestTokenService tokens;
    private final ParticipantRepository participants;

    public GuestTokenAuthenticationFilter(GuestTokenService tokens, ParticipantRepository participants) {
        this.tokens = tokens;
        this.participants = participants;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length()).trim();

            participants.findByTokenHash(tokens.hash(token)).ifPresent(participant -> {
                var authentication = UsernamePasswordAuthenticationToken.authenticated(
                        participant, null, List.of(new SimpleGrantedAuthority("ROLE_GUEST")));

                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);
            });
        }

        chain.doFilter(request, response);
    }
}