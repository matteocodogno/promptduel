package com.aiscream.promptduel.application

import com.aiscream.promptduel.domain.AttackVector
import com.aiscream.promptduel.domain.AttackVectorRegistry
import com.aiscream.promptduel.domain.GameSession
import com.aiscream.promptduel.domain.GameStatus
import com.aiscream.promptduel.domain.InjectionAttempt
import com.aiscream.promptduel.domain.RoundStatus
import com.aiscream.promptduel.domain.SessionStateMachine
import com.aiscream.promptduel.domain.TeamRole
import com.aiscream.promptduel.infrastructure.messaging.GameEvent
import com.aiscream.promptduel.infrastructure.messaging.GameEventPublisher
import com.aiscream.promptduel.infrastructure.messaging.GameEventType
import com.aiscream.promptduel.infrastructure.persistence.RoundRepository
import com.aiscream.promptduel.infrastructure.persistence.SessionRepository
import com.aiscream.promptduel.infrastructure.persistence.SystemPromptRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class GameServiceTest {
    @Mock private lateinit var sessionStateMachine: SessionStateMachine

    @Mock private lateinit var sessionRepository: SessionRepository

    @Mock private lateinit var systemPromptRepository: SystemPromptRepository

    @Mock private lateinit var roundRepository: RoundRepository

    @Mock private lateinit var attackVectorRegistry: AttackVectorRegistry

    @Mock private lateinit var eventPublisher: GameEventPublisher

    private lateinit var gameService: DefaultGameService

    @BeforeEach
    fun setUp() {
        gameService =
            DefaultGameService(
                sessionStateMachine,
                sessionRepository,
                systemPromptRepository,
                roundRepository,
                attackVectorRegistry,
                eventPublisher,
            )
    }

    // ── createSession ─────────────────────────────────────────────────────────

    @Test
    fun `createSession with JAILBREAKER role persists session and returns correct result`() {
        whenever(attackVectorRegistry.getBaseSystemPrompt()).thenReturn("You are a code review assistant.")
        whenever(sessionRepository.save(any())).thenAnswer { it.arguments[0] }
        whenever(systemPromptRepository.save(any())).thenAnswer { it.arguments[0] }

        val result = gameService.createSession(TeamRole.JAILBREAKER)

        assertTrue(result.isSuccess)
        assertEquals(TeamRole.JAILBREAKER, result.getOrThrow().role)
        assertNotNull(result.getOrThrow().gameId)
        assertNotNull(result.getOrThrow().playerId)
    }

    @Test
    fun `createSession with GUARDIAN role assigns guardianId to session`() {
        whenever(attackVectorRegistry.getBaseSystemPrompt()).thenReturn("You are a code review assistant.")
        whenever(sessionRepository.save(any())).thenAnswer { it.arguments[0] }
        whenever(systemPromptRepository.save(any())).thenAnswer { it.arguments[0] }

        val result = gameService.createSession(TeamRole.GUARDIAN)

        assertTrue(result.isSuccess)
        assertEquals(TeamRole.GUARDIAN, result.getOrThrow().role)
    }

    @Test
    fun `createSession persists base system prompt as version 1`() {
        whenever(attackVectorRegistry.getBaseSystemPrompt()).thenReturn("base-prompt")
        whenever(sessionRepository.save(any())).thenAnswer { it.arguments[0] }
        whenever(systemPromptRepository.save(any())).thenAnswer { it.arguments[0] }

        gameService.createSession(TeamRole.JAILBREAKER)

        val captor = argumentCaptor<com.aiscream.promptduel.domain.SystemPromptVersion>()
        verify(systemPromptRepository).save(captor.capture())
        assertEquals(1, captor.firstValue.versionNumber)
        assertEquals(1, captor.firstValue.roundNumber)
        assertEquals("base-prompt", captor.firstValue.content)
    }

    @Test
    fun `createSession persists session with WAITING_FOR_PLAYERS status`() {
        whenever(attackVectorRegistry.getBaseSystemPrompt()).thenReturn("base-prompt")
        whenever(sessionRepository.save(any())).thenAnswer { it.arguments[0] }
        whenever(systemPromptRepository.save(any())).thenAnswer { it.arguments[0] }

        gameService.createSession(TeamRole.JAILBREAKER)

        val captor = argumentCaptor<GameSession>()
        verify(sessionRepository).save(captor.capture())
        assertEquals(GameStatus.WAITING_FOR_PLAYERS, captor.firstValue.status)
    }

    // ── joinSession ───────────────────────────────────────────────────────────

    @Test
    fun `joinSession returns failure when session not found`() {
        whenever(sessionRepository.findById(any())).thenReturn(null)

        val result = gameService.joinSession(UUID.randomUUID())

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is GameSessionError.SessionNotFound)
    }

    @Test
    fun `joinSession returns failure when session is not WAITING_FOR_PLAYERS`() {
        val session = aSession(status = GameStatus.IN_PROGRESS)
        whenever(sessionRepository.findById(session.id)).thenReturn(session)

        val result = gameService.joinSession(session.id)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is GameSessionError.SessionNotWaiting)
    }

    @Test
    fun `joinSession returns failure when session is full`() {
        val session =
            aSession(
                jailbreakerId = UUID.randomUUID(),
                guardianId = UUID.randomUUID(),
            )
        whenever(sessionRepository.findById(session.id)).thenReturn(session)

        val result = gameService.joinSession(session.id)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is GameSessionError.SessionFull)
    }

    @Test
    fun `joinSession assigns GUARDIAN role when jailbreaker already present`() {
        val session = aSession(jailbreakerId = UUID.randomUUID(), guardianId = null)
        whenever(sessionRepository.findById(session.id)).thenReturn(session)
        whenever(sessionRepository.update(any())).thenAnswer { it.arguments[0] }

        val result = gameService.joinSession(session.id)

        assertTrue(result.isSuccess)
        assertEquals(TeamRole.GUARDIAN, result.getOrThrow().role)
    }

    @Test
    fun `joinSession assigns JAILBREAKER role when guardian already present`() {
        val session = aSession(jailbreakerId = null, guardianId = UUID.randomUUID())
        whenever(sessionRepository.findById(session.id)).thenReturn(session)
        whenever(sessionRepository.update(any())).thenAnswer { it.arguments[0] }

        val result = gameService.joinSession(session.id)

        assertTrue(result.isSuccess)
        assertEquals(TeamRole.JAILBREAKER, result.getOrThrow().role)
    }

    @Test
    fun `joinSession publishes PLAYER_JOINED event`() {
        val session = aSession(jailbreakerId = UUID.randomUUID(), guardianId = null)
        whenever(sessionRepository.findById(session.id)).thenReturn(session)
        whenever(sessionRepository.update(any())).thenAnswer { it.arguments[0] }

        gameService.joinSession(session.id)

        val captor = argumentCaptor<GameEvent>()
        verify(eventPublisher).broadcast(any(), captor.capture())
        assertEquals(GameEventType.PLAYER_JOINED, captor.firstValue.type)
    }

    // ── setPlayerReady ────────────────────────────────────────────────────────

    @Test
    fun `setPlayerReady returns failure when session not found`() {
        whenever(sessionRepository.findById(any())).thenReturn(null)

        val result = gameService.setPlayerReady(UUID.randomUUID(), UUID.randomUUID())

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is GameSessionError.SessionNotFound)
    }

    @Test
    fun `setPlayerReady returns failure when player is not in the session`() {
        val session = aSession(jailbreakerId = UUID.randomUUID(), guardianId = UUID.randomUUID())
        whenever(sessionRepository.findById(session.id)).thenReturn(session)

        val result = gameService.setPlayerReady(session.id, UUID.randomUUID())

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is GameSessionError.PlayerNotInSession)
    }

    @Test
    fun `setPlayerReady does not start game when only one player is ready`() {
        val jailbreakerId = UUID.randomUUID()
        val session =
            aSession(jailbreakerId = jailbreakerId, guardianId = UUID.randomUUID())
        whenever(sessionRepository.findById(session.id)).thenReturn(session)

        val result = gameService.setPlayerReady(session.id, jailbreakerId)

        assertTrue(result.isSuccess)
        verify(eventPublisher, never()).broadcast(any(), any())
    }

    @Test
    fun `setPlayerReady starts game and broadcasts GAME_STARTED when both players ready`() {
        val jailbreakerId = UUID.randomUUID()
        val guardianId = UUID.randomUUID()
        val session = aSession(jailbreakerId = jailbreakerId, guardianId = guardianId)
        val startedSession =
            session.copy(status = GameStatus.IN_PROGRESS, roundStatus = RoundStatus.ACTIVE)
        val attackVector = anAttackVector()
        val currentPrompt =
            com.aiscream.promptduel.domain.SystemPromptVersion(
                id = UUID.randomUUID(),
                gameSessionId = session.id,
                roundNumber = 1,
                versionNumber = 1,
                content = "base-prompt",
                createdAt = Instant.now(),
            )

        whenever(sessionRepository.findById(session.id)).thenReturn(session)
        whenever(sessionStateMachine.startGame(session)).thenReturn(Result.success(startedSession))
        whenever(sessionRepository.update(startedSession)).thenReturn(startedSession)
        whenever(attackVectorRegistry.getVector(1)).thenReturn(attackVector)
        whenever(systemPromptRepository.findLatestByGameSessionId(session.id)).thenReturn(currentPrompt)

        gameService.setPlayerReady(session.id, jailbreakerId)
        val result = gameService.setPlayerReady(session.id, guardianId)

        assertTrue(result.isSuccess)
        val captor = argumentCaptor<GameEvent>()
        verify(eventPublisher).broadcast(any(), captor.capture())
        assertEquals(GameEventType.GAME_STARTED, captor.firstValue.type)
    }

    // ── reconnectPlayer ───────────────────────────────────────────────────────

    @Test
    fun `reconnectPlayer returns failure when session not found`() {
        whenever(sessionRepository.findById(any())).thenReturn(null)

        val result = gameService.reconnectPlayer(UUID.randomUUID(), UUID.randomUUID())

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is GameSessionError.SessionNotFound)
    }

    @Test
    fun `reconnectPlayer returns failure when player not in session`() {
        val session = aSession(jailbreakerId = UUID.randomUUID(), guardianId = UUID.randomUUID())
        whenever(sessionRepository.findById(session.id)).thenReturn(session)

        val result = gameService.reconnectPlayer(session.id, UUID.randomUUID())

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is GameSessionError.PlayerNotInSession)
    }

    @Test
    fun `reconnectPlayer returns snapshot with JAILBREAKER role for jailbreaker`() {
        val jailbreakerId = UUID.randomUUID()
        val session =
            aSession(status = GameStatus.IN_PROGRESS, jailbreakerId = jailbreakerId, guardianId = UUID.randomUUID())
        val currentPrompt =
            com.aiscream.promptduel.domain.SystemPromptVersion(
                id = UUID.randomUUID(),
                gameSessionId = session.id,
                roundNumber = 1,
                versionNumber = 1,
                content = "current-prompt",
                createdAt = Instant.now(),
            )
        val attackVector = anAttackVector()

        whenever(sessionRepository.findById(session.id)).thenReturn(session)
        whenever(systemPromptRepository.findLatestByGameSessionId(session.id)).thenReturn(currentPrompt)
        whenever(roundRepository.findAllByGameSessionId(session.id)).thenReturn(emptyList<InjectionAttempt>())
        whenever(systemPromptRepository.findAllByGameSessionId(session.id)).thenReturn(listOf(currentPrompt))
        whenever(attackVectorRegistry.getVector(any())).thenReturn(attackVector)

        val result = gameService.reconnectPlayer(session.id, jailbreakerId)

        assertTrue(result.isSuccess)
        assertEquals(TeamRole.JAILBREAKER, result.getOrThrow().myRole)
        assertEquals("current-prompt", result.getOrThrow().currentSystemPrompt)
    }

    @Test
    fun `reconnectPlayer sends SESSION_RESUMED to reconnecting player and broadcasts PLAYER_RECONNECTED`() {
        val jailbreakerId = UUID.randomUUID()
        val guardianId = UUID.randomUUID()
        val session =
            aSession(status = GameStatus.IN_PROGRESS, jailbreakerId = jailbreakerId, guardianId = guardianId)
        val currentPrompt =
            com.aiscream.promptduel.domain.SystemPromptVersion(
                id = UUID.randomUUID(),
                gameSessionId = session.id,
                roundNumber = 1,
                versionNumber = 1,
                content = "current-prompt",
                createdAt = Instant.now(),
            )
        val attackVector = anAttackVector()

        whenever(sessionRepository.findById(session.id)).thenReturn(session)
        whenever(systemPromptRepository.findLatestByGameSessionId(session.id)).thenReturn(currentPrompt)
        whenever(roundRepository.findAllByGameSessionId(session.id)).thenReturn(emptyList<InjectionAttempt>())
        whenever(systemPromptRepository.findAllByGameSessionId(session.id)).thenReturn(listOf(currentPrompt))
        whenever(attackVectorRegistry.getVector(any())).thenReturn(attackVector)

        gameService.reconnectPlayer(session.id, jailbreakerId)

        val sentCaptor = argumentCaptor<GameEvent>()
        verify(eventPublisher).sendToPlayer(any(), sentCaptor.capture())
        assertEquals(GameEventType.SESSION_RESUMED, sentCaptor.firstValue.type)

        val broadcastCaptor = argumentCaptor<GameEvent>()
        verify(eventPublisher).broadcast(any(), broadcastCaptor.capture())
        assertEquals(GameEventType.PLAYER_RECONNECTED, broadcastCaptor.firstValue.type)
    }

    // ── notifyDisconnection ───────────────────────────────────────────────────

    @Test
    fun `notifyDisconnection does nothing when session not found for player`() {
        whenever(sessionRepository.findByPlayerId(any())).thenReturn(null)

        gameService.notifyDisconnection(UUID.randomUUID())

        verify(eventPublisher, never()).broadcast(any(), any())
    }

    @Test
    fun `notifyDisconnection broadcasts PLAYER_DISCONNECTED when session found`() {
        val playerId = UUID.randomUUID()
        val session = aSession(jailbreakerId = playerId, guardianId = UUID.randomUUID())
        whenever(sessionRepository.findByPlayerId(playerId)).thenReturn(session)

        gameService.notifyDisconnection(playerId)

        val captor = argumentCaptor<GameEvent>()
        verify(eventPublisher).broadcast(any(), captor.capture())
        assertEquals(GameEventType.PLAYER_DISCONNECTED, captor.firstValue.type)
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun aSession(
        id: UUID = UUID.randomUUID(),
        status: GameStatus = GameStatus.WAITING_FOR_PLAYERS,
        jailbreakerId: UUID? = null,
        guardianId: UUID? = null,
    ) = GameSession(
        id = id,
        status = status,
        roundStatus = RoundStatus.ACTIVE,
        currentRound = 1,
        jailbreakerId = jailbreakerId,
        guardianId = guardianId,
        version = 0L,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )

    private fun anAttackVector() =
        AttackVector(
            roundNumber = 1,
            name = "Direct Override",
            description = "Attacker adds instructions directly before the code.",
            tier1Hint = "Hint 1",
            tier2HintExample = "Example defensive prompt",
        )
}
