--liquibase formatted sql

--changeset promptduel:005 labels:schema comment:Change game_sessions.version from INTEGER to BIGINT for JPA optimistic locking
ALTER TABLE game_sessions ALTER COLUMN version TYPE BIGINT;

--rollback ALTER TABLE game_sessions ALTER COLUMN version TYPE INTEGER;
