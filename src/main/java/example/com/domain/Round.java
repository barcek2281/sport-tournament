package example.com.domain;

import jakarta.persistence.*;
import lombok.*;

@Table(name = "round")
@Entity
@Setter
@Getter
public class Round {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @Column(name = "number", nullable = false)
    private int number;

    @Column(name = "title", nullable = false, length = 64)
    private String title;
}
