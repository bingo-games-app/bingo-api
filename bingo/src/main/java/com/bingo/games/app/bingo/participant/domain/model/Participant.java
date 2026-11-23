package com.bingo.games.app.bingo.participant.domain.model;

import org.springframework.security.access.AccessDeniedException;

import java.util.UUID;

public record Participant(UUID id, UUID gameId, String name) {

    public void requireGame(UUID requestedGameId) {
        if (!gameId.equals(requestedGameId)) {
            throw new AccessDeniedException("O participante não pertence a este jogo");
        }
    }
}
