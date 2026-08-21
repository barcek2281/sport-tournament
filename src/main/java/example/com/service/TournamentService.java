package example.com.service;

import example.com.domain.Tournament;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TournamentService {
    Tournament createTournament(Tournament tournament);

    List<Tournament> getAllTournaments(Pageable pageable);
}
