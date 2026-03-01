package com.aiscream.promptduel

import com.aiscream.promptduel.infrastructure.config.LiquibaseProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(LiquibaseProperties::class)
class PromptDuelApplication

fun main(args: Array<String>) {
    runApplication<PromptDuelApplication>(*args)
}
