package com.aiscream.promptduel.infrastructure.messaging

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.springframework.messaging.simp.SimpMessagingTemplate
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class SimpGameEventPublisherTest {
    @Mock
    private lateinit var messagingTemplate: SimpMessagingTemplate

    private lateinit var publisher: SimpGameEventPublisher

    @BeforeEach
    fun setUp() {
        publisher = SimpGameEventPublisher(messagingTemplate)
    }

    @Test
    fun `broadcast sends event to topic destination for the given gameId`() {
        val gameId = UUID.fromString("11111111-1111-1111-1111-111111111111")
        val event = GameEvent(type = GameEventType.GAME_STARTED, gameId = gameId)

        publisher.broadcast(gameId, event)

        verify(messagingTemplate).convertAndSend("/topic/game/$gameId", event)
    }

    @Test
    fun `broadcast includes payload in the event sent to the topic`() {
        val gameId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        val event =
            GameEvent(
                type = GameEventType.PLAYER_JOINED,
                gameId = gameId,
                payload = mapOf("role" to "JAILBREAKER"),
            )

        publisher.broadcast(gameId, event)

        verify(messagingTemplate).convertAndSend("/topic/game/$gameId", event)
    }

    @Test
    fun `sendToPlayer sends event to user queue destination with playerId as user`() {
        val gameId = UUID.fromString("22222222-2222-2222-2222-222222222222")
        val playerId = UUID.fromString("33333333-3333-3333-3333-333333333333")
        val event = GameEvent(type = GameEventType.HINT_RECEIVED, gameId = gameId)

        publisher.sendToPlayer(playerId, event)

        verify(messagingTemplate).convertAndSendToUser(
            playerId.toString(),
            "/queue/game/$gameId",
            event,
        )
    }

    @Test
    fun `sendToPlayer uses event gameId for the queue destination`() {
        val gameId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
        val playerId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc")
        val event = GameEvent(type = GameEventType.SESSION_RESUMED, gameId = gameId)

        publisher.sendToPlayer(playerId, event)

        verify(messagingTemplate).convertAndSendToUser(
            playerId.toString(),
            "/queue/game/$gameId",
            event,
        )
    }
}
