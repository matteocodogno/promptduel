package com.aiscream.promptduel

import com.aiscream.promptduel.infrastructure.config.EvaluationProperties
import com.aiscream.promptduel.infrastructure.config.LiteLLMProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(LiteLLMProperties::class, EvaluationProperties::class)
class PromptDuelApplication

fun main(args: Array<String>) {
    runApplication<PromptDuelApplication>(*args)
}
