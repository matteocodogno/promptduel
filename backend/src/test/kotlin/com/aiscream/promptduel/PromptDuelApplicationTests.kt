package com.aiscream.promptduel

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * Context smoke test.
 *
 * Verifies that the Spring application context loads successfully with:
 *   - all declared beans wired correctly
 *   - application.yml parsed and all required properties bound
 *   - datasource connected to a real PostgreSQL 16 instance (via TestContainers)
 *
 * Flyway and JPA schema validation are disabled via application-test.yml
 * because migration scripts are added in TASK-3.
 */
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@Testcontainers
@ActiveProfiles("test")
class PromptDuelApplicationTests {
    companion object {
        @Container
        @JvmStatic
        val postgres: PostgreSQLContainer<*> =
            PostgreSQLContainer("postgres:16")
                .withDatabaseName("promptduel_test")
                .withUsername("test")
                .withPassword("test")

        @DynamicPropertySource
        @JvmStatic
        fun overrideDataSourceProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
        }
    }

    @Test
    fun contextLoads() {
        // If the application context fails to start, Spring throws before
        // reaching this line and the test fails with a descriptive cause.
    }
}
