package example.com.service;

import example.com.domain.Participant;

public interface ParticipantService {
    Participant createParticipants(int tournamentId, Participant participant);
}
