CREATE TYPE match_status AS ENUM ('PENDING', 'READY', 'COMPLETED');

CREATE TABLE match
(
    id                   SERIAL PRIMARY KEY,
    tournament_id        INTEGER     NOT NULL REFERENCES tournament (id) ON DELETE CASCADE,
    round_id             INTEGER     NOT NULL REFERENCES round (id) ON DELETE CASCADE,
    position             INTEGER     NOT NULL,

    slot1_participant_id INTEGER REFERENCES participant (id),
    slot2_participant_id INTEGER REFERENCES participant (id),
    winner_id            INTEGER REFERENCES participant (id),

    next_match_id        INTEGER REFERENCES match (id) ON DELETE CASCADE,
    next_slot            INT,

    status              match_status NOT NULL DEFAULT 'PENDING',
    is_bye               BOOLEAN     NOT NULL DEFAULT FALSE,
    completed_at         TIMESTAMPTZ
);