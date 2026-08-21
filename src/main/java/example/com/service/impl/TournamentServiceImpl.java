package example.com.service.impl;

import example.com.controller.dto.MatchInfoDto;
import example.com.controller.dto.ParticipantInfoDto;
import example.com.controller.dto.RoundInfoDto;
import example.com.controller.dto.TournamentInfoDto;
import example.com.controller.mapper.TournamentMapper;
import example.com.domain.*;
import example.com.domain.exception.ResourceNotFound;
import example.com.repository.MatchRepository;
import example.com.repository.RoundRepository;
import example.com.repository.TournamentRepository;
import example.com.service.TournamentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TournamentServiceImpl implements TournamentService {
    private final TournamentRepository tournamentRepository;
    private final RoundRepository roundRepository;
    private final MatchRepository matchRepository;
    private final TournamentMapper tournamentMapper;

    @Override
    public Tournament createTournament(Tournament tournament) {
        return tournamentRepository.save(tournament);
    }

    @Override
    public List<Tournament> getAllTournaments(Pageable pageable) {
        return tournamentRepository.findAll(pageable).getContent();
    }

    @Override
    public Tournament getById(int id) {
        return tournamentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFound("tournament not found by id"));
    }

    @Override
    @Transactional(readOnly = true)
    public TournamentInfoDto getTournamentInfo(int id) {
        return buildTournamentInfo(getById(id));
    }

    @Override
    @Transactional
    public TournamentInfoDto startTournament(int id) {
        Tournament tournament = getById(id);
        if (tournament.getStatus() != TournamentStatus.DRAFT) {
            throw new IllegalStateException("tournament is not draft yet");
        }
        List<Participant> participants = tournamentRepository.findAllParticipantByTournamentIdRatingDesc(id);
        if (participants.size() < 2) {
            throw new IllegalStateException("tournament participant less than 2");
        }

        int numberOfParticipant = participants.size();
        int bracketSize = nextPowerOfTwo(numberOfParticipant);
        int numberRound = Integer.numberOfTrailingZeros(bracketSize);

        List<Round> rounds = new ArrayList<>(numberRound);
        for (int number = 1; number <= numberRound; number++) {
            Round round = new Round();
            round.setTournament(tournament);
            round.setNumber(number);
            round.setTitle(roundTitle(bracketSize >> number));
            rounds.add(round);
        }
        roundRepository.saveAll(rounds);

        Match finalMatch = new Match();
        finalMatch.setTournament(tournament);
        finalMatch.setRound(rounds.get(numberRound - 1));
        finalMatch.setPosition(0);

        matchRepository.save(finalMatch);

        List<Match> matches = List.of(finalMatch);

        for (int number = numberRound - 1; number >= 1; number--) {
            int count = bracketSize >> number;
            List<Match> current = new ArrayList<>(count);
            for (int position = 0; position < count; position++) {
                Match match = new Match();

                match.setTournament(tournament);
                match.setRound(rounds.get(number - 1));
                match.setPosition(position);

                match.setNextMatch(matches.get(position / 2));
                match.setNextSlot(position % 2 == 0 ? 1 : 2);
                current.add(match);
            }
            matchRepository.saveAll(current);
            matches = current;
        }

        List<Match> firstRound = matches;
        int[] seedOrder = seedOrder(bracketSize);
        for (int slot = 0; slot < bracketSize; slot++) {
            int seed = seedOrder[slot];
            if (seed > numberOfParticipant) {
                continue;
            }
            Participant participant = participants.get(seed - 1);
            Match match = firstRound.get(slot / 2);
            if (slot % 2 == 0) {
                match.setSlot1Participant(participant);
            } else {
                match.setSlot2Participant(participant);
            }
        }

        for (Match match : firstRound) {
            Participant first = match.getSlot1Participant();
            Participant second = match.getSlot2Participant();

            if (first != null && second != null) {
                match.setStatus(MatchStatus.READY);
            } else if (first != null || second != null) {
                match.setBye(true);
                completeMatch(match, first != null ? first : second);
            }
        }

        tournament.setStatus(TournamentStatus.IN_PROGRESS);
        tournament.setStartedAt(LocalDateTime.now());

        return buildTournamentInfo(tournament);
    }

    private TournamentInfoDto buildTournamentInfo(Tournament tournament) {
        int id = tournament.getId();

        List<ParticipantInfoDto> participants =
                tournamentRepository.findAllParticipantByTournamentIdRatingDesc(id).stream()
                        .map(tournamentMapper::toParticipantInfoDto)
                        .toList();

        List<Match> matches = matchRepository.findBracketByTournamentId(id);

        Map<Integer, List<MatchInfoDto>> matchesByRound = new LinkedHashMap<>();
        for (Match match : matches) {
            matchesByRound
                    .computeIfAbsent(match.getRound().getId(), roundId -> new ArrayList<>())
                    .add(tournamentMapper.toMatchInfoDto(match));
        }

        List<RoundInfoDto> rounds = roundRepository.findAllByTournamentIdOrderByNumberAsc(id).stream()
                .map(round -> tournamentMapper.toRoundInfoDto(
                        round, matchesByRound.getOrDefault(round.getId(), List.of())))
                .toList();

        List<MatchInfoDto> history = matches.stream()
                .filter(match -> match.getStatus() == MatchStatus.COMPLETED)
                .sorted(Comparator
                        .comparing(Match::getCompletedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparingInt(match -> match.getRound().getNumber())
                        .thenComparingInt(Match::getPosition))
                .map(tournamentMapper::toMatchInfoDto)
                .toList();

        ParticipantInfoDto champion = matches.stream()
                .filter(match -> match.getNextMatch() == null)
                .map(Match::getWinner)
                .filter(Objects::nonNull)
                .findFirst()
                .map(tournamentMapper::toParticipantInfoDto)
                .orElse(null);

        return new TournamentInfoDto(
                tournament.getId(),
                tournament.getName(),
                tournament.getDiscipline(),
                tournament.getStatus(),
                tournament.getCreatedAt(),
                tournament.getStartedAt(),
                tournament.getFinishedAt(),
                participants.size(),
                rounds.size(),
                matches.size(),
                history.size(),
                champion,
                participants,
                rounds,
                history
        );
    }

    private void completeMatch(Match match, Participant winner) {
        match.setWinner(winner);
        match.setStatus(MatchStatus.COMPLETED);
        match.setCompletedAt(LocalDateTime.now());

        Match nextMatch = match.getNextMatch();
        if (nextMatch == null) {
            return;
        }
        if (match.getNextSlot() == 1) {
            nextMatch.setSlot1Participant(winner);
        } else {
            nextMatch.setSlot2Participant(winner);
        }
        if (nextMatch.getSlot1Participant() != null && nextMatch.getSlot2Participant() != null) {
            nextMatch.setStatus(MatchStatus.READY);
        }
    }

    /**
     * Порядок посева: slot i турнирной сетки занимает участник с номером seedOrder[i].
     * Для 8 участников это [1,8,4,5,2,7,3,6] - первый сеяный встречает последнего,
     * а сойтись первый со вторым могут только в финале.
     */
    private int[] seedOrder(int bracketSize) {
        int[] order = {1};
        while (order.length < bracketSize) {
            int size = order.length * 2;
            int[] next = new int[size];
            for (int i = 0; i < order.length; i++) {
                next[2 * i] = order[i];
                next[2 * i + 1] = size + 1 - order[i];
            }
            order = next;
        }
        return order;
    }

    private int nextPowerOfTwo(int n) {
        int highestBit = Integer.highestOneBit(n);
        return highestBit == n ? n : highestBit << 1;
    }

    private String roundTitle(int matchesInRound) {
        return switch (matchesInRound) {
            case 1 -> "Final";
            case 2 -> "Semifinal";
            default -> "1/" + matchesInRound;
        };
    }
}
