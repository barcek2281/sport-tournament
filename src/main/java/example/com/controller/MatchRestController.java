package example.com.controller;

import example.com.controller.dto.MathResultRequestDto;
import example.com.controller.dto.TournamentInfoDto;
import example.com.service.TournamentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("matches")
@RequiredArgsConstructor
public class MatchRestController {
    private final TournamentService tournamentService;

    @PostMapping("{id}/result")
    public TournamentInfoDto resultMatch(@PathVariable int id, @RequestBody MathResultRequestDto mathResult) {
        return tournamentService.resultMatch(id, mathResult.winnerId());
    }
}
