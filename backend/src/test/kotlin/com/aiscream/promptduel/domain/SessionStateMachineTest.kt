package com.aiscream.promptduel.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class SessionStateMachineTest {
    private val machine: SessionStateMachine = DefaultSessionStateMachine()

    private fun waitingSession() =
        GameSession(
            id = UUID.randomUUID(),
            status = GameStatus.WAITING_FOR_PLAYERS,
            roundStatus = RoundStatus.ACTIVE,
            currentRound = 1,
            jailbreakerId = UUID.randomUUID(),
            guardianId = null,
            version = 0,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )

    private fun inProgressSession(
        roundStatus: RoundStatus = RoundStatus.ACTIVE,
        currentRound: Int = 1,
    ) = waitingSession().copy(
        status = GameStatus.IN_PROGRESS,
        guardianId = UUID.randomUUID(),
        roundStatus = roundStatus,
        currentRound = currentRound,
    )

    // ── startGame ─────────────────────────────────────────────────────────────

    @Test
    fun `startGame transitions WAITING_FOR_PLAYERS to IN_PROGRESS`() {
        val result = machine.startGame(waitingSession())
        assertTrue(result.isSuccess)
        assertEquals(GameStatus.IN_PROGRESS, result.getOrThrow().status)
    }

    @Test
    fun `startGame sets round to 1 and roundStatus to ACTIVE`() {
        val session = machine.startGame(waitingSession()).getOrThrow()
        assertEquals(1, session.currentRound)
        assertEquals(RoundStatus.ACTIVE, session.roundStatus)
    }

    @Test
    fun `startGame fails when session is already IN_PROGRESS`() {
        val result = machine.startGame(inProgressSession())
        assertTrue(result.isFailure)
        assertInstanceOf(InvalidTransitionException::class.java, result.exceptionOrNull())
    }

    @Test
    fun `startGame fails when session is COMPLETED`() {
        val completed = inProgressSession().copy(status = GameStatus.COMPLETED)
        val result = machine.startGame(completed)
        assertTrue(result.isFailure)
        assertInstanceOf(InvalidTransitionException::class.java, result.exceptionOrNull())
    }

    // ── beginEvaluation ───────────────────────────────────────────────────────

    @Test
    fun `beginEvaluation transitions ACTIVE to EVALUATING`() {
        val result = machine.beginEvaluation(inProgressSession(RoundStatus.ACTIVE))
        assertTrue(result.isSuccess)
        assertEquals(RoundStatus.EVALUATING, result.getOrThrow().roundStatus)
    }

    @Test
    fun `beginEvaluation fails when roundStatus is EVALUATING`() {
        val result = machine.beginEvaluation(inProgressSession(RoundStatus.EVALUATING))
        assertTrue(result.isFailure)
        assertInstanceOf(InvalidTransitionException::class.java, result.exceptionOrNull())
    }

    @Test
    fun `beginEvaluation fails when session is not IN_PROGRESS`() {
        val result = machine.beginEvaluation(waitingSession())
        assertTrue(result.isFailure)
        assertInstanceOf(InvalidTransitionException::class.java, result.exceptionOrNull())
    }

    // ── recordOutcome — JAILBREAKER_WIN ───────────────────────────────────────

    @Test
    fun `recordOutcome JAILBREAKER_WIN sets roundStatus to JAILBREAKER_WIN`() {
        val session = inProgressSession(RoundStatus.EVALUATING)
        val result = machine.recordOutcome(session, RoundOutcome.JAILBREAKER_WIN)
        assertTrue(result.isSuccess)
        assertEquals(RoundStatus.JAILBREAKER_WIN, result.getOrThrow().roundStatus)
    }

    @Test
    fun `recordOutcome JAILBREAKER_WIN does not change currentRound`() {
        val session = inProgressSession(RoundStatus.EVALUATING, currentRound = 2)
        val result = machine.recordOutcome(session, RoundOutcome.JAILBREAKER_WIN)
        assertEquals(2, result.getOrThrow().currentRound)
    }

    // ── recordOutcome — GUARDIAN_WIN ──────────────────────────────────────────

    @Test
    fun `recordOutcome GUARDIAN_WIN sets roundStatus to GUARDIAN_WIN`() {
        val session = inProgressSession(RoundStatus.EVALUATING)
        val result = machine.recordOutcome(session, RoundOutcome.GUARDIAN_WIN)
        assertTrue(result.isSuccess)
        assertEquals(RoundStatus.GUARDIAN_WIN, result.getOrThrow().roundStatus)
    }

    @Test
    fun `recordOutcome fails when roundStatus is not EVALUATING`() {
        val result = machine.recordOutcome(inProgressSession(RoundStatus.ACTIVE), RoundOutcome.GUARDIAN_WIN)
        assertTrue(result.isFailure)
        assertInstanceOf(InvalidTransitionException::class.java, result.exceptionOrNull())
    }

    @Test
    fun `recordOutcome fails when session is not IN_PROGRESS`() {
        val waiting = waitingSession().copy(roundStatus = RoundStatus.EVALUATING)
        val result = machine.recordOutcome(waiting, RoundOutcome.GUARDIAN_WIN)
        assertTrue(result.isFailure)
        assertInstanceOf(InvalidTransitionException::class.java, result.exceptionOrNull())
    }

    // ── startNextAttempt ──────────────────────────────────────────────────────

    @Test
    fun `startNextAttempt transitions JAILBREAKER_WIN to ACTIVE`() {
        val session = inProgressSession(RoundStatus.JAILBREAKER_WIN)
        val result = machine.startNextAttempt(session)
        assertTrue(result.isSuccess)
        assertEquals(RoundStatus.ACTIVE, result.getOrThrow().roundStatus)
    }

    @Test
    fun `startNextAttempt keeps same round number`() {
        val session = inProgressSession(RoundStatus.JAILBREAKER_WIN, currentRound = 3)
        val result = machine.startNextAttempt(session)
        assertEquals(3, result.getOrThrow().currentRound)
    }

    @Test
    fun `startNextAttempt fails when roundStatus is not JAILBREAKER_WIN`() {
        val result = machine.startNextAttempt(inProgressSession(RoundStatus.ACTIVE))
        assertTrue(result.isFailure)
        assertInstanceOf(InvalidTransitionException::class.java, result.exceptionOrNull())
    }

    // ── advanceToNextRound ────────────────────────────────────────────────────

    @Test
    fun `advanceToNextRound increments currentRound`() {
        val session = inProgressSession(RoundStatus.GUARDIAN_WIN, currentRound = 1)
        val result = machine.advanceToNextRound(session)
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow().currentRound)
    }

    @Test
    fun `advanceToNextRound resets roundStatus to ACTIVE`() {
        val session = inProgressSession(RoundStatus.GUARDIAN_WIN, currentRound = 2)
        val result = machine.advanceToNextRound(session)
        assertEquals(RoundStatus.ACTIVE, result.getOrThrow().roundStatus)
    }

    @Test
    fun `advanceToNextRound fails when roundStatus is not GUARDIAN_WIN`() {
        val result = machine.advanceToNextRound(inProgressSession(RoundStatus.ACTIVE))
        assertTrue(result.isFailure)
        assertInstanceOf(InvalidTransitionException::class.java, result.exceptionOrNull())
    }

    @Test
    fun `advanceToNextRound fails when already on round 4`() {
        val session = inProgressSession(RoundStatus.GUARDIAN_WIN, currentRound = 4)
        val result = machine.advanceToNextRound(session)
        assertTrue(result.isFailure)
        assertInstanceOf(InvalidTransitionException::class.java, result.exceptionOrNull())
    }

    // ── completeGame ──────────────────────────────────────────────────────────

    @Test
    fun `completeGame transitions IN_PROGRESS to COMPLETED`() {
        val session = inProgressSession(RoundStatus.GUARDIAN_WIN, currentRound = 4)
        val result = machine.completeGame(session)
        assertTrue(result.isSuccess)
        assertEquals(GameStatus.COMPLETED, result.getOrThrow().status)
    }

    @Test
    fun `completeGame fails when not on round 4`() {
        val session = inProgressSession(RoundStatus.GUARDIAN_WIN, currentRound = 3)
        val result = machine.completeGame(session)
        assertTrue(result.isFailure)
        assertInstanceOf(InvalidTransitionException::class.java, result.exceptionOrNull())
    }

    @Test
    fun `completeGame fails when roundStatus is not GUARDIAN_WIN`() {
        val session = inProgressSession(RoundStatus.ACTIVE, currentRound = 4)
        val result = machine.completeGame(session)
        assertTrue(result.isFailure)
        assertInstanceOf(InvalidTransitionException::class.java, result.exceptionOrNull())
    }

    @Test
    fun `completeGame fails when session is already COMPLETED`() {
        val session =
            inProgressSession(RoundStatus.GUARDIAN_WIN, currentRound = 4)
                .copy(status = GameStatus.COMPLETED)
        val result = machine.completeGame(session)
        assertTrue(result.isFailure)
        assertInstanceOf(InvalidTransitionException::class.java, result.exceptionOrNull())
    }
}
