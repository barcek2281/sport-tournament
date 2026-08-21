package example.com.controller;

import example.com.controller.dto.CreateParticipantsRequestDto;
import example.com.controller.dto.CreateTournamentRequestDto;
import example.com.controller.dto.CreateTournamentResponseDto;
import example.com.controller.dto.TournamentInfoDto;
import example.com.controller.mapper.TournamentMapper;
import example.com.domain.Participant;
import example.com.domain.Tournament;
import example.com.service.ParticipantService;
import example.com.service.TournamentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("tournaments")
@Slf4j
@RequiredArgsConstructor
public class TournamentRestController {
    private final TournamentService tournamentService;
    private final ParticipantService participantService;
    private final TournamentMapper tournamentMapper;

    @PostMapping()
    public ResponseEntity<CreateTournamentResponseDto> createTournament(@RequestBody CreateTournamentRequestDto requestDto) {
        Tournament tournament = tournamentMapper.toEntity(requestDto);
        Tournament createdTournament = tournamentService.createTournament(tournament);
        return new ResponseEntity<>(tournamentMapper.toCreateResponse(createdTournament), HttpStatus.CREATED);
    }

    @GetMapping
    public List<Tournament> getAllTournaments(@RequestParam(defaultValue = "0", required = false) int page,
                                              @RequestParam(defaultValue = "10", required = false) int size) {
        Pageable pageable = PageRequest.of(page, size);
        return tournamentService.getAllTournaments(pageable);
    }

    @GetMapping("/{id}/bracket")
    public TournamentInfoDto getTournament(@PathVariable int id) {
        return tournamentService.getTournamentInfo(id);
    }

    @PostMapping("/{id}/participants")
    public Participant createParticipant(@PathVariable int id,
                                         @RequestBody CreateParticipantsRequestDto requestDto) {
        Participant participant = new Participant();
        participant.setName(requestDto.name());
        participant.setRating(requestDto.rating());
        return participantService.createParticipants(id, participant);
    }

    @PostMapping("/{id}/start")
    public TournamentInfoDto startTournament(@PathVariable int id) {
        return tournamentService.startTournament(id);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> deleteTournament(@PathVariable int id) {
        tournamentService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
