package com.bingo.games.app.bingo.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@Transactional
class GuestTokenAuthenticationTest {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private JdbcClient jdbc;
    @Autowired
    private GuestTokenService tokens;

    private MockMvc mockMvc;
    private UUID gameId;
    private String token;

    @BeforeEach
    void setUp() {
        // MockMvc com a cadeia de filtros do Spring Security aplicada
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

        UUID ownerId = UUID.randomUUID();
        gameId = UUID.randomUUID();
        token = tokens.newToken();

        jdbc.sql("""
                        INSERT INTO app_user (id, name, email, password_hash)
                        VALUES (:id, 'Dono', :email, 'hash-qualquer')
                        """)
                .param("id", ownerId)
                .param("email", ownerId + "@teste.com")
                .update();

        jdbc.sql("""
                        INSERT INTO game (id, name, owner_id, join_code, mode)
                        VALUES (:id, 'Jogo de teste', :owner, 'TEST01', 'MANUAL')
                        """)
                .param("id", gameId)
                .param("owner", ownerId)
                .update();

        jdbc.sql("""
                        INSERT INTO participant (game_id, name, guest_token_hash)
                        VALUES (:game, 'Maria', :hash)
                        """)
                .param("game", gameId)
                .param("hash", tokens.hash(token))
                .update();
    }

    @Test
    void semToken_retorna401() throws Exception {
        mockMvc.perform(get("/api/games/{id}/whoami", gameId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void tokenInexistente_retorna401() throws Exception {
        mockMvc.perform(get("/api/games/{id}/whoami", gameId)
                        .header("Authorization", "Bearer um-token-que-nao-existe"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void tokenValido_acessaOProprioJogo() throws Exception {
        mockMvc.perform(get("/api/games/{id}/whoami", gameId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("Maria"));
    }

    @Test
    void tokenValido_emOutroJogo_retorna403() throws Exception {
        mockMvc.perform(get("/api/games/{id}/whoami", UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}