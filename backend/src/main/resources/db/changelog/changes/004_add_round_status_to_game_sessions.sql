--liquibase formatted sql

--changeset promptduel:004 labels:schema comment:Add round_status column to game_sessions
ALTER TABLE game_sessions
    ADD COLUMN round_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE';

--rollback ALTER TABLE game_sessions DROP COLUMN round_status;
