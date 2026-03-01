package com.aiscream.promptduel.infrastructure.messaging

import java.util.UUID

interface GameEventPublisher {
    fun broadcast(
        gameId: UUID,
        event: GameEvent,
    )

    fun sendToPlayer(
        playerId: UUID,
        event: GameEvent,
    )
}
