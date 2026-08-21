package example.com.service.impl;

import example.com.domain.Participant;
import example.com.domain.Tournament;
import example.com.domain.TournamentStatus;
import example.com.repository.ParticipantRepository;
import example.com.service.ParticipantService;
import example.com.service.TournamentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ParticipantServiceImpl implements ParticipantService {

    private final ParticipantRepository participantRepository;
    private final TournamentService tournamentService;

    @Override
    @Transactional
    public Participant createParticipants(int tournamentId, Participant participant) {
        Tournament tournament = tournamentService.getById(tournamentId);
        if (tournament.getStatus() != TournamentStatus.DRAFT) {
            throw new IllegalStateException("Tournament already has been started or completed.");
        }
        participant.setTournament(tournament);
        return participantRepository.save(participant);
    }
}
