package example.com.service.impl;

import example.com.controller.dto.MatchInfoDto;
import example.com.controller.dto.ParticipantInfoDto;
import example.com.controller.dto.RoundInfoDto;
import example.com.controller.dto.TournamentInfoDto;
import example.com.controller.mapper.TournamentMapper;
import example.com.controller.mapper.TournamentMapperImpl;
import example.com.domain.Match;
import example.com.domain.MatchStatus;
import example.com.domain.Participant;
import example.com.domain.Round;
import example.com.domain.Tournament;
import example.com.domain.TournamentStatus;
import example.com.repository.MatchRepository;
import example.com.repository.RoundRepository;
import example.com.repository.TournamentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Проверяет построение олимпийской сетки в {@link TournamentServiceImpl#startTournament(int)}.
 * Репозитории замоканы: сохранённые сущности складываются в списки и оттуда же читаются
 * обратно, как это делает Hibernate в рамках одной транзакции.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Построение турнирной сетки")
class TournamentServiceImplBracketTest {

    private static final int TOURNAMENT_ID = 1;
    private static final int PARTICIPANT_ID_BASE = 1000;

    @Mock
    private TournamentRepository tournamentRepository;
    @Mock
    private RoundRepository roundRepository;
    @Mock
    private MatchRepository matchRepository;

    private final TournamentMapper tournamentMapper = new TournamentMapperImpl();

    private TournamentServiceImpl service;
    private Tournament tournament;

    private final List<Round> savedRounds = new ArrayList<>();
    private final List<Match> savedMatches = new ArrayList<>();
    private int idSequence;

    @BeforeEach
    void setUp() {
        service = new TournamentServiceImpl(tournamentRepository, roundRepository, matchRepository, tournamentMapper);

        tournament = new Tournament();
        tournament.setId(TOURNAMENT_ID);
        tournament.setName("Cup");
        tournament.setDiscipline("chess");
        tournament.setStatus(TournamentStatus.DRAFT);
    }

    // --- количество раундов и матчей -------------------------------------------------

    @ParameterizedTest(name = "{0} участников -> сетка на {1}, раундов {2}, матчей {3}")
    @CsvSource({
            " 2,  2, 1,  1",
            " 3,  4, 2,  3",
            " 4,  4, 2,  3",
            " 5,  8, 3,  7",
            " 7,  8, 3,  7",
            " 8,  8, 3,  7",
            " 9, 16, 4, 15",
            "16, 16, 4, 15",
            "17, 32, 5, 31",
    })
    @DisplayName("размер сетки округляется вверх до степени двойки")
    void buildsRoundsAndMatchesForBracketSize(int participantCount, int bracketSize, int rounds, int matches) {
        TournamentInfoDto info = start(participantCount);

        assertThat(info.participantsCount()).isEqualTo(participantCount);
        assertThat(info.roundsCount()).isEqualTo(rounds);
        assertThat(info.matchesCount()).isEqualTo(matches);

        // в раунде с номером r ровно bracketSize / 2^r матчей: 8 -> 4 -> 2 -> 1
        for (RoundInfoDto round : info.rounds()) {
            assertThat(round.matches())
                    .as("матчей в раунде %d (%s)", round.number(), round.title())
                    .hasSize(bracketSize >> round.number());
        }
    }

    @ParameterizedTest(name = "{0} участников")
    @ValueSource(ints = {2, 3, 4, 5, 6, 7, 8, 9, 12, 15, 16, 17, 31, 32, 33})
    @DisplayName("каждый участник попадает в первый раунд ровно один раз")
    void everyParticipantIsPlacedExactlyOnce(int participantCount) {
        TournamentInfoDto info = start(participantCount);

        List<Integer> placed = new ArrayList<>();
        for (MatchInfoDto match : firstRound(info).matches()) {
            if (match.slot1() != null) {
                placed.add(match.slot1().id());
            }
            if (match.slot2() != null) {
                placed.add(match.slot2().id());
            }
        }

        assertThat(placed).doesNotHaveDuplicates();
        assertThat(placed).containsExactlyInAnyOrderElementsOf(expectedParticipantIds(participantCount));
    }

    // --- посев и проходы без игры ----------------------------------------------------

    @ParameterizedTest(name = "{0} участников")
    @ValueSource(ints = {2, 3, 5, 6, 7, 9, 12, 17, 31, 33})
    @DisplayName("проход без игры достаётся сильнейшим по рейтингу")
    void byesGoToTopSeeds(int participantCount) {
        TournamentInfoDto info = start(participantCount);
        int byeCount = nextPowerOfTwo(participantCount) - participantCount;

        List<Integer> byeWinners = firstRound(info).matches().stream()
                .filter(MatchInfoDto::bye)
                .map(match -> match.winner().id())
                .toList();

        assertThat(byeWinners).hasSize(byeCount);
        // сеяные пронумерованы по убыванию рейтинга, поэтому проход получают ровно первые byeCount
        assertThat(byeWinners).containsExactlyInAnyOrderElementsOf(expectedParticipantIds(byeCount));
    }

    @ParameterizedTest(name = "{0} участников")
    @ValueSource(ints = {2, 3, 4, 5, 6, 7, 8, 9, 12, 15, 16, 17, 31, 32, 33})
    @DisplayName("в первом раунде нет матчей с двумя пустыми слотами")
    void noFirstRoundMatchIsCompletelyEmpty(int participantCount) {
        TournamentInfoDto info = start(participantCount);

        // матч без обоих участников навсегда застрял бы в PENDING и заблокировал сетку
        assertThat(firstRound(info).matches())
                .allSatisfy(match -> assertThat(match.slot1() != null || match.slot2() != null)
                        .as("матч #%d в первом раунде пуст", match.id())
                        .isTrue());
    }

    @Test
    @DisplayName("первый и второй сеяные встречаются только в финале")
    void topTwoSeedsMeetOnlyInFinal() {
        TournamentInfoDto info = start(8);

        List<Integer> pathOfFirstSeed = pathToFinal(info, participantId(1));
        List<Integer> pathOfSecondSeed = pathToFinal(info, participantId(2));

        Integer firstCommonMatch = pathOfFirstSeed.stream()
                .filter(pathOfSecondSeed::contains)
                .findFirst()
                .orElseThrow();

        assertThat(firstCommonMatch).isEqualTo(finalMatch(info).id());
    }

    @Test
    @DisplayName("победитель прохода без игры проставлен в следующий раунд")
    void byeWinnerIsAdvancedToNextRound() {
        TournamentInfoDto info = start(5);

        // 5 участников -> сетка на 8, проход получают сеяные 1, 2 и 3
        List<MatchInfoDto> secondRound = info.rounds().get(1).matches();
        List<Integer> advanced = secondRound.stream()
                .flatMap(match -> java.util.stream.Stream.of(match.slot1(), match.slot2()))
                .filter(java.util.Objects::nonNull)
                .map(ParticipantInfoDto::id)
                .toList();

        assertThat(advanced).containsExactlyInAnyOrder(participantId(1), participantId(2), participantId(3));
    }

    // --- связи между матчами ---------------------------------------------------------

    @ParameterizedTest(name = "{0} участников")
    @ValueSource(ints = {2, 3, 5, 8, 9, 16, 17})
    @DisplayName("ссылки nextMatch образуют дерево с единственным финалом")
    void nextMatchLinksFormTree(int participantCount) {
        TournamentInfoDto info = start(participantCount);

        List<MatchInfoDto> withoutNext = allMatches(info).stream()
                .filter(match -> match.nextMatchId() == null)
                .toList();
        assertThat(withoutNext).as("матч без продолжения должен быть один - финал").hasSize(1);

        Map<Integer, MatchInfoDto> byId = indexById(info);
        for (RoundInfoDto round : info.rounds()) {
            for (MatchInfoDto match : round.matches()) {
                if (match.nextMatchId() == null) {
                    continue;
                }
                MatchInfoDto next = byId.get(match.nextMatchId());
                assertThat(next).as("матч #%d ссылается на несуществующий", match.id()).isNotNull();
                assertThat(roundNumberOf(info, next.id()))
                        .as("матч #%d должен вести в следующий раунд", match.id())
                        .isEqualTo(round.number() + 1);
                assertThat(next.position())
                        .as("позиция матча #%d в следующем раунде", match.id())
                        .isEqualTo(match.position() / 2);
                assertThat(match.nextSlot())
                        .as("слот матча #%d в следующем раунде", match.id())
                        .isEqualTo(match.position() % 2 == 0 ? 1 : 2);
            }
        }
    }

    @Test
    @DisplayName("два соседних матча ведут в один и тот же следующий, но в разные слоты")
    void siblingMatchesFeedOppositeSlots() {
        TournamentInfoDto info = start(8);

        List<MatchInfoDto> first = firstRound(info).matches();
        for (int position = 0; position < first.size(); position += 2) {
            MatchInfoDto left = first.get(position);
            MatchInfoDto right = first.get(position + 1);

            assertThat(left.nextMatchId()).isEqualTo(right.nextMatchId());
            assertThat(left.nextSlot()).isEqualTo(1);
            assertThat(right.nextSlot()).isEqualTo(2);
        }
    }

    // --- состояние турнира после старта ----------------------------------------------

    @Test
    @DisplayName("турнир переходит в IN_PROGRESS, чемпион ещё не определён")
    void tournamentBecomesInProgress() {
        TournamentInfoDto info = start(8);

        assertThat(info.status()).isEqualTo(TournamentStatus.IN_PROGRESS);
        assertThat(info.startedAt()).isNotNull();
        assertThat(info.finishedAt()).isNull();
        assertThat(info.champion()).isNull();
    }

    @Test
    @DisplayName("полностью укомплектованные матчи первого раунда готовы к игре")
    void fullFirstRoundMatchesAreReady() {
        TournamentInfoDto info = start(8);

        assertThat(firstRound(info).matches())
                .allSatisfy(match -> assertThat(match.status()).isEqualTo(MatchStatus.READY));
        assertThat(info.completedMatchesCount()).isZero();
    }

    @Test
    @DisplayName("матчи следующих раундов ждут соперников в PENDING")
    void laterRoundsStartPending() {
        TournamentInfoDto info = start(8);

        assertThat(info.rounds().subList(1, info.rounds().size()))
                .flatMap(RoundInfoDto::matches)
                .allSatisfy(match -> {
                    assertThat(match.status()).isEqualTo(MatchStatus.PENDING);
                    assertThat(match.slot1()).isNull();
                    assertThat(match.slot2()).isNull();
                });
    }

    // --- отказы ----------------------------------------------------------------------

    @ParameterizedTest(name = "{0} участник(ов)")
    @ValueSource(ints = {0, 1})
    @DisplayName("турнир меньше чем с двумя участниками не стартует")
    void rejectsTournamentWithLessThanTwoParticipants(int participantCount) {
        givenParticipants(participantCount);

        assertThatThrownBy(() -> service.startTournament(TOURNAMENT_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("tournament participant less than 2");

        verify(matchRepository, never()).save(any());
        verify(roundRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("повторный старт возвращает текущее состояние, не пересоздавая сетку")
    void restartDoesNotRebuildBracket() {
        givenParticipants(4);
        givenReadBackStubs();
        tournament.setStatus(TournamentStatus.IN_PROGRESS);

        TournamentInfoDto info = service.startTournament(TOURNAMENT_ID);

        assertThat(info.status()).isEqualTo(TournamentStatus.IN_PROGRESS);
        assertThat(info.matchesCount()).isZero();
        verify(matchRepository, never()).save(any());
        verify(roundRepository, never()).saveAll(any());
    }

    // --- вспомогательное -------------------------------------------------------------

    private TournamentInfoDto start(int participantCount) {
        givenParticipants(participantCount);
        givenSaveStubs();
        givenReadBackStubs();
        return service.startTournament(TOURNAMENT_ID);
    }

    private void givenParticipants(int count) {
        List<Participant> participants = new ArrayList<>(count);
        for (int seed = 1; seed <= count; seed++) {
            Participant participant = new Participant();
            participant.setId(participantId(seed));
            participant.setName("seed" + seed);
            participant.setRating(3000 - seed);
            participant.setTournament(tournament);
            participants.add(participant);
        }
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(tournament));
        when(tournamentRepository.findAllParticipantByTournamentIdRatingDesc(TOURNAMENT_ID)).thenReturn(participants);
    }

    private void givenSaveStubs() {
        when(roundRepository.saveAll(any())).thenAnswer(invocation -> {
            List<Round> rounds = invocation.getArgument(0);
            rounds.forEach(round -> round.setId(++idSequence));
            savedRounds.addAll(rounds);
            return rounds;
        });
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> {
            Match match = invocation.getArgument(0);
            match.setId(++idSequence);
            savedMatches.add(match);
            return match;
        });
        // lenient: в сетке ровно на двух участников есть только финал, он сохраняется
        // одиночным save(), и до пакетного saveAll() дело не доходит
        lenient().when(matchRepository.saveAll(any())).thenAnswer(invocation -> {
            List<Match> matches = invocation.getArgument(0);
            matches.forEach(match -> match.setId(++idSequence));
            savedMatches.addAll(matches);
            return matches;
        });
    }

    /** Отдаёт назад те же объекты, что были сохранены - в порядке реального запроса. */
    private void givenReadBackStubs() {
        when(matchRepository.findBracketByTournamentId(TOURNAMENT_ID)).thenAnswer(invocation ->
                savedMatches.stream()
                        .sorted(Comparator.comparingInt((Match match) -> match.getRound().getNumber())
                                .thenComparingInt(Match::getPosition))
                        .toList());
        when(roundRepository.findAllByTournamentIdOrderByNumberAsc(TOURNAMENT_ID)).thenAnswer(invocation ->
                savedRounds.stream()
                        .sorted(Comparator.comparingInt(Round::getNumber))
                        .toList());
    }

    private static int participantId(int seed) {
        return PARTICIPANT_ID_BASE + seed;
    }

    private static List<Integer> expectedParticipantIds(int count) {
        List<Integer> ids = new ArrayList<>(count);
        for (int seed = 1; seed <= count; seed++) {
            ids.add(participantId(seed));
        }
        return ids;
    }

    private static int nextPowerOfTwo(int n) {
        int highestBit = Integer.highestOneBit(n);
        return highestBit == n ? n : highestBit << 1;
    }

    private static RoundInfoDto firstRound(TournamentInfoDto info) {
        return info.rounds().getFirst();
    }

    private static List<MatchInfoDto> allMatches(TournamentInfoDto info) {
        return info.rounds().stream().flatMap(round -> round.matches().stream()).toList();
    }

    private static MatchInfoDto finalMatch(TournamentInfoDto info) {
        return allMatches(info).stream()
                .filter(match -> match.nextMatchId() == null)
                .findFirst()
                .orElseThrow();
    }

    private static Map<Integer, MatchInfoDto> indexById(TournamentInfoDto info) {
        Map<Integer, MatchInfoDto> byId = new HashMap<>();
        allMatches(info).forEach(match -> byId.put(match.id(), match));
        return byId;
    }

    private static int roundNumberOf(TournamentInfoDto info, int matchId) {
        return info.rounds().stream()
                .filter(round -> round.matches().stream().anyMatch(match -> match.id() == matchId))
                .map(RoundInfoDto::number)
                .findFirst()
                .orElseThrow();
    }

    /** Цепочка матчей от первого раунда до финала для указанного участника. */
    private static List<Integer> pathToFinal(TournamentInfoDto info, int participantId) {
        MatchInfoDto start = firstRound(info).matches().stream()
                .filter(match -> isIn(match, participantId))
                .findFirst()
                .orElseThrow();

        Map<Integer, MatchInfoDto> byId = indexById(info);
        List<Integer> path = new ArrayList<>();
        Integer current = start.id();
        while (current != null) {
            path.add(current);
            current = byId.get(current).nextMatchId();
        }
        return path;
    }

    private static boolean isIn(MatchInfoDto match, int participantId) {
        return (match.slot1() != null && match.slot1().id() == participantId)
                || (match.slot2() != null && match.slot2().id() == participantId);
    }
}
