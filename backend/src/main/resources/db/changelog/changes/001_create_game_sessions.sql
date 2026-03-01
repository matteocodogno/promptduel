--liquibase formatted sql

--changeset promptduel:001 labels:schema comment:Create game_sessions table
CREATE TABLE game_sessions
(
    id             UUID         NOT NULL,
    status         VARCHAR(30)  NOT NULL,
    current_round  INTEGER      NOT NULL DEFAULT 1,
    jailbreaker_id UUID,
    guardian_id    UUID,
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL,
    version        INTEGER      NOT NULL DEFAULT 0,
    CONSTRAINT pk_game_sessions PRIMARY KEY (id)
);

--rollback DROP TABLE game_sessions;
