package com.bingo.games.app.bingo.participant.infra.repositories.commands.persist;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;


@Repository
public class JdbcPersistParticipantRepository {
    private static final String INSERT_PARTICIPANT_SQL = """
            INSERT INTO participant (game_id, name, guest_token_hash)
            SELECT id, :name, :token_hash
            FROM game
            WHERE join_code = :game_code
              AND status = 'WAITING'
            RETURNING id
            """;

    private final JdbcClient jdbcClient;

    public JdbcPersistParticipantRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Optional<UUID> saveGuest(
      String gameCode,
      String name,
      String tokenHash
    ){
        return jdbcClient.sql(INSERT_PARTICIPANT_SQL)
                .param("game_code", gameCode)
                .param("name",name)
                .param("token_hash", tokenHash)
                .query(UUID.class)
                .optional();
    }


}
