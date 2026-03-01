package com.aiscream.promptduel.application

import com.aiscream.promptduel.domain.GameStatus
import com.aiscream.promptduel.domain.RoundOutcome
import com.aiscream.promptduel.domain.TeamRole
import java.util.UUID

// ── Result types ──────────────────────────────────────────────────────────────

data class CreateSessionResult(
    val gameId: UUID,
    val playerId: UUID,
    val role: TeamRole,
)

data class JoinSessionResult(
    val gameId: UUID,
    val playerId: UUID,
    val role: TeamRole,
)

data class GameSessionSnapshot(
    val gameId: UUID,
    val status: GameStatus,
    val currentRound: Int,
    val myRole: TeamRole,
    val currentSystemPrompt: String,
    val rounds: List<RoundSnapshot>,
)

data class RoundSnapshot(
    val roundNumber: Int,
    val attackVectorName: String,
    val attempts: List<InjectionAttemptSnapshot>,
    val finalSystemPrompt: String?,
)

data class InjectionAttemptSnapshot(
    val attemptNumber: Int,
    val injectionText: String,
    val llmResponse: String,
    val outcome: RoundOutcome,
    val systemPromptVersionNumber: Int,
)

// ── Error types ───────────────────────────────────────────────────────────────

sealed class GameSessionError(
    message: String,
) : Exception(message) {
    data class SessionNotFound(
        val gameId: UUID,
    ) : GameSessionError("Session not found: $gameId")

    data class SessionFull(
        val gameId: UUID,
    ) : GameSessionError("Session is already full: $gameId")

    data class SessionNotWaiting(
        val gameId: UUID,
        val actualStatus: GameStatus,
    ) : GameSessionError("Session $gameId is not waiting for players (status: $actualStatus)")

    data class PlayerNotInSession(
        val gameId: UUID,
        val playerId: UUID,
    ) : GameSessionError("Player $playerId is not part of session $gameId")
}

// ── Service interface ─────────────────────────────────────────────────────────

interface GameService {
    fun createSession(creatorRole: TeamRole): Result<CreateSessionResult>

    fun joinSession(gameId: UUID): Result<JoinSessionResult>

    fun setPlayerReady(
        gameId: UUID,
        playerId: UUID,
    ): Result<Unit>

    fun reconnectPlayer(
        gameId: UUID,
        playerId: UUID,
    ): Result<GameSessionSnapshot>

    fun notifyDisconnection(playerId: UUID)
}
