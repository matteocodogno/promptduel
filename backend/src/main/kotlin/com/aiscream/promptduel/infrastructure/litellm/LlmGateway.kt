package com.aiscream.promptduel.infrastructure.litellm

data class LlmResponse(
    val content: String,
    val model: String,
    val tokensUsed: Int,
)

sealed class LlmError : Throwable() {
    data class HttpError(
        val statusCode: Int,
        val body: String,
    ) : LlmError()

    data class Timeout(
        val durationMs: Long,
    ) : LlmError()

    data class ParseError(
        override val message: String,
    ) : LlmError()
}

interface LlmGateway {
    fun complete(
        systemPrompt: String,
        userMessage: String,
    ): Result<LlmResponse>
}
