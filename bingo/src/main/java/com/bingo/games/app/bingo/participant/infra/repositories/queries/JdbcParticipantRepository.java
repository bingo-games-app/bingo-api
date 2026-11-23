package com.bingo.games.app.bingo.participant.infra.repositories.queries;

import com.bingo.games.app.bingo.participant.domain.model.Participant;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcParticipantRepository {

    private final JdbcClient jdbc;

    public JdbcParticipantRepository(JdbcClient jdbc) {
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
