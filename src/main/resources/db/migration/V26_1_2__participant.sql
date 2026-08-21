CREATE TABLE participant
(
    id            SERIAL PRIMARY KEY,
    tournament_id INTEGER      NOT NULL REFERENCES tournament (id) ON DELETE CASCADE,
    name          VARCHAR(255) NOT NULL,
    rating        INTEGER,
    seed          INTEGER,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);