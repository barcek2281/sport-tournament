package example.com.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Table(name = "match")
@Entity
@Setter
@Getter
public class Match {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "round_id", nullable = false)
    private Round round;

    private int position;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot1_participant_id")
    private Participant slot1Participant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot2_participant_id")
    private Participant slot2Participant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private Participant winner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "next_match_id")
    private Match nextMatch;

    private Integer nextSlot;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false)
    private MatchStatus status = MatchStatus.PENDING;

    private boolean isBye = false;

    private LocalDateTime completedAt;
}
