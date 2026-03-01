package com.aiscream.promptduel.application

import com.aiscream.promptduel.domain.AttackVectorRegistry
import com.aiscream.promptduel.domain.GameStatus
import com.aiscream.promptduel.domain.InjectionAttempt
import com.aiscream.promptduel.domain.RoundOutcome
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
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class DefaultRoundService(
    private val sessionRepository: SessionRepository,
    private val roundRepository: RoundRepository,
    private val systemPromptRepository: SystemPromptRepository,
    private val evaluationService: EvaluationService,
    private val attackVectorRegistry: AttackVectorRegistry,
    private val eventPublisher: GameEventPublisher,
    private val sessionStateMachine: SessionStateMachine,
) : RoundService {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun submitInjection(
        gameId: UUID,
        playerId: UUID,
        injectionText: String,
    ): Result<InjectionResult> {
        val session =
            sessionRepository.findById(gameId)
                ?: return Result.failure(RoundError.SessionNotFound(gameId))

        if (session.status != GameStatus.IN_PROGRESS) {
            return Result.failure(RoundError.SessionNotInProgress(gameId))
        }

        if (session.jailbreakerId != playerId) {
            return Result.failure(RoundError.WrongRole(gameId, playerId, TeamRole.JAILBREAKER))
        }

        // Transition session to EVALUATING so concurrent submissions are rejected
        val evaluatingSession =
            sessionStateMachine.beginEvaluation(session).getOrElse { return Result.failure(it) }
        sessionRepository.update(evaluatingSession)

        // Load the Guardian's current system prompt and the static vulnerable code
        val currentPrompt =
            systemPromptRepository.findLatestByGameSessionId(gameId)
                ?: return Result.failure(
                    RoundError.InvalidRoundState(gameId, "No system prompt found for session $gameId"),
                )
        val vulnerableCode = attackVectorRegistry.getVulnerableCodeSample()

        // Delegate to EvaluationService
        val evaluation =
            evaluationService
                .evaluate(
                    systemPrompt = currentPrompt.content,
                    vulnerableCode = vulnerableCode,
                    injectionText = injectionText,
                    attackVectorId = session.currentRound,
                ).getOrElse { return Result.failure(it) }

        // Record outcome on session
        val outcomeSess =
            sessionStateMachine
                .recordOutcome(evaluatingSession, evaluation.outcome)
                .getOrElse { return Result.failure(it) }
        sessionRepository.update(outcomeSess)

        // Compute attempt number: count existing attempts in the current round only
        val attemptNumber =
            roundRepository
                .findAllByGameSessionId(gameId)
                .count { it.roundNumber == session.currentRound } + 1

        // Persist the injection attempt
        roundRepository.save(
            InjectionAttempt(
                id = UUID.randomUUID(),
                gameSessionId = gameId,
                roundNumber = session.currentRound,
                attemptNumber = attemptNumber,
                injectionText = injectionText,
                llmResponse = evaluation.llmResponse,
                evaluationMethod = evaluation.evaluationMethod,
                outcome = evaluation.outcome,
                systemPromptVersionId = currentPrompt.id,
                createdAt = Instant.now(),
            ),
        )

        log.debug(
            "Session {}: round {} attempt {} → {}",
            gameId,
            session.currentRound,
            attemptNumber,
            evaluation.outcome,
        )

        return when (evaluation.outcome) {
            RoundOutcome.JAILBREAKER_WIN -> {
                eventPublisher.broadcast(
                    gameId,
                    GameEvent(
                        type = GameEventType.ROUND_ATTEMPT_FAILED,
                        gameId = gameId,
                        payload =
                            mapOf(
                                "injectionText" to injectionText,
                                "llmResponse" to evaluation.llmResponse,
                                "attemptNumber" to attemptNumber,
                                "outcome" to evaluation.outcome.name,
                            ),
                    ),
                )
                Result.success(
                    InjectionResult(
                        outcome = evaluation.outcome,
                        llmResponse = evaluation.llmResponse,
                        attemptNumber = attemptNumber,
                    ),
                )
            }

            RoundOutcome.GUARDIAN_WIN ->
                handleGuardianWin(
                    gameId = gameId,
                    outcomeSess = outcomeSess,
                    attemptNumber = attemptNumber,
                    llmResponse = evaluation.llmResponse,
                    currentPrompt = currentPrompt,
                )
        }
    }

    private fun handleGuardianWin(
        gameId: UUID,
        outcomeSess: com.aiscream.promptduel.domain.GameSession,
        attemptNumber: Int,
        llmResponse: String,
        currentPrompt: SystemPromptVersion,
    ): Result<InjectionResult> {
        val completedRound = outcomeSess.currentRound

        return if (completedRound >= 4) {
            // All 4 rounds defended — complete the game
            val completedSess =
                sessionStateMachine.completeGame(outcomeSess).getOrElse { return Result.failure(it) }
            sessionRepository.update(completedSess)

            eventPublisher.broadcast(
                gameId,
                GameEvent(
                    type = GameEventType.GAME_COMPLETED,
                    gameId = gameId,
                    payload =
                        mapOf(
                            "totalRounds" to 4,
                            "summaryUrl" to "/summary/$gameId",
                        ),
                ),
            )

            log.info("Session {} completed after {} rounds", gameId, completedRound)

            Result.success(InjectionResult(RoundOutcome.GUARDIAN_WIN, llmResponse, attemptNumber))
        } else {
            // Advance to the next round
            val nextRoundSess =
                sessionStateMachine.advanceToNextRound(outcomeSess).getOrElse { return Result.failure(it) }
            sessionRepository.update(nextRoundSess)

            // Carry the Guardian's winning prompt forward as version 1 of the new round
            systemPromptRepository.save(
                SystemPromptVersion(
                    id = UUID.randomUUID(),
                    gameSessionId = gameId,
                    roundNumber = nextRoundSess.currentRound,
                    versionNumber = 1,
                    content = currentPrompt.content,
                    createdAt = Instant.now(),
                ),
            )

            val nextVector = attackVectorRegistry.getVector(nextRoundSess.currentRound)

            eventPublisher.broadcast(
                gameId,
                GameEvent(
                    type = GameEventType.ROUND_COMPLETED,
                    gameId = gameId,
                    payload =
                        mapOf(
                            "roundNumber" to completedRound,
                            "outcome" to RoundOutcome.GUARDIAN_WIN.name,
                            "nextRound" to nextRoundSess.currentRound,
                            "attackVectorName" to nextVector.name,
                            "systemPrompt" to currentPrompt.content,
                        ),
                ),
            )

            log.debug(
                "Session {}: round {} completed → advancing to round {}",
                gameId,
                completedRound,
                nextRoundSess.currentRound,
            )

            Result.success(InjectionResult(RoundOutcome.GUARDIAN_WIN, llmResponse, attemptNumber))
        }
    }

    override fun updateSystemPrompt(
        gameId: UUID,
        playerId: UUID,
        newPrompt: String,
    ): Result<SystemPromptVersion> {
        val session =
            sessionRepository.findById(gameId)
                ?: return Result.failure(RoundError.SessionNotFound(gameId))

        if (session.guardianId != playerId) {
            return Result.failure(RoundError.WrongRole(gameId, playerId, TeamRole.GUARDIAN))
        }

        if (session.roundStatus != RoundStatus.JAILBREAKER_WIN) {
            return Result.failure(
                RoundError.InvalidRoundState(
                    gameId,
                    "Cannot update prompt: round is not in JAILBREAKER_WIN state (actual: ${session.roundStatus})",
                ),
            )
        }

        val latestVersion = systemPromptRepository.findLatestByGameSessionId(gameId)
        val nextVersionNumber = (latestVersion?.versionNumber ?: 0) + 1

        val savedVersion =
            systemPromptRepository.save(
                SystemPromptVersion(
                    id = UUID.randomUUID(),
                    gameSessionId = gameId,
                    roundNumber = session.currentRound,
                    versionNumber = nextVersionNumber,
                    content = newPrompt,
                    createdAt = Instant.now(),
                ),
            )

        // Transition session back to ACTIVE for the next injection attempt
        val activeSess =
            sessionStateMachine.startNextAttempt(session).getOrElse { return Result.failure(it) }
        sessionRepository.update(activeSess)

        eventPublisher.broadcast(
            gameId,
            GameEvent(
                type = GameEventType.PROMPT_UPDATED,
                gameId = gameId,
                payload =
                    mapOf(
                        "systemPrompt" to newPrompt,
                        "versionNumber" to nextVersionNumber,
                    ),
            ),
        )

        log.debug("Session {}: system prompt updated to version {}", gameId, nextVersionNumber)

        return Result.success(savedVersion)
    }
}
