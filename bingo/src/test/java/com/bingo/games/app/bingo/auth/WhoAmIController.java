package com.bingo.games.app.bingo.auth;

import com.bingo.games.app.bingo.participant.Participant;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
class WhoAmIController {

    @GetMapping("/api/games/{gameId}/whoami")
    String whoAmI(@PathVariable UUID gameId, @AuthenticationPrincipal Participant me) {
        me.requireGame(gameId);
        return me.name();
    }
}