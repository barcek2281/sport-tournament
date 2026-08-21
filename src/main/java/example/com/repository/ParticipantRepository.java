package example.com.repository;

import example.com.domain.Participant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipantRepository extends JpaRepository<Participant, Integer> {
}
