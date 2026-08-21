package example.com.controller;

import example.com.controller.dto.CreateTournamentRequestDto;
import example.com.controller.dto.CreateTournamentResponseDto;
import example.com.controller.mapper.TournamentMapper;
import example.com.domain.Tournament;
import example.com.service.TournamentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("tournaments")
@Slf4j
@RequiredArgsConstructor
public class TournamentRestController {
    private final TournamentService tournamentService;
    private final TournamentMapper tournamentMapper;

    @PostMapping()
    public CreateTournamentResponseDto createTournament(@RequestBody CreateTournamentRequestDto requestDto) {
        Tournament tournament = tournamentMapper.toEntity(requestDto);
        Tournament createdTournament = tournamentService.createTournament(tournament);
        return tournamentMapper.toCreateResponse(createdTournament);
    }

    @GetMapping
    public List<Tournament> getAllTournaments(@RequestParam int page, @RequestParam int size) {
        Pageable pageable = PageRequest.of(page, size);
        return tournamentService.getAllTournaments(pageable);
    }
}
