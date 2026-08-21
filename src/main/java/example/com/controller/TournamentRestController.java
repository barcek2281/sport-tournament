package example.com.controller;

import example.com.controller.dto.CreateTournamentRequestDto;
import example.com.controller.dto.CreateTournamentResponseDto;
import example.com.domain.Tournament;
import example.com.service.TournamentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("tournaments")
@Slf4j
@RequiredArgsConstructor
public class TournamentRestController {
    private final TournamentService tournamentService;

    @PostMapping()
    public CreateTournamentResponseDto createTournament(@RequestBody CreateTournamentRequestDto requestDto) {
        Tournament tournament = new Tournament();
        tournament.setName(requestDto.name());
        tournament.setDiscipline(requestDto.discipline());

        Tournament createdTournament = tournamentService.createTournament(tournament);
        return new CreateTournamentResponseDto(
                createdTournament.getId(),
                createdTournament.getName(),
                createdTournament.getDiscipline()
        );
    }
}
