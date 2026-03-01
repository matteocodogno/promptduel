package com.aiscream.promptduel.infrastructure.litellm

import com.aiscream.promptduel.infrastructure.config.LiteLLMProperties
import com.aiscream.promptduel.infrastructure.litellm.dto.ChatCompletionMessage
import com.aiscream.promptduel.infrastructure.litellm.dto.ChatCompletionRequest
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@SpringBootTest(webEnvironment = NONE)
@ActiveProfiles("test")
class LiteLLMClientIntegrationTest {

    @Autowired
    lateinit var liteLLMHttpClient: LiteLLMHttpClient

    companion object {
        @Container
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine")

        // Started at class-load time so the port is available during @DynamicPropertySource
        private val wireMock = WireMockServer(wireMockConfig().dynamicPort()).also { it.start() }

        @AfterAll
        @JvmStatic
        fun stopWireMock() {
            wireMock.stop()
        }

        @JvmStatic
        @DynamicPropertySource
        fun configure(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("app.litellm.host") { "localhost" }
            registry.add("app.litellm.port") { wireMock.port() }
            registry.add("app.litellm.api-key") { "test-key" }
        }
    }

    @BeforeEach
    fun resetWireMock() {
        wireMock.resetAll()
    }

    @Test
    fun `sends POST to chat completions with correct body and Authorization header`() {
        wireMock.stubFor(
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

        val request = ChatCompletionRequest(
            model = "local-smart",
            messages = listOf(
                ChatCompletionMessage(role = "system", content = "You are a code reviewer."),
                ChatCompletionMessage(role = "user", content = "Review this: SELECT * FROM users"),
            ),
        )

        val response = liteLLMHttpClient.complete(request)

        assertNotNull(response)
        assertEquals("local-smart", response.model)
        assertEquals("SQL injection found.", response.choices.first().message.content)

        wireMock.verify(
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
