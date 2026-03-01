package com.aiscream.promptduel.infrastructure.litellm.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatCompletionMessage>,
)

data class ChatCompletionMessage(
    val role: String,
    val content: String,
)

data class ChatCompletionResponse(
    val id: String = "",
    val model: String,
    val choices: List<ChatCompletionChoice>,
    val usage: ChatCompletionUsage = ChatCompletionUsage(),
)

data class ChatCompletionChoice(
    val index: Int = 0,
    val message: ChatCompletionMessage,
    @JsonProperty("finish_reason")
    val finishReason: String = "",
)

data class ChatCompletionUsage(
    @JsonProperty("prompt_tokens")
    val promptTokens: Int = 0,
    @JsonProperty("completion_tokens")
    val completionTokens: Int = 0,
    @JsonProperty("total_tokens")
    val totalTokens: Int = 0,
)
