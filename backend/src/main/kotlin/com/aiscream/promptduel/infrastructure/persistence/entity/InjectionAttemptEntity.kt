package com.aiscream.promptduel.infrastructure.persistence.entity

import com.aiscream.promptduel.domain.EvaluationMethod
import com.aiscream.promptduel.domain.InjectionAttempt
import com.aiscream.promptduel.domain.RoundOutcome
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "injection_attempts")
class InjectionAttemptEntity(
    @Id
    val id: UUID,
    @Column(name = "game_session_id", nullable = false)
    val gameSessionId: UUID,
    @Column(name = "round_number", nullable = false)
    val roundNumber: Int,
    @Column(name = "attempt_number", nullable = false)
    val attemptNumber: Int,
    @Column(name = "injection_text", nullable = false, columnDefinition = "TEXT")
    val injectionText: String,
    @Column(name = "llm_response", nullable = false, columnDefinition = "TEXT")
    val llmResponse: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "evaluation_method", nullable = false, length = 20)
    val evaluationMethod: EvaluationMethod,
    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 20)
    val outcome: RoundOutcome,
    @Column(name = "system_prompt_version_id", nullable = false)
    val systemPromptVersionId: UUID,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,
) {
    fun toDomain(): InjectionAttempt =
        InjectionAttempt(
            id = id,
            gameSessionId = gameSessionId,
            roundNumber = roundNumber,
            attemptNumber = attemptNumber,
            injectionText = injectionText,
            llmResponse = llmResponse,
            evaluationMethod = evaluationMethod,
            outcome = outcome,
            systemPromptVersionId = systemPromptVersionId,
            createdAt = createdAt,
        )

    companion object {
        fun fromDomain(attempt: InjectionAttempt): InjectionAttemptEntity =
            InjectionAttemptEntity(
                id = attempt.id,
                gameSessionId = attempt.gameSessionId,
                roundNumber = attempt.roundNumber,
                attemptNumber = attempt.attemptNumber,
                injectionText = attempt.injectionText,
                llmResponse = attempt.llmResponse,
                evaluationMethod = attempt.evaluationMethod,
                outcome = attempt.outcome,
                systemPromptVersionId = attempt.systemPromptVersionId,
                createdAt = attempt.createdAt,
            )
    }
}
