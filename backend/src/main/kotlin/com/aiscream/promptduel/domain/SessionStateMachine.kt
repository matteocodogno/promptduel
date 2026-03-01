package com.aiscream.promptduel.domain

class InvalidTransitionException(
    message: String,
) : Exception(message)

interface SessionStateMachine {
    /** WAITING_FOR_PLAYERS → IN_PROGRESS (round 1, ACTIVE) */
    fun startGame(session: GameSession): Result<GameSession>

    /** IN_PROGRESS + ACTIVE → EVALUATING */
    fun beginEvaluation(session: GameSession): Result<GameSession>

    /** IN_PROGRESS + EVALUATING → JAILBREAKER_WIN | GUARDIAN_WIN */
    fun recordOutcome(
        session: GameSession,
        outcome: RoundOutcome,
    ): Result<GameSession>

    /** JAILBREAKER_WIN → ACTIVE (same round, guardian updated prompt) */
    fun startNextAttempt(session: GameSession): Result<GameSession>

    /** GUARDIAN_WIN + round < 4 → ACTIVE, round + 1 */
    fun advanceToNextRound(session: GameSession): Result<GameSession>

    /** GUARDIAN_WIN + round 4 → COMPLETED */
    fun completeGame(session: GameSession): Result<GameSession>
}
