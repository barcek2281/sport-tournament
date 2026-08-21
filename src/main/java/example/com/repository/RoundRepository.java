package example.com.repository;

import example.com.domain.Round;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoundRepository extends JpaRepository<Round, Integer> {
    List<Round> findAllByTournamentIdOrderByNumberAsc(int tournamentId);
}
