package com.aiscream.promptduel.application

import com.aiscream.promptduel.domain.AttackVectorRegistry
import com.aiscream.promptduel.domain.GameSession
import com.aiscream.promptduel.domain.GameStatus
import com.aiscream.promptduel.domain.InjectionAttempt
import com.aiscream.promptduel.domain.RoundStatus
import com.aiscream.promptduel.domain.SessionStateMachine
import com.aiscream.promptduel.domain.SystemPromptVersion
import com.aiscream.promptduel.domain.TeamRole
import com.aiscream.promptduel.infrastructure.messaging.GameEvent
import com.aiscream.promptduel.infrastructure.messaging.GameEventPublisher
import com.aiscream.promptduel.infrastructure.messaging.GameEventType
import com.aiscream.promptduel.infrastructure.persistence.RoundRepository
import com.aiscream.promptduel.infrastructure.persistence.SessionRepository
import com.aiscream.promptduel.infrastructure.persistence.SystemPromptRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
class DefaultGameService(
    private val sessionStateMachine: SessionStateMachine,
    private val sessionRepository: SessionRepository,
    private val systemPromptRepository: SystemPromptRepository,
    private val roundRepository: RoundRepository,
    private val attackVectorRegistry: AttackVectorRegistry,
    private val eventPublisher: GameEventPublisher,
) : GameService {
    /** Tracks which players have signalled ready per session (in-memory, intentionally not persisted). */
    private val readyPlayers: ConcurrentHashMap<UUID, MutableSet<UUID>> = ConcurrentHashMap()

    override fun createSession(creatorRole: TeamRole): Result<CreateSessionResult> {
        val gameId = UUID.randomUUID()
        val playerId = UUID.randomUUID()
        val now = Instant.now()

        val session =
            GameSession(
                id = gameId,
                status = GameStatus.WAITING_FOR_PLAYERS,
                roundStatus = RoundStatus.ACTIVE,
                currentRound = 1,
                jailbreakerId = if (creatorRole == TeamRole.JAILBREAKER) playerId else null,
                guardianId = if (creatorRole == TeamRole.GUARDIAN) playerId else null,
                version = 0L,
                createdAt = now,
                updatedAt = now,
            )
        sessionRepository.save(session)

        val basePrompt = attackVectorRegistry.getBaseSystemPrompt()
        systemPromptRepository.save(
            SystemPromptVersion(
                id = UUID.randomUUID(),
                gameSessionId = gameId,
                roundNumber = 1,
                versionNumber = 1,
                content = basePrompt,
                createdAt = now,
            ),
        )

        return Result.success(CreateSessionResult(gameId = gameId, playerId = playerId, role = creatorRole))
    }

    override fun joinSession(gameId: UUID): Result<JoinSessionResult> {
        val session =
            sessionRepository.findById(gameId)
                ?: return Result.failure(GameSessionError.SessionNotFound(gameId))

        if (session.status != GameStatus.WAITING_FOR_PLAYERS) {
            return Result.failure(GameSessionError.SessionNotWaiting(gameId, session.status))
        }

        if (session.jailbreakerId != null && session.guardianId != null) {
            return Result.failure(GameSessionError.SessionFull(gameId))
        }

        val joiningRole: TeamRole
        val playerId = UUID.randomUUID()
        val updatedSession =
            if (session.jailbreakerId != null) {
                joiningRole = TeamRole.GUARDIAN
                session.copy(guardianId = playerId, updatedAt = Instant.now())
            } else {
                joiningRole = TeamRole.JAILBREAKER
                session.copy(jailbreakerId = playerId, updatedAt = Instant.now())
            }

        sessionRepository.update(updatedSession)

        eventPublisher.broadcast(
            gameId,
            GameEvent(
                type = GameEventType.PLAYER_JOINED,
                gameId = gameId,
                payload = mapOf("role" to joiningRole.name),
            ),
        )

        return Result.success(JoinSessionResult(gameId = gameId, playerId = playerId, role = joiningRole))
    }

    override fun setPlayerReady(
        gameId: UUID,
        playerId: UUID,
    ): Result<Unit> {
        val session =
            sessionRepository.findById(gameId)
                ?: return Result.failure(GameSessionError.SessionNotFound(gameId))

        if (session.jailbreakerId != playerId && session.guardianId != playerId) {
            return Result.failure(GameSessionError.PlayerNotInSession(gameId, playerId))
        }

        val ready = readyPlayers.getOrPut(gameId) { ConcurrentHashMap.newKeySet() }
        ready.add(playerId)

        val bothReady =
            session.jailbreakerId != null &&
                session.guardianId != null &&
                ready.contains(session.jailbreakerId) &&
                ready.contains(session.guardianId)

        if (bothReady) {
            return sessionStateMachine.startGame(session).map { startedSession ->
                sessionRepository.update(startedSession)
                readyPlayers.remove(gameId)

                val currentPrompt = systemPromptRepository.findLatestByGameSessionId(gameId)
                val attackVector = attackVectorRegistry.getVector(startedSession.currentRound)

                eventPublisher.broadcast(
                    gameId,
                    GameEvent(
                        type = GameEventType.GAME_STARTED,
                        gameId = gameId,
                        payload =
                            mapOf(
                                "currentRound" to startedSession.currentRound,
                                "systemPrompt" to (currentPrompt?.content ?: ""),
                                "attackVectorName" to attackVector.name,
                            ),
                    ),
                )
            }
        }

        return Result.success(Unit)
    }

    override fun reconnectPlayer(
        gameId: UUID,
        playerId: UUID,
    ): Result<GameSessionSnapshot> {
        val session =
            sessionRepository.findById(gameId)
                ?: return Result.failure(GameSessionError.SessionNotFound(gameId))

        val myRole =
            when (playerId) {
                session.jailbreakerId -> TeamRole.JAILBREAKER
                session.guardianId -> TeamRole.GUARDIAN
                else -> return Result.failure(GameSessionError.PlayerNotInSession(gameId, playerId))
            }

        val currentPrompt = systemPromptRepository.findLatestByGameSessionId(gameId)
        val allAttempts = roundRepository.findAllByGameSessionId(gameId)
        val allPromptVersions = systemPromptRepository.findAllByGameSessionId(gameId)

        val snapshot =
            GameSessionSnapshot(
                gameId = gameId,
                status = session.status,
                currentRound = session.currentRound,
                myRole = myRole,
                currentSystemPrompt = currentPrompt?.content ?: "",
                rounds = buildRoundSnapshots(session.currentRound, allAttempts, allPromptVersions),
            )

        eventPublisher.sendToPlayer(
            playerId,
            GameEvent(
                type = GameEventType.SESSION_RESUMED,
                gameId = gameId,
                payload = mapOf("snapshot" to snapshot),
            ),
        )

        eventPublisher.broadcast(
            gameId,
            GameEvent(
                type = GameEventType.PLAYER_RECONNECTED,
                gameId = gameId,
                payload = mapOf("role" to myRole.name),
            ),
        )

        return Result.success(snapshot)
    }

    override fun notifyDisconnection(playerId: UUID) {
        val session = sessionRepository.findByPlayerId(playerId) ?: return

        val role =
            when (playerId) {
                session.jailbreakerId -> TeamRole.JAILBREAKER
                session.guardianId -> TeamRole.GUARDIAN
                else -> return
            }

        eventPublisher.broadcast(
            session.id,
            GameEvent(
                type = GameEventType.PLAYER_DISCONNECTED,
                gameId = session.id,
                payload = mapOf("role" to role.name),
            ),
        )
    }

    private fun buildRoundSnapshots(
        currentRound: Int,
        attempts: List<InjectionAttempt>,
        promptVersions: List<SystemPromptVersion>,
    ): List<RoundSnapshot> {
        val attemptsByRound = attempts.groupBy { it.roundNumber }
        val promptVersionById = promptVersions.associateBy { it.id }
        val latestPromptByRound =
            promptVersions
                .groupBy { it.roundNumber }
                .mapValues { (_, versions) -> versions.maxByOrNull { it.versionNumber }?.content }

        return (1..currentRound).map { round ->
            val roundAttempts = attemptsByRound[round] ?: emptyList()
            val attackVector = attackVectorRegistry.getVector(round)

            RoundSnapshot(
                roundNumber = round,
                attackVectorName = attackVector.name,
                attempts =
                    roundAttempts.map { attempt ->
                        InjectionAttemptSnapshot(
                            attemptNumber = attempt.attemptNumber,
                            injectionText = attempt.injectionText,
                            llmResponse = attempt.llmResponse,
                            outcome = attempt.outcome,
                            systemPromptVersionNumber =
                                promptVersionById[attempt.systemPromptVersionId]?.versionNumber ?: 0,
                        )
                    },
                finalSystemPrompt = latestPromptByRound[round],
            )
        }
    }
}
