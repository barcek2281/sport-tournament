package example.com.controller.dto;

import example.com.domain.MatchStatus;

import java.time.LocalDateTime;

public record MatchInfoDto(
        int id,
        int position,
        MatchStatus status,
        boolean bye,
        ParticipantInfoDto slot1,
        ParticipantInfoDto slot2,
        ParticipantInfoDto winner,
        Integer nextMatchId,
        Integer nextSlot,
        LocalDateTime completedAt
) {
}
