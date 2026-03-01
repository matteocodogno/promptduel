package com.aiscream.promptduel.infrastructure.litellm

import com.aiscream.promptduel.infrastructure.litellm.dto.ChatCompletionRequest
import com.aiscream.promptduel.infrastructure.litellm.dto.ChatCompletionResponse
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange

@HttpExchange(contentType = "application/json")
interface LiteLLMHttpClient {
    @PostExchange("/chat/completions")
    fun complete(
        @RequestBody request: ChatCompletionRequest,
    ): ChatCompletionResponse
}
