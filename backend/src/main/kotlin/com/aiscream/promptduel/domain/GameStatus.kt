package com.aiscream.promptduel.domain

enum class GameStatus {
    WAITING_FOR_PLAYERS,
    IN_PROGRESS,
    COMPLETED,
}

enum class RoundStatus {
    ACTIVE,
    EVALUATING,
    JAILBREAKER_WIN,
    GUARDIAN_WIN,
}

enum class RoundOutcome {
    JAILBREAKER_WIN,
    GUARDIAN_WIN,
}

enum class TeamRole {
    JAILBREAKER,
    GUARDIAN,
}
