CREATE TABLE round
(
    id            SERIAL PRIMARY KEY,
    tournament_id INTEGER     NOT NULL REFERENCES tournament (id) ON DELETE CASCADE,
    number        INTEGER     NOT NULL,
    title         VARCHAR(64) NOT NULL
);

CREATE INDEX idx_round_tournament ON round (tournament_id);