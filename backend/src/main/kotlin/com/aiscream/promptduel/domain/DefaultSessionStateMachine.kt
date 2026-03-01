package com.aiscream.promptduel.domain

import org.springframework.stereotype.Component

@Component
class DefaultSessionStateMachine : SessionStateMachine {
    override fun startGame(session: GameSession): Result<GameSession> {
        if (session.status != GameStatus.WAITING_FOR_PLAYERS) {
            return Result.failure(
                InvalidTransitionException(
                    "Cannot start game: session is ${session.status}, expected WAITING_FOR_PLAYERS",
                ),
            )
        }
        return Result.success(
            session.copy(
                status = GameStatus.IN_PROGRESS,
                currentRound = 1,
                roundStatus = RoundStatus.ACTIVE,
            ),
        )
    }

    override fun beginEvaluation(session: GameSession): Result<GameSession> {
        if (session.status != GameStatus.IN_PROGRESS) {
            return Result.failure(
                InvalidTransitionException(
                    "Cannot begin evaluation: session is ${session.status}, expected IN_PROGRESS",
                ),
            )
        }
        if (session.roundStatus != RoundStatus.ACTIVE) {
            return Result.failure(
                InvalidTransitionException(
                    "Cannot begin evaluation: round status is ${session.roundStatus}, expected ACTIVE",
                ),
            )
        }
        return Result.success(session.copy(roundStatus = RoundStatus.EVALUATING))
    }

    override fun recordOutcome(
        session: GameSession,
        outcome: RoundOutcome,
    ): Result<GameSession> {
        if (session.status != GameStatus.IN_PROGRESS) {
            return Result.failure(
                InvalidTransitionException(
                    "Cannot record outcome: session is ${session.status}, expected IN_PROGRESS",
                ),
            )
        }
        if (session.roundStatus != RoundStatus.EVALUATING) {
            return Result.failure(
                InvalidTransitionException(
                    "Cannot record outcome: round status is ${session.roundStatus}, expected EVALUATING",
                ),
            )
        }
        val newRoundStatus =
            when (outcome) {
                RoundOutcome.JAILBREAKER_WIN -> RoundStatus.JAILBREAKER_WIN
                RoundOutcome.GUARDIAN_WIN -> RoundStatus.GUARDIAN_WIN
            }
        return Result.success(session.copy(roundStatus = newRoundStatus))
    }

    override fun startNextAttempt(session: GameSession): Result<GameSession> {
        if (session.roundStatus != RoundStatus.JAILBREAKER_WIN) {
            return Result.failure(
                InvalidTransitionException(
                    "Cannot start next attempt: round status is ${session.roundStatus}, expected JAILBREAKER_WIN",
                ),
            )
        }
        return Result.success(session.copy(roundStatus = RoundStatus.ACTIVE))
    }

    override fun advanceToNextRound(session: GameSession): Result<GameSession> {
        if (session.roundStatus != RoundStatus.GUARDIAN_WIN) {
            return Result.failure(
                InvalidTransitionException(
                    "Cannot advance round: round status is ${session.roundStatus}, expected GUARDIAN_WIN",
                ),
            )
        }
        if (session.currentRound >= 4) {
            return Result.failure(
                InvalidTransitionException(
                    "Cannot advance round: already on final round ${session.currentRound}",
                ),
            )
        }
        return Result.success(
            session.copy(
                currentRound = session.currentRound + 1,
                roundStatus = RoundStatus.ACTIVE,
            ),
        )
    }

    override fun completeGame(session: GameSession): Result<GameSession> {
        if (session.status != GameStatus.IN_PROGRESS) {
            return Result.failure(
                InvalidTransitionException(
                    "Cannot complete game: session is ${session.status}, expected IN_PROGRESS",
                ),
            )
        }
        if (session.roundStatus != RoundStatus.GUARDIAN_WIN) {
            return Result.failure(
                InvalidTransitionException(
                    "Cannot complete game: round status is ${session.roundStatus}, expected GUARDIAN_WIN",
                ),
            )
        }
        if (session.currentRound != 4) {
            return Result.failure(
                InvalidTransitionException(
                    "Cannot complete game: only on round ${session.currentRound}, need round 4",
                ),
            )
        }
        return Result.success(session.copy(status = GameStatus.COMPLETED))
    }
}
