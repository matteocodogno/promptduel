package com.aiscream.promptduel.infrastructure.messaging

import java.util.UUID

enum class GameEventType {
    PLAYER_JOINED,
    GAME_STARTED,
    INJECTION_SUBMITTED,
    ROUND_ATTEMPT_FAILED,
    ROUND_COMPLETED,
    PROMPT_UPDATED,
    HINT_RECEIVED,
    GAME_COMPLETED,
    PLAYER_DISCONNECTED,
    PLAYER_RECONNECTED,
    SESSION_RESUMED,
}

data class GameEvent(
    val type: GameEventType,
    val gameId: UUID,
    val payload: Map<String, Any> = emptyMap(),
)
