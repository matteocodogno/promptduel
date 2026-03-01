package com.aiscream.promptduel.infrastructure.litellm

import com.aiscream.promptduel.infrastructure.config.LiteLLMProperties
import com.aiscream.promptduel.infrastructure.config.LiteLLMClientConfig
import com.aiscream.promptduel.infrastructure.litellm.dto.ChatCompletionRequest
import com.aiscream.promptduel.infrastructure.litellm.dto.ChatCompletionMessage
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

@WireMockTest
class LiteLLMClientIntegrationTest {

    @Test
    fun `sends POST to chat completions with correct body and Authorization header`(
        wm: WireMockRuntimeInfo,
    ) {
        wm.wireMock.register(
            post(urlEqualTo("/chat/completions"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                            {
                              "id": "chatcmpl-123",
                              "model": "local-smart",
                              "choices": [
                                {
                                  "index": 0,
                                  "message": { "role": "assistant", "content": "SQL injection found." },
                                  "finish_reason": "stop"
                                }
                              ],
                              "usage": { "prompt_tokens": 10, "completion_tokens": 5, "total_tokens": 15 }
                            }
                            """.trimIndent()
                        )
                )
        )

        val props = LiteLLMProperties(
            host = "localhost",
            port = wm.httpPort,
            model = "local-smart",
            timeoutMs = 5000,
            apiKey = "test-key",
        )
        val client = LiteLLMClientConfig().liteLLMHttpClient(props)

        val request = ChatCompletionRequest(
            model = "local-smart",
            messages = listOf(
                ChatCompletionMessage(role = "system", content = "You are a code reviewer."),
                ChatCompletionMessage(role = "user", content = "Review this: SELECT * FROM users"),
            ),
        )

        val response = client.complete(request)

        assertNotNull(response)
        assertEquals("local-smart", response.model)
        assertEquals("SQL injection found.", response.choices.first().message.content)

        wm.wireMock.verifyThat(
            postRequestedFor(urlEqualTo("/chat/completions"))
                .withHeader("Authorization", equalTo("Bearer test-key"))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(
                    equalToJson(
                        """
                        {
                          "model": "local-smart",
                          "messages": [
                            { "role": "system", "content": "You are a code reviewer." },
                            { "role": "user", "content": "Review this: SELECT * FROM users" }
                          ]
                        }
                        """.trimIndent(),
                        true, false
                    )
                )
        )
    }

    @Test
    fun `properties bind model default to local-smart`() {
        val props = LiteLLMProperties()
        assertEquals("local-smart", props.model)
        assertEquals("localhost", props.host)
        assertEquals(4000, props.port)
        assertEquals(30_000L, props.timeoutMs)
    }

    @Test
    fun `properties accept overridden values`() {
        val props = LiteLLMProperties(
            host = "llm-host",
            port = 9000,
            model = "custom-model",
            timeoutMs = 60_000L,
            apiKey = "secret",
        )
        assertEquals("llm-host", props.host)
        assertEquals(9000, props.port)
        assertEquals("custom-model", props.model)
        assertEquals(60_000L, props.timeoutMs)
        assertEquals("secret", props.apiKey)
    }
}
