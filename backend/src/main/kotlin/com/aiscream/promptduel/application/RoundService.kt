package com.aiscream.promptduel.application

import com.aiscream.promptduel.domain.RoundOutcome
import com.aiscream.promptduel.domain.SystemPromptVersion
import com.aiscream.promptduel.domain.TeamRole
import java.util.UUID

// ── Result types ──────────────────────────────────────────────────────────────

data class InjectionResult(
    val outcome: RoundOutcome,
    val llmResponse: String,
    val attemptNumber: Int,
)

// ── Error types ───────────────────────────────────────────────────────────────

sealed class RoundError(
    message: String,
) : Exception(message) {
    data class SessionNotFound(
        val gameId: UUID,
    ) : RoundError("Session not found: $gameId")

    data class SessionNotInProgress(
        val gameId: UUID,
    ) : RoundError("Session $gameId is not in progress")

    data class WrongRole(
        val gameId: UUID,
        val playerId: UUID,
        val required: TeamRole,
    ) : RoundError("Player $playerId is not ${required.name} in session $gameId")

    data class InvalidRoundState(
        val gameId: UUID,
        override val message: String,
    ) : RoundError(message)
}

// ── Service interface ─────────────────────────────────────────────────────────

interface RoundService {
    fun submitInjection(
        gameId: UUID,
        playerId: UUID,
        injectionText: String,
    ): Result<InjectionResult>

    fun updateSystemPrompt(
        gameId: UUID,
        playerId: UUID,
        newPrompt: String,
    ): Result<SystemPromptVersion>
}
