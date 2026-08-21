CREATE TYPE tournament_status AS ENUM ('DRAFT','IN_PROGRESS','COMPLETED');

CREATE TABLE IF NOT EXISTS tournament
(
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    discipline  VARCHAR(1000) NOT NULL,
    status      tournament_status NOT NULL DEFAULT 'DRAFT',
    created_at  TIMESTAMP                  DEFAULT CURRENT_TIMESTAMP,
    started_at  TIMESTAMP,
    finished_at TIMESTAMP
);