package com.aiscream.promptduel.domain

import org.springframework.stereotype.Component

object StateMachineRules {
    data class GameState(
        val status: GameStatus,
        val roundStatus: RoundStatus? = null,
    )

    data class Transition(
        val fromState: GameState,
        val toState: GameState,
        val additionalValidation: ((GameSession) -> String?)? = null,
    )

    val validTransitions =
        mapOf<String, Transition>(
            "startGame" to
                Transition(
                    fromState = GameState(GameStatus.WAITING_FOR_PLAYERS),
                    toState = GameState(GameStatus.IN_PROGRESS, RoundStatus.ACTIVE),
                ),
            "beginEvaluation" to
                Transition(
                    fromState = GameState(GameStatus.IN_PROGRESS, RoundStatus.ACTIVE),
                    toState = GameState(GameStatus.IN_PROGRESS, RoundStatus.EVALUATING),
                ),
            "recordOutcomeJailbreakerWin" to
                Transition(
                    fromState = GameState(GameStatus.IN_PROGRESS, RoundStatus.EVALUATING),
                    toState = GameState(GameStatus.IN_PROGRESS, RoundStatus.JAILBREAKER_WIN),
                ),
            "recordOutcomeGuardianWin" to
                Transition(
                    fromState = GameState(GameStatus.IN_PROGRESS, RoundStatus.EVALUATING),
                    toState = GameState(GameStatus.IN_PROGRESS, RoundStatus.GUARDIAN_WIN),
                ),
            "startNextAttempt" to
                Transition(
                    fromState = GameState(GameStatus.IN_PROGRESS, RoundStatus.JAILBREAKER_WIN),
                    toState = GameState(GameStatus.IN_PROGRESS, RoundStatus.ACTIVE),
                ),
            "advanceToNextRound" to
                Transition(
                    fromState = GameState(GameStatus.IN_PROGRESS, RoundStatus.GUARDIAN_WIN),
                    toState = GameState(GameStatus.IN_PROGRESS, RoundStatus.ACTIVE),
                    additionalValidation = { session ->
                        if (session.currentRound >= 4) {
                            "Cannot advance round: already on final round ${session.currentRound}"
                        } else {
                            null
                        }
                    },
                ),
            "completeGame" to
                Transition(
                    fromState = GameState(GameStatus.IN_PROGRESS, RoundStatus.GUARDIAN_WIN),
                    toState = GameState(GameStatus.COMPLETED, RoundStatus.GUARDIAN_WIN),
                    additionalValidation = { session ->
                        if (session.currentRound != 4) {
                            "Cannot complete game: only on round ${session.currentRound}, need round 4"
                        } else {
                            null
                        }
                    },
                ),
        )

    fun validateTransition(
        transitionName: String,
        session: GameSession,
    ): Result<Unit> {
        val transition =
            validTransitions[transitionName]
                ?: return Result.failure(
                    IllegalArgumentException("Unknown transition: $transitionName"),
                )

        val currentState = GameState(session.status, session.roundStatus)

        if (currentState.status != transition.fromState.status) {
            return Result.failure(
                InvalidTransitionException(
                    "Cannot $transitionName: session is ${session.status}, expected ${transition.fromState.status}",
                ),
            )
        }

        if (transition.fromState.roundStatus != null && currentState.roundStatus != transition.fromState.roundStatus) {
            return Result.failure(
                InvalidTransitionException(
                    "Cannot $transitionName: round status is ${session.roundStatus}, expected ${transition.fromState.roundStatus}",
                ),
            )
        }

        transition.additionalValidation?.invoke(session)?.let { errorMessage ->
            return Result.failure(InvalidTransitionException(errorMessage))
        }

        return Result.success(Unit)
    }
}

@Component
class DefaultSessionStateMachine : SessionStateMachine {
    override fun startGame(session: GameSession): Result<GameSession> =
        StateMachineRules
            .validateTransition("startGame", session)
            .map {
                session.copy(
                    status = GameStatus.IN_PROGRESS,
                    currentRound = 1,
                    roundStatus = RoundStatus.ACTIVE,
                )
            }

    override fun beginEvaluation(session: GameSession): Result<GameSession> =
        StateMachineRules
            .validateTransition("beginEvaluation", session)
            .map { session.copy(roundStatus = RoundStatus.EVALUATING) }

    override fun recordOutcome(
        session: GameSession,
        outcome: RoundOutcome,
    ): Result<GameSession> {
        val transitionName =
            when (outcome) {
                RoundOutcome.JAILBREAKER_WIN -> "recordOutcomeJailbreakerWin"
                RoundOutcome.GUARDIAN_WIN -> "recordOutcomeGuardianWin"
            }
        val newRoundStatus =
            when (outcome) {
                RoundOutcome.JAILBREAKER_WIN -> RoundStatus.JAILBREAKER_WIN
                RoundOutcome.GUARDIAN_WIN -> RoundStatus.GUARDIAN_WIN
            }
        return StateMachineRules
            .validateTransition(transitionName, session)
            .map { session.copy(roundStatus = newRoundStatus) }
    }

    override fun startNextAttempt(session: GameSession): Result<GameSession> =
        StateMachineRules
            .validateTransition("startNextAttempt", session)
            .map { session.copy(roundStatus = RoundStatus.ACTIVE) }

    override fun advanceToNextRound(session: GameSession): Result<GameSession> =
        StateMachineRules
            .validateTransition("advanceToNextRound", session)
            .map {
                session.copy(
                    currentRound = session.currentRound + 1,
                    roundStatus = RoundStatus.ACTIVE,
                )
            }

    override fun completeGame(session: GameSession): Result<GameSession> =
        StateMachineRules
            .validateTransition("completeGame", session)
            .map { session.copy(status = GameStatus.COMPLETED) }
}
