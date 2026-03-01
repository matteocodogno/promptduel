package com.aiscream.promptduel.domain

import java.time.Instant
import java.util.UUID

data class InjectionAttempt(
    val id: UUID,
    val gameSessionId: UUID,
    val roundNumber: Int,
    val attemptNumber: Int,
    val injectionText: String,
    val llmResponse: String,
    val evaluationMethod: EvaluationMethod,
    val outcome: RoundOutcome,
    val systemPromptVersionId: UUID,
    val createdAt: Instant,
)
