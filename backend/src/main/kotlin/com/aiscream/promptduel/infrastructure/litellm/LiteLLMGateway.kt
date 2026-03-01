package com.aiscream.promptduel.infrastructure.litellm

import com.aiscream.promptduel.infrastructure.config.LiteLLMProperties
import com.aiscream.promptduel.infrastructure.litellm.dto.ChatCompletionMessage
import com.aiscream.promptduel.infrastructure.litellm.dto.ChatCompletionRequest
import org.springframework.stereotype.Component
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClientResponseException

@Component
class LiteLLMGateway(
    private val httpClient: LiteLLMHttpClient,
    private val props: LiteLLMProperties,
) : LlmGateway {
    override fun complete(
        systemPrompt: String,
        userMessage: String,
    ): Result<LlmResponse> {
        val request =
            ChatCompletionRequest(
                model = props.model,
                messages =
                    listOf(
                        ChatCompletionMessage(role = "system", content = systemPrompt),
                        ChatCompletionMessage(role = "user", content = userMessage),
                    ),
            )

        return try {
            val response = httpClient.complete(request)
            val content =
                response.choices
                    .firstOrNull()
                    ?.message
                    ?.content
            if (content.isNullOrBlank()) {
                Result.failure(LlmError.ParseError("LLM returned empty or missing content"))
            } else {
                Result.success(
                    LlmResponse(
                        content = content,
                        model = response.model,
                        tokensUsed = response.usage.totalTokens,
                    ),
                )
            }
        } catch (e: RestClientResponseException) {
            Result.failure(
                LlmError.HttpError(
                    statusCode = e.statusCode.value(),
                    body = e.responseBodyAsString,
                ),
            )
        } catch (e: ResourceAccessException) {
            Result.failure(LlmError.Timeout(durationMs = props.timeoutMs))
        }
    }
}
