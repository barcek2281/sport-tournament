package example.com.service.impl;

import example.com.domain.Tournament;
import example.com.repository.TournamentRepository;
import example.com.service.TournamentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TournamentServiceImpl implements TournamentService {
    private final TournamentRepository tournamentRepository;
    @Override
    public Tournament createTournament(Tournament tournament) {
        return tournamentRepository.save(tournament);
    }

    @Override
    public List<Tournament> getAllTournaments(Pageable pageable) {

        return tournamentRepository.findAll(pageable).getContent();
    }
}
