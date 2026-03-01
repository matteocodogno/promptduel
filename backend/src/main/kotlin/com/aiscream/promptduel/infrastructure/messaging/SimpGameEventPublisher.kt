package com.aiscream.promptduel.infrastructure.messaging

import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class SimpGameEventPublisher(
    private val messagingTemplate: SimpMessagingTemplate,
) : GameEventPublisher {
    override fun broadcast(
        gameId: UUID,
        event: GameEvent,
    ) {
        messagingTemplate.convertAndSend("/topic/game/$gameId", event)
    }

    override fun sendToPlayer(
        playerId: UUID,
        event: GameEvent,
    ) {
        messagingTemplate.convertAndSendToUser(
            playerId.toString(),
            "/queue/game/${event.gameId}",
            event,
        )
    }
}
