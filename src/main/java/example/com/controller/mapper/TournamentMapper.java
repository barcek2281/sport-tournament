package example.com.controller.mapper;

import example.com.controller.dto.CreateTournamentRequestDto;
import example.com.controller.dto.CreateTournamentResponseDto;
import example.com.domain.Tournament;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

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
}
