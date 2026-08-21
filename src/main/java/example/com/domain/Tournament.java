package example.com.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.sql.Timestamp;

@Table(name = "tournament")
@Entity
@Setter
@Getter
public class Tournament {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "name")
    private String name;
    @Column(name = "discipline")
    private String discipline;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status")
    private TournamentStatus status = TournamentStatus.DRAFT;

    @CreationTimestamp
    private Timestamp createdAt;
    private Timestamp startedAt;
    private Timestamp finishedAt;
}
