package example.com.controller.mapper;

import example.com.controller.dto.CreateTournamentRequestDto;
import example.com.controller.dto.CreateTournamentResponseDto;
import example.com.controller.dto.MatchInfoDto;
import example.com.controller.dto.ParticipantInfoDto;
import example.com.controller.dto.RoundInfoDto;
import example.com.domain.Match;
import example.com.domain.Participant;
import example.com.domain.Round;
import example.com.domain.Tournament;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface TournamentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "startedAt", ignore = true)
    @Mapping(target = "finishedAt", ignore = true)
    Tournament toEntity(CreateTournamentRequestDto requestDto);

    CreateTournamentResponseDto toCreateResponse(Tournament tournament);

    ParticipantInfoDto toParticipantInfoDto(Participant participant);

    @Mapping(target = "slot1", source = "slot1Participant")
    @Mapping(target = "slot2", source = "slot2Participant")
    @Mapping(target = "nextMatchId", source = "nextMatch.id")
    MatchInfoDto toMatchInfoDto(Match match);

    @Mapping(target = "id", source = "round.id")
    @Mapping(target = "number", source = "round.number")
    @Mapping(target = "title", source = "round.title")
    @Mapping(target = "matches", source = "matches")
    RoundInfoDto toRoundInfoDto(Round round, List<MatchInfoDto> matches);
}
