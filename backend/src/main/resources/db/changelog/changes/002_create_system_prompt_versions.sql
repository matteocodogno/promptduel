--liquibase formatted sql

--changeset promptduel:002 labels:schema comment:Create system_prompt_versions table
CREATE TABLE system_prompt_versions
(
    id              UUID        NOT NULL,
    game_session_id UUID        NOT NULL,
    round_number    INTEGER     NOT NULL,
    version_number  INTEGER     NOT NULL,
    content         TEXT        NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_system_prompt_versions PRIMARY KEY (id),
    CONSTRAINT fk_spv_game_session FOREIGN KEY (game_session_id)
        REFERENCES game_sessions (id),
    CONSTRAINT uq_spv_session_round_version
        UNIQUE (game_session_id, round_number, version_number)
);

CREATE INDEX idx_system_prompt_session ON system_prompt_versions (game_session_id);

--rollback DROP TABLE system_prompt_versions;
