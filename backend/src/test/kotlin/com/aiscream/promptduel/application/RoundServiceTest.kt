package com.aiscream.promptduel.application

import com.aiscream.promptduel.domain.AttackVector
import com.aiscream.promptduel.domain.AttackVectorRegistry
import com.aiscream.promptduel.domain.EvaluationMethod
import com.aiscream.promptduel.domain.GameSession
import com.aiscream.promptduel.domain.GameStatus
import com.aiscream.promptduel.domain.InjectionAttempt
import com.aiscream.promptduel.domain.RoundOutcome
import com.aiscream.promptduel.domain.RoundStatus
import com.aiscream.promptduel.domain.SessionStateMachine
import com.aiscream.promptduel.domain.SystemPromptVersion
import com.aiscream.promptduel.infrastructure.litellm.LlmError
import com.aiscream.promptduel.infrastructure.messaging.GameEvent
import com.aiscream.promptduel.infrastructure.messaging.GameEventPublisher
import com.aiscream.promptduel.infrastructure.messaging.GameEventType
import com.aiscream.promptduel.infrastructure.persistence.RoundRepository
import com.aiscream.promptduel.infrastructure.persistence.SessionRepository
import com.aiscream.promptduel.infrastructure.persistence.SystemPromptRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class RoundServiceTest {
    @Mock private lateinit var sessionRepository: SessionRepository

    @Mock private lateinit var roundRepository: RoundRepository

    @Mock private lateinit var systemPromptRepository: SystemPromptRepository

    @Mock private lateinit var evaluationService: EvaluationService

    @Mock private lateinit var attackVectorRegistry: AttackVectorRegistry

    @Mock private lateinit var eventPublisher: GameEventPublisher

    @Mock private lateinit var sessionStateMachine: SessionStateMachine

    private lateinit var roundService: DefaultRoundService

    @BeforeEach
    fun setUp() {
        roundService =
            DefaultRoundService(
                sessionRepository,
                roundRepository,
                systemPromptRepository,
                evaluationService,
                attackVectorRegistry,
                eventPublisher,
                sessionStateMachine,
            )
    }

    // ── submitInjection — validation ──────────────────────────────────────────

    @Test
    fun `submitInjection returns failure when session not found`() {
        whenever(sessionRepository.findById(GAME_ID)).thenReturn(null)

        val result = roundService.submitInjection(GAME_ID, JAILBREAKER_ID, "inject")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RoundError.SessionNotFound)
    }

    @Test
    fun `submitInjection returns failure when session is not IN_PROGRESS`() {
        whenever(sessionRepository.findById(GAME_ID)).thenReturn(
            session(status = GameStatus.WAITING_FOR_PLAYERS),
        )

        val result = roundService.submitInjection(GAME_ID, JAILBREAKER_ID, "inject")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RoundError.SessionNotInProgress)
    }

    @Test
    fun `submitInjection returns failure when caller is not JAILBREAKER`() {
        whenever(sessionRepository.findById(GAME_ID)).thenReturn(session())

        val result = roundService.submitInjection(GAME_ID, GUARDIAN_ID, "inject")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RoundError.WrongRole)
    }

    // ── submitInjection — JAILBREAKER_WIN path ────────────────────────────────

    @Test
    fun `submitInjection on JAILBREAKER_WIN persists attempt and broadcasts ROUND_ATTEMPT_FAILED`() {
        stubJailbreakerWinFlow()

        val result = roundService.submitInjection(GAME_ID, JAILBREAKER_ID, "inject text")

        assertTrue(result.isSuccess)
        assertEquals(RoundOutcome.JAILBREAKER_WIN, result.getOrThrow().outcome)

        verify(roundRepository).save(any())

        val eventCaptor = argumentCaptor<GameEvent>()
        verify(eventPublisher).broadcast(eq(GAME_ID), eventCaptor.capture())
        assertEquals(GameEventType.ROUND_ATTEMPT_FAILED, eventCaptor.firstValue.type)
    }

    @Test
    fun `submitInjection ROUND_ATTEMPT_FAILED payload contains injection text, llm response, and attempt number`() {
        stubJailbreakerWinFlow(llmResponse = "bad response", injectionText = "evil inject")

        roundService.submitInjection(GAME_ID, JAILBREAKER_ID, "evil inject")

        val eventCaptor = argumentCaptor<GameEvent>()
        verify(eventPublisher).broadcast(eq(GAME_ID), eventCaptor.capture())
        val payload = eventCaptor.firstValue.payload
        assertEquals("evil inject", payload["injectionText"])
        assertEquals("bad response", payload["llmResponse"])
        assertEquals(1, payload["attemptNumber"])
    }

    // ── submitInjection — GUARDIAN_WIN path ───────────────────────────────────

    @Test
    fun `submitInjection on GUARDIAN_WIN round less than 4 advances round and broadcasts ROUND_COMPLETED`() {
        stubGuardianWinFlowRoundAdvance(currentRound = 1)

        val result = roundService.submitInjection(GAME_ID, JAILBREAKER_ID, "inject text")

        assertTrue(result.isSuccess)
        assertEquals(RoundOutcome.GUARDIAN_WIN, result.getOrThrow().outcome)

        val eventCaptor = argumentCaptor<GameEvent>()
        verify(eventPublisher).broadcast(eq(GAME_ID), eventCaptor.capture())
        assertEquals(GameEventType.ROUND_COMPLETED, eventCaptor.firstValue.type)
    }

    @Test
    fun `submitInjection ROUND_COMPLETED payload contains round number, outcome, and next round`() {
        stubGuardianWinFlowRoundAdvance(currentRound = 1)

        roundService.submitInjection(GAME_ID, JAILBREAKER_ID, "inject text")

        val eventCaptor = argumentCaptor<GameEvent>()
        verify(eventPublisher).broadcast(eq(GAME_ID), eventCaptor.capture())
        val payload = eventCaptor.firstValue.payload
        assertEquals(1, payload["roundNumber"])
        assertEquals(RoundOutcome.GUARDIAN_WIN.name, payload["outcome"])
        assertEquals(2, payload["nextRound"])
    }

    @Test
    fun `submitInjection on GUARDIAN_WIN round 4 completes game and broadcasts GAME_COMPLETED`() {
        stubGuardianWinFlowGameComplete()

        val result = roundService.submitInjection(GAME_ID, JAILBREAKER_ID, "inject text")

        assertTrue(result.isSuccess)
        assertEquals(RoundOutcome.GUARDIAN_WIN, result.getOrThrow().outcome)

        val eventCaptor = argumentCaptor<GameEvent>()
        verify(eventPublisher).broadcast(eq(GAME_ID), eventCaptor.capture())
        assertEquals(GameEventType.GAME_COMPLETED, eventCaptor.firstValue.type)
        val summaryUrl = eventCaptor.firstValue.payload["summaryUrl"] as String
        assertTrue(summaryUrl.contains(GAME_ID.toString()))
    }

    // ── submitInjection — attempt number ──────────────────────────────────────

    @Test
    fun `submitInjection attempt number is 1 when no previous attempts in round`() {
        stubJailbreakerWinFlow()
        whenever(roundRepository.findAllByGameSessionId(GAME_ID)).thenReturn(emptyList())

        val savedCaptor = argumentCaptor<InjectionAttempt>()
        whenever(roundRepository.save(savedCaptor.capture())).thenAnswer { savedCaptor.lastValue }

        roundService.submitInjection(GAME_ID, JAILBREAKER_ID, "inject text")

        assertEquals(1, savedCaptor.firstValue.attemptNumber)
    }

    @Test
    fun `submitInjection attempt number increments based on existing attempts in the same round`() {
        stubJailbreakerWinFlow()
        whenever(roundRepository.findAllByGameSessionId(GAME_ID)).thenReturn(
            listOf(
                injectionAttempt(roundNumber = 1, attemptNumber = 1),
                injectionAttempt(roundNumber = 1, attemptNumber = 2),
            ),
        )

        val savedCaptor = argumentCaptor<InjectionAttempt>()
        whenever(roundRepository.save(savedCaptor.capture())).thenAnswer { savedCaptor.lastValue }

        roundService.submitInjection(GAME_ID, JAILBREAKER_ID, "inject text")

        assertEquals(3, savedCaptor.firstValue.attemptNumber)
    }

    @Test
    fun `submitInjection counts only attempts from the current round`() {
        stubJailbreakerWinFlow()
        // 2 attempts from round 1, 1 attempt from previous round 2 (different round)
        whenever(roundRepository.findAllByGameSessionId(GAME_ID)).thenReturn(
            listOf(
                injectionAttempt(roundNumber = 1, attemptNumber = 1),
                injectionAttempt(roundNumber = 2, attemptNumber = 1),
            ),
        )

        val savedCaptor = argumentCaptor<InjectionAttempt>()
        whenever(roundRepository.save(savedCaptor.capture())).thenAnswer { savedCaptor.lastValue }

        roundService.submitInjection(GAME_ID, JAILBREAKER_ID, "inject text")

        // current round is 1, only 1 existing attempt in round 1 → next is 2
        assertEquals(2, savedCaptor.firstValue.attemptNumber)
    }

    // ── submitInjection — prompt and code usage ───────────────────────────────

    @Test
    fun `submitInjection uses latest system prompt for evaluation`() {
        val customPrompt = promptVersion(content = "my guardian prompt")
        val sess = session()
        val evaluatingSess = sess.copy(roundStatus = RoundStatus.EVALUATING)
        val jailbreakerWinSess = evaluatingSess.copy(roundStatus = RoundStatus.JAILBREAKER_WIN)
        whenever(sessionRepository.findById(GAME_ID)).thenReturn(sess)
        whenever(sessionStateMachine.beginEvaluation(sess)).thenReturn(Result.success(evaluatingSess))
        whenever(sessionRepository.update(any())).thenAnswer { it.arguments[0] }
        whenever(systemPromptRepository.findLatestByGameSessionId(GAME_ID)).thenReturn(customPrompt)
        whenever(attackVectorRegistry.getVulnerableCodeSample()).thenReturn("vuln code")
        val sysCaptor = argumentCaptor<String>()
        whenever(evaluationService.evaluate(sysCaptor.capture(), any(), any(), any())).thenReturn(
            Result.success(EvaluationResult(RoundOutcome.JAILBREAKER_WIN, "resp", EvaluationMethod.PATTERN_MATCH)),
        )
        whenever(sessionStateMachine.recordOutcome(evaluatingSess, RoundOutcome.JAILBREAKER_WIN))
            .thenReturn(Result.success(jailbreakerWinSess))
        whenever(roundRepository.findAllByGameSessionId(GAME_ID)).thenReturn(emptyList())
        whenever(roundRepository.save(any())).thenAnswer { it.arguments[0] }

        roundService.submitInjection(GAME_ID, JAILBREAKER_ID, "inject text")

        assertEquals("my guardian prompt", sysCaptor.firstValue)
    }

    @Test
    fun `submitInjection uses vulnerable code sample from registry`() {
        val sess = session()
        val evaluatingSess = sess.copy(roundStatus = RoundStatus.EVALUATING)
        val jailbreakerWinSess = evaluatingSess.copy(roundStatus = RoundStatus.JAILBREAKER_WIN)
        whenever(sessionRepository.findById(GAME_ID)).thenReturn(sess)
        whenever(sessionStateMachine.beginEvaluation(sess)).thenReturn(Result.success(evaluatingSess))
        whenever(sessionRepository.update(any())).thenAnswer { it.arguments[0] }
        whenever(systemPromptRepository.findLatestByGameSessionId(GAME_ID)).thenReturn(promptVersion())
        whenever(attackVectorRegistry.getVulnerableCodeSample()).thenReturn("SELECT * FROM users")
        val codeCaptor = argumentCaptor<String>()
        whenever(evaluationService.evaluate(any(), codeCaptor.capture(), any(), any())).thenReturn(
            Result.success(EvaluationResult(RoundOutcome.JAILBREAKER_WIN, "resp", EvaluationMethod.PATTERN_MATCH)),
        )
        whenever(sessionStateMachine.recordOutcome(evaluatingSess, RoundOutcome.JAILBREAKER_WIN))
            .thenReturn(Result.success(jailbreakerWinSess))
        whenever(roundRepository.findAllByGameSessionId(GAME_ID)).thenReturn(emptyList())
        whenever(roundRepository.save(any())).thenAnswer { it.arguments[0] }

        roundService.submitInjection(GAME_ID, JAILBREAKER_ID, "inject text")

        assertEquals("SELECT * FROM users", codeCaptor.firstValue)
    }

    // ── submitInjection — error propagation ───────────────────────────────────

    @Test
    fun `submitInjection propagates LLM failure`() {
        val sess = session()
        val evaluatingSess = sess.copy(roundStatus = RoundStatus.EVALUATING)
        whenever(sessionRepository.findById(GAME_ID)).thenReturn(sess)
        whenever(sessionStateMachine.beginEvaluation(sess)).thenReturn(Result.success(evaluatingSess))
        whenever(sessionRepository.update(any())).thenAnswer { it.arguments[0] }
        whenever(systemPromptRepository.findLatestByGameSessionId(GAME_ID)).thenReturn(promptVersion())
        whenever(attackVectorRegistry.getVulnerableCodeSample()).thenReturn("code")
        whenever(evaluationService.evaluate(any(), any(), any(), any())).thenReturn(
            Result.failure(LlmError.Timeout(30_000L)),
        )

        val result = roundService.submitInjection(GAME_ID, JAILBREAKER_ID, "inject text")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is LlmError.Timeout)
    }

    // ── updateSystemPrompt — validation ───────────────────────────────────────

    @Test
    fun `updateSystemPrompt returns failure when session not found`() {
        whenever(sessionRepository.findById(GAME_ID)).thenReturn(null)

        val result = roundService.updateSystemPrompt(GAME_ID, GUARDIAN_ID, "new prompt")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RoundError.SessionNotFound)
    }

    @Test
    fun `updateSystemPrompt returns failure when caller is not GUARDIAN`() {
        whenever(sessionRepository.findById(GAME_ID)).thenReturn(session(roundStatus = RoundStatus.JAILBREAKER_WIN))

        val result = roundService.updateSystemPrompt(GAME_ID, JAILBREAKER_ID, "new prompt")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RoundError.WrongRole)
    }

    @Test
    fun `updateSystemPrompt returns failure when round is not JAILBREAKER_WIN`() {
        whenever(sessionRepository.findById(GAME_ID)).thenReturn(session(roundStatus = RoundStatus.ACTIVE))

        val result = roundService.updateSystemPrompt(GAME_ID, GUARDIAN_ID, "new prompt")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RoundError.InvalidRoundState)
    }

    // ── updateSystemPrompt — happy path ───────────────────────────────────────

    @Test
    fun `updateSystemPrompt persists new version with incremented version number`() {
        val sess = session(roundStatus = RoundStatus.JAILBREAKER_WIN)
        val activeSess = sess.copy(roundStatus = RoundStatus.ACTIVE)
        whenever(sessionRepository.findById(GAME_ID)).thenReturn(sess)
        whenever(systemPromptRepository.findLatestByGameSessionId(GAME_ID)).thenReturn(promptVersion(versionNumber = 2))
        val savedCaptor = argumentCaptor<SystemPromptVersion>()
        whenever(systemPromptRepository.save(savedCaptor.capture())).thenAnswer { savedCaptor.lastValue }
        whenever(sessionStateMachine.startNextAttempt(sess)).thenReturn(Result.success(activeSess))
        whenever(sessionRepository.update(activeSess)).thenReturn(activeSess)

        roundService.updateSystemPrompt(GAME_ID, GUARDIAN_ID, "new content")

        assertEquals(3, savedCaptor.firstValue.versionNumber)
        assertEquals("new content", savedCaptor.firstValue.content)
    }

    @Test
    fun `updateSystemPrompt broadcasts PROMPT_UPDATED with new prompt and version number`() {
        val sess = session(roundStatus = RoundStatus.JAILBREAKER_WIN)
        val activeSess = sess.copy(roundStatus = RoundStatus.ACTIVE)
        whenever(sessionRepository.findById(GAME_ID)).thenReturn(sess)
        whenever(systemPromptRepository.findLatestByGameSessionId(GAME_ID)).thenReturn(promptVersion(versionNumber = 1))
        whenever(systemPromptRepository.save(any())).thenAnswer { it.arguments[0] }
        whenever(sessionStateMachine.startNextAttempt(sess)).thenReturn(Result.success(activeSess))
        whenever(sessionRepository.update(activeSess)).thenReturn(activeSess)

        roundService.updateSystemPrompt(GAME_ID, GUARDIAN_ID, "updated prompt content")

        val eventCaptor = argumentCaptor<GameEvent>()
        verify(eventPublisher).broadcast(eq(GAME_ID), eventCaptor.capture())
        assertEquals(GameEventType.PROMPT_UPDATED, eventCaptor.firstValue.type)
        assertEquals("updated prompt content", eventCaptor.firstValue.payload["systemPrompt"])
        assertEquals(2, eventCaptor.firstValue.payload["versionNumber"])
    }

    @Test
    fun `updateSystemPrompt returns the saved system prompt version`() {
        val sess = session(roundStatus = RoundStatus.JAILBREAKER_WIN)
        val activeSess = sess.copy(roundStatus = RoundStatus.ACTIVE)
        whenever(sessionRepository.findById(GAME_ID)).thenReturn(sess)
        whenever(systemPromptRepository.findLatestByGameSessionId(GAME_ID)).thenReturn(promptVersion(versionNumber = 1))
        whenever(systemPromptRepository.save(any())).thenAnswer { it.arguments[0] }
        whenever(sessionStateMachine.startNextAttempt(sess)).thenReturn(Result.success(activeSess))
        whenever(sessionRepository.update(activeSess)).thenReturn(activeSess)

        val result = roundService.updateSystemPrompt(GAME_ID, GUARDIAN_ID, "final content")

        assertTrue(result.isSuccess)
        assertEquals("final content", result.getOrThrow().content)
        assertEquals(2, result.getOrThrow().versionNumber)
    }

    @Test
    fun `updateSystemPrompt transitions session back to ACTIVE`() {
        val sess = session(roundStatus = RoundStatus.JAILBREAKER_WIN)
        val activeSess = sess.copy(roundStatus = RoundStatus.ACTIVE)
        whenever(sessionRepository.findById(GAME_ID)).thenReturn(sess)
        whenever(systemPromptRepository.findLatestByGameSessionId(GAME_ID)).thenReturn(promptVersion(versionNumber = 1))
        whenever(systemPromptRepository.save(any())).thenAnswer { it.arguments[0] }
        whenever(sessionStateMachine.startNextAttempt(sess)).thenReturn(Result.success(activeSess))
        whenever(sessionRepository.update(activeSess)).thenReturn(activeSess)

        roundService.updateSystemPrompt(GAME_ID, GUARDIAN_ID, "new prompt")

        verify(sessionStateMachine).startNextAttempt(sess)
        verify(sessionRepository).update(activeSess)
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /** Stubs the full JAILBREAKER_WIN flow for submitInjection. */
    private fun stubJailbreakerWinFlow(
        llmResponse: String = "bad response",
        injectionText: String = "inject text",
    ) {
        val sess = session()
        val evaluatingSess = sess.copy(roundStatus = RoundStatus.EVALUATING)
        val jailbreakerWinSess = evaluatingSess.copy(roundStatus = RoundStatus.JAILBREAKER_WIN)
        whenever(sessionRepository.findById(GAME_ID)).thenReturn(sess)
        whenever(sessionStateMachine.beginEvaluation(sess)).thenReturn(Result.success(evaluatingSess))
        whenever(sessionRepository.update(any())).thenAnswer { it.arguments[0] }
        whenever(systemPromptRepository.findLatestByGameSessionId(GAME_ID)).thenReturn(promptVersion())
        whenever(attackVectorRegistry.getVulnerableCodeSample()).thenReturn("vuln code")
        whenever(evaluationService.evaluate(any(), any(), eq(injectionText), any())).thenReturn(
            Result.success(EvaluationResult(RoundOutcome.JAILBREAKER_WIN, llmResponse, EvaluationMethod.PATTERN_MATCH)),
        )
        whenever(sessionStateMachine.recordOutcome(evaluatingSess, RoundOutcome.JAILBREAKER_WIN))
            .thenReturn(Result.success(jailbreakerWinSess))
        // roundRepository.findAllByGameSessionId returns empty list by default; save returns null (unused)
    }

    /** Stubs the full GUARDIAN_WIN + round advance flow. */
    private fun stubGuardianWinFlowRoundAdvance(currentRound: Int = 1) {
        val sess = session(currentRound = currentRound)
        val evaluatingSess = sess.copy(roundStatus = RoundStatus.EVALUATING)
        val guardianWinSess = evaluatingSess.copy(roundStatus = RoundStatus.GUARDIAN_WIN)
        val nextRoundSess = guardianWinSess.copy(currentRound = currentRound + 1, roundStatus = RoundStatus.ACTIVE)
        whenever(sessionRepository.findById(GAME_ID)).thenReturn(sess)
        whenever(sessionStateMachine.beginEvaluation(sess)).thenReturn(Result.success(evaluatingSess))
        whenever(sessionRepository.update(any())).thenAnswer { it.arguments[0] }
        whenever(systemPromptRepository.findLatestByGameSessionId(GAME_ID)).thenReturn(promptVersion())
        whenever(attackVectorRegistry.getVulnerableCodeSample()).thenReturn("vuln code")
        whenever(evaluationService.evaluate(any(), any(), any(), any())).thenReturn(
            Result.success(EvaluationResult(RoundOutcome.GUARDIAN_WIN, "found vuln", EvaluationMethod.PATTERN_MATCH)),
        )
        whenever(sessionStateMachine.recordOutcome(evaluatingSess, RoundOutcome.GUARDIAN_WIN))
            .thenReturn(Result.success(guardianWinSess))
        whenever(sessionStateMachine.advanceToNextRound(guardianWinSess)).thenReturn(Result.success(nextRoundSess))
        // roundRepository.findAllByGameSessionId returns empty list by default; save returns null (unused)
        whenever(systemPromptRepository.save(any())).thenAnswer { it.arguments[0] }
        whenever(attackVectorRegistry.getVector(currentRound + 1)).thenReturn(attackVector(currentRound + 1))
    }

    /** Stubs the full GUARDIAN_WIN + game complete flow (round 4). */
    private fun stubGuardianWinFlowGameComplete() {
        val sess = session(currentRound = 4)
        val evaluatingSess = sess.copy(roundStatus = RoundStatus.EVALUATING)
        val guardianWinSess = evaluatingSess.copy(roundStatus = RoundStatus.GUARDIAN_WIN)
        val completedSess = guardianWinSess.copy(status = GameStatus.COMPLETED)
        whenever(sessionRepository.findById(GAME_ID)).thenReturn(sess)
        whenever(sessionStateMachine.beginEvaluation(sess)).thenReturn(Result.success(evaluatingSess))
        whenever(sessionRepository.update(any())).thenAnswer { it.arguments[0] }
        whenever(systemPromptRepository.findLatestByGameSessionId(GAME_ID)).thenReturn(promptVersion())
        whenever(attackVectorRegistry.getVulnerableCodeSample()).thenReturn("vuln code")
        whenever(evaluationService.evaluate(any(), any(), any(), any())).thenReturn(
            Result.success(EvaluationResult(RoundOutcome.GUARDIAN_WIN, "found vuln", EvaluationMethod.PATTERN_MATCH)),
        )
        whenever(sessionStateMachine.recordOutcome(evaluatingSess, RoundOutcome.GUARDIAN_WIN))
            .thenReturn(Result.success(guardianWinSess))
        whenever(sessionStateMachine.completeGame(guardianWinSess)).thenReturn(Result.success(completedSess))
        whenever(roundRepository.findAllByGameSessionId(GAME_ID)).thenReturn(emptyList())
        whenever(roundRepository.save(any())).thenAnswer { it.arguments[0] }
    }

    companion object {
        private val GAME_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
        private val JAILBREAKER_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000002")
        private val GUARDIAN_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000003")

        private fun session(
            status: GameStatus = GameStatus.IN_PROGRESS,
            roundStatus: RoundStatus = RoundStatus.ACTIVE,
            currentRound: Int = 1,
        ) = GameSession(
            id = GAME_ID,
            status = status,
            roundStatus = roundStatus,
            currentRound = currentRound,
            jailbreakerId = JAILBREAKER_ID,
            guardianId = GUARDIAN_ID,
            version = 0L,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )

        private fun promptVersion(
            content: String = "system prompt",
            versionNumber: Int = 1,
        ) = SystemPromptVersion(
            id = UUID.fromString("00000000-0000-0000-0000-000000000010"),
            gameSessionId = GAME_ID,
            roundNumber = 1,
            versionNumber = versionNumber,
            content = content,
            createdAt = Instant.now(),
        )

        private fun injectionAttempt(
            roundNumber: Int = 1,
            attemptNumber: Int = 1,
        ) = InjectionAttempt(
            id = UUID.randomUUID(),
            gameSessionId = GAME_ID,
            roundNumber = roundNumber,
            attemptNumber = attemptNumber,
            injectionText = "inject",
            llmResponse = "response",
            evaluationMethod = EvaluationMethod.PATTERN_MATCH,
            outcome = RoundOutcome.JAILBREAKER_WIN,
            systemPromptVersionId = UUID.randomUUID(),
            createdAt = Instant.now(),
        )

        private fun attackVector(roundNumber: Int = 1) =
            AttackVector(
                roundNumber = roundNumber,
                name = "Attack $roundNumber",
                description = "Description",
                tier1Hint = "Hint 1",
                tier2HintExample = "Example",
            )
    }
}
