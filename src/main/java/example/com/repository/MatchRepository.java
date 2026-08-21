package example.com.repository;

import example.com.domain.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MatchRepository extends JpaRepository<Match, Integer> {

    @Query(value = "SELECT m FROM Match m "
            + "JOIN FETCH m.round r "
            + "LEFT JOIN FETCH m.slot1Participant "
            + "LEFT JOIN FETCH m.slot2Participant "
            + "LEFT JOIN FETCH m.winner "
            + "LEFT JOIN FETCH m.nextMatch "
            + "WHERE m.tournament.id = :tournamentId "
            + "ORDER BY r.number ASC, m.position ASC")
    List<Match> findBracketByTournamentId(@Param("tournamentId") int tournamentId);
}
