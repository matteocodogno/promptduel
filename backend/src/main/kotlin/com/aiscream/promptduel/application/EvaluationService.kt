package com.aiscream.promptduel.application

import com.aiscream.promptduel.domain.EvaluationMethod
import com.aiscream.promptduel.domain.RoundOutcome

data class EvaluationResult(
    val outcome: RoundOutcome,
    val llmResponse: String,
    val evaluationMethod: EvaluationMethod,
)

interface EvaluationService {
    fun evaluate(
        systemPrompt: String,
        vulnerableCode: String,
        injectionText: String,
        attackVectorId: Int,
    ): Result<EvaluationResult>
}
