package com.aiscream.promptduel.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.DefaultValue

@ConfigurationProperties(prefix = "app.litellm")
data class LiteLLMProperties(
    @DefaultValue("localhost")
    val host: String = "localhost",
    @DefaultValue("4000")
    val port: Int = 4000,
    @DefaultValue("local-smart")
    val model: String = "local-smart",
    @DefaultValue("30000")
    val timeoutMs: Long = 30_000L,
    @DefaultValue("")
    val apiKey: String = "",
)
