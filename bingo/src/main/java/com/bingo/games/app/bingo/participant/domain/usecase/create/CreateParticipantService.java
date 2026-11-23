package com.bingo.games.app.bingo.participant.domain.usecase.create;


import com.bingo.games.app.bingo.auth.GuestTokenService;
import com.bingo.games.app.bingo.participant.infra.repositories.commands.persist.JdbcPersistParticipantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CreateParticipantService implements ICreateParticipant {


        final GuestTokenService  tokenService;
        final JdbcPersistParticipantRepository repository;

        public CreateParticipantService(
                GuestTokenService tokenService,
                JdbcPersistParticipantRepository repository
        ){
            this.tokenService = tokenService;
            this.repository = repository;
        }

    @Override
    public CreateParticipantOutput execute(CreateParticipantInput input) {
        String token = tokenService.newToken();
        String tokenHash = tokenService.hash(token);

        var participanteId = repository
                .saveGuest(input.gameCode(), input.nickname(), tokenHash)
                .orElseThrow(()-> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Partida não encontrada ou não está aguardando jogadores"
                ));

        return new CreateParticipantOutput(
                participanteId,
                input.nickname(),
                token

        );
    }

}
