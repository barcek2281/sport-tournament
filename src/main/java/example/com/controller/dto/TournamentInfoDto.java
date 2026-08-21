package example.com.controller.dto;

import example.com.domain.TournamentStatus;

import java.time.LocalDateTime;
import java.util.List;

public record TournamentInfoDto(
        int id,
        String name,
        String discipline,
        TournamentStatus status,
        LocalDateTime createdAt,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,

        int participantsCount,
        int roundsCount,
        int matchesCount,
        int completedMatchesCount,

        ParticipantInfoDto champion,
        List<ParticipantInfoDto> participants,
        List<RoundInfoDto> rounds,
        List<MatchInfoDto> history
) {
}
