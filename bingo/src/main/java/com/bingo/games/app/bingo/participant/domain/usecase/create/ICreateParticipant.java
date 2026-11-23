package com.bingo.games.app.bingo.participant.domain.usecase.create;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public interface ICreateParticipant {
    CreateParticipantOutput execute(CreateParticipantInput input);


    public record CreateParticipantInput(
            @NotBlank String gameCode,
            @NotBlank @Size(max = 100) String nickname
    ){}

    public record CreateParticipantOutput(
            UUID participantId,
            String nickname,
            String token
    ){}
}
