package com.bingo.games.app.bingo.participant.application.controller;

import com.bingo.games.app.bingo.participant.domain.usecase.create.ICreateParticipant;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games")
public class ParticipantController {

    private final ICreateParticipant createParticipant;

    public ParticipantController(ICreateParticipant createParticipant) {
        this.createParticipant = createParticipant;
    }

    @PostMapping("/join")
    public ICreateParticipant.CreateParticipantOutput join(
            @Valid @RequestBody ICreateParticipant.CreateParticipantInput input
    ){
        return createParticipant.execute(input);
    }
}
