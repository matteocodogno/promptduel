package com.aiscream.promptduel.infrastructure.litellm

import com.aiscream.promptduel.infrastructure.config.LiteLLMProperties
import com.aiscream.promptduel.infrastructure.litellm.dto.ChatCompletionChoice
import com.aiscream.promptduel.infrastructure.litellm.dto.ChatCompletionMessage
import com.aiscream.promptduel.infrastructure.litellm.dto.ChatCompletionResponse
import com.aiscream.promptduel.infrastructure.litellm.dto.ChatCompletionUsage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClientResponseException
import java.net.SocketTimeoutException

@ExtendWith(MockitoExtension::class)
class LlmGatewayTest {
    @Mock
    private lateinit var httpClient: LiteLLMHttpClient

    private val props = LiteLLMProperties(model = "test-model", timeoutMs = 5_000L)

    private lateinit var gateway: LlmGateway

    @BeforeEach
    fun setUp() {
        gateway = LiteLLMGateway(httpClient, props)
    }

    // ── happy path ────────────────────────────────────────────────────────────

    @Test
    fun `complete returns LlmResponse with content model and token count on success`() {
        whenever(httpClient.complete(any())).thenReturn(successResponse("The code has a SQL injection vulnerability."))

        val result = gateway.complete("You are a reviewer.", "Review this code.")

        assertTrue(result.isSuccess)
        with(result.getOrThrow()) {
            assertEquals("The code has a SQL injection vulnerability.", content)
            assertEquals("test-model", model)
            assertEquals(15, tokensUsed)
        }
    }

    @Test
    fun `complete sends system and user messages using the configured model`() {
        whenever(httpClient.complete(any())).thenReturn(successResponse("ok"))

        gateway.complete("sys prompt", "user msg")

        verify(httpClient).complete(
            org.mockito.kotlin.argThat { req ->
                req.model == "test-model" &&
                    req.messages.any { it.role == "system" && it.content == "sys prompt" } &&
                    req.messages.any { it.role == "user" && it.content == "user msg" }
            },
        )
    }

    // ── error mapping ─────────────────────────────────────────────────────────

    @Test
    fun `complete maps 4xx response to HttpError`() {
        whenever(httpClient.complete(any())).thenThrow(
            RestClientResponseException(
                "Bad Request",
                400,
                "Bad Request",
                null,
                "bad input".toByteArray(),
                Charsets.UTF_8,
            ),
        )

        val result = gateway.complete("sys", "user")

        assertTrue(result.isFailure)
        val error = assertInstanceOf(LlmError.HttpError::class.java, result.exceptionOrNull())
        assertEquals(400, error.statusCode)
    }

    @Test
    fun `complete maps 5xx response to HttpError with status code and body`() {
        whenever(httpClient.complete(any())).thenThrow(
            RestClientResponseException(
                "Internal Server Error",
                500,
                "Internal Server Error",
                null,
                "server error".toByteArray(),
                Charsets.UTF_8,
            ),
        )

        val result = gateway.complete("sys", "user")

        assertTrue(result.isFailure)
        val error = assertInstanceOf(LlmError.HttpError::class.java, result.exceptionOrNull())
        assertEquals(500, error.statusCode)
        assertEquals("server error", error.body)
    }

    @Test
    fun `complete maps ResourceAccessException to Timeout with configured duration`() {
        whenever(httpClient.complete(any())).thenThrow(
            ResourceAccessException("Read timed out", SocketTimeoutException("timeout")),
        )

        val result = gateway.complete("sys", "user")

        assertTrue(result.isFailure)
        val error = assertInstanceOf(LlmError.Timeout::class.java, result.exceptionOrNull())
        assertEquals(5_000L, error.durationMs)
    }

    @Test
    fun `complete maps empty choices list to ParseError`() {
        whenever(httpClient.complete(any())).thenReturn(
            ChatCompletionResponse(model = "test-model", choices = emptyList(), usage = ChatCompletionUsage()),
        )

        val result = gateway.complete("sys", "user")

        assertTrue(result.isFailure)
        assertInstanceOf(LlmError.ParseError::class.java, result.exceptionOrNull())
    }

    @Test
    fun `complete maps blank content in response to ParseError`() {
        whenever(httpClient.complete(any())).thenReturn(
            ChatCompletionResponse(
                model = "test-model",
                choices =
                    listOf(
                        ChatCompletionChoice(message = ChatCompletionMessage(role = "assistant", content = "   ")),
                    ),
                usage = ChatCompletionUsage(),
            ),
        )

        val result = gateway.complete("sys", "user")

        assertTrue(result.isFailure)
        assertInstanceOf(LlmError.ParseError::class.java, result.exceptionOrNull())
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun successResponse(content: String) =
        ChatCompletionResponse(
            model = "test-model",
            choices =
                listOf(
                    ChatCompletionChoice(
                        message = ChatCompletionMessage(role = "assistant", content = content),
                        finishReason = "stop",
                    ),
                ),
            usage = ChatCompletionUsage(promptTokens = 10, completionTokens = 5, totalTokens = 15),
        )
}
