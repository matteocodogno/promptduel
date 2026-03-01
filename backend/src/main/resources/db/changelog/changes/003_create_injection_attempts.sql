--liquibase formatted sql

--changeset promptduel:003 labels:schema comment:Create injection_attempts table
CREATE TABLE injection_attempts
(
    id                       UUID        NOT NULL,
    game_session_id          UUID        NOT NULL,
    round_number             INTEGER     NOT NULL,
    attempt_number           INTEGER     NOT NULL,
    injection_text           TEXT        NOT NULL,
    llm_response             TEXT        NOT NULL,
    evaluation_method        VARCHAR(20) NOT NULL,
    outcome                  VARCHAR(20) NOT NULL,
    system_prompt_version_id UUID        NOT NULL,
    created_at               TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_injection_attempts PRIMARY KEY (id),
    CONSTRAINT fk_ia_game_session FOREIGN KEY (game_session_id)
        REFERENCES game_sessions (id),
    CONSTRAINT fk_ia_system_prompt_version FOREIGN KEY (system_prompt_version_id)
        REFERENCES system_prompt_versions (id),
    CONSTRAINT uq_ia_session_round_attempt
        UNIQUE (game_session_id, round_number, attempt_number)
);

CREATE INDEX idx_injection_attempts_session ON injection_attempts (game_session_id);

--rollback DROP TABLE injection_attempts;
