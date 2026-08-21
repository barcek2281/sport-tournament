package example.com.service.impl;

import example.com.domain.Participant;
import example.com.domain.Tournament;
import example.com.repository.ParticipantRepository;
import example.com.service.ParticipantService;
import example.com.service.TournamentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ParticipantServiceImpl implements ParticipantService {

    private final ParticipantRepository participantRepository;
    private final TournamentService tournamentService;

    @Override
    public Participant createParticipants(int tournamentId, Participant participant) {
        Tournament tournament = tournamentService.getById(tournamentId);
        participant.setTournament(tournament);
        return participantRepository.save(participant);
    }
}
