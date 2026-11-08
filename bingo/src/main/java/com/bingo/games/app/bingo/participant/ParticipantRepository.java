package com.bingo.games.app.bingo.participant;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class ParticipantRepository {

    private final JdbcClient jdbc;

    public ParticipantRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Participant> findByTokenHash(String tokenHash) {
        return jdbc.sql("""
                        SELECT id, game_id, name
                          FROM participant
                         WHERE guest_token_hash = :hash
                        """)
                .param("hash", tokenHash)
                .query((rs, rowNum) -> new Participant(
                        rs.getObject("id", UUID.class),
                        rs.getObject("game_id", UUID.class),
                        rs.getString("name")))
                .optional();
    }
}