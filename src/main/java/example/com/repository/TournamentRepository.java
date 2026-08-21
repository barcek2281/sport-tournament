package example.com.repository;

import example.com.domain.Participant;
import example.com.domain.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TournamentRepository extends JpaRepository<Tournament, Integer> {
    @Query("SELECT p FROM Participant p WHERE p.tournament.id = :tournamentId "
            + "ORDER BY p.rating DESC NULLS LAST, p.id ASC")
    List<Participant> findAllParticipantByTournamentIdRatingDesc(@Param("tournamentId") int tournamentId);
}
