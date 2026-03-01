package com.aiscream.promptduel

import com.aiscream.promptduel.infrastructure.config.LiquibaseProperties
import com.aiscream.promptduel.infrastructure.config.LiteLLMProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(LiquibaseProperties::class, LiteLLMProperties::class)
class PromptDuelApplication

fun main(args: Array<String>) {
    runApplication<PromptDuelApplication>(*args)
}
