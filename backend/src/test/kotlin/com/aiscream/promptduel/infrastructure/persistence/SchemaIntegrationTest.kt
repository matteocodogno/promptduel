package com.aiscream.promptduel.infrastructure.persistence

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@SpringBootTest(webEnvironment = NONE)
@ActiveProfiles("test")
class SchemaIntegrationTest {
    companion object {
        @Container
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine")

        @JvmStatic
        @DynamicPropertySource
        fun datasourceProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
        }
    }

    @Autowired
    lateinit var jdbc: JdbcTemplate

    // ── Table existence ──────────────────────────────────────────────────────

    @Test
    fun `game_sessions table exists with all required columns`() {
        val columns = fetchColumns("game_sessions")
        assertTrue(columns.contains("id"), "missing: id")
        assertTrue(columns.contains("status"), "missing: status")
        assertTrue(columns.contains("current_round"), "missing: current_round")
        assertTrue(columns.contains("jailbreaker_id"), "missing: jailbreaker_id")
        assertTrue(columns.contains("guardian_id"), "missing: guardian_id")
        assertTrue(columns.contains("created_at"), "missing: created_at")
        assertTrue(columns.contains("updated_at"), "missing: updated_at")
        assertTrue(columns.contains("version"), "missing: version (optimistic lock)")
    }

    @Test
    fun `system_prompt_versions table exists with all required columns`() {
        val columns = fetchColumns("system_prompt_versions")
        assertTrue(columns.contains("id"), "missing: id")
        assertTrue(columns.contains("game_session_id"), "missing: game_session_id")
        assertTrue(columns.contains("round_number"), "missing: round_number")
        assertTrue(columns.contains("version_number"), "missing: version_number")
        assertTrue(columns.contains("content"), "missing: content")
        assertTrue(columns.contains("created_at"), "missing: created_at")
    }

    @Test
    fun `injection_attempts table exists with all required columns`() {
        val columns = fetchColumns("injection_attempts")
        assertTrue(columns.contains("id"), "missing: id")
        assertTrue(columns.contains("game_session_id"), "missing: game_session_id")
        assertTrue(columns.contains("round_number"), "missing: round_number")
        assertTrue(columns.contains("attempt_number"), "missing: attempt_number")
        assertTrue(columns.contains("injection_text"), "missing: injection_text")
        assertTrue(columns.contains("llm_response"), "missing: llm_response")
        assertTrue(columns.contains("evaluation_method"), "missing: evaluation_method")
        assertTrue(columns.contains("outcome"), "missing: outcome")
        assertTrue(columns.contains("system_prompt_version_id"), "missing: system_prompt_version_id")
        assertTrue(columns.contains("created_at"), "missing: created_at")
    }

    // ── Unique constraints ───────────────────────────────────────────────────

    @Test
    fun `system_prompt_versions has unique constraint on (game_session_id, round_number, version_number)`() {
        val count =
            jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM information_schema.table_constraints tc
                JOIN information_schema.constraint_column_usage cu
                  ON tc.constraint_name = cu.constraint_name
                 AND tc.table_name = cu.table_name
                WHERE tc.constraint_type = 'UNIQUE'
                  AND tc.table_name = 'system_prompt_versions'
                  AND cu.column_name IN ('game_session_id', 'round_number', 'version_number')
                GROUP BY tc.constraint_name
                HAVING COUNT(cu.column_name) = 3
                """.trimIndent(),
                Int::class.java,
            ) ?: 0
        assertTrue(count > 0, "Missing UNIQUE(game_session_id, round_number, version_number) on system_prompt_versions")
    }

    @Test
    fun `injection_attempts has unique constraint on (game_session_id, round_number, attempt_number)`() {
        val count =
            jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM information_schema.table_constraints tc
                JOIN information_schema.constraint_column_usage cu
                  ON tc.constraint_name = cu.constraint_name
                 AND tc.table_name = cu.table_name
                WHERE tc.constraint_type = 'UNIQUE'
                  AND tc.table_name = 'injection_attempts'
                  AND cu.column_name IN ('game_session_id', 'round_number', 'attempt_number')
                GROUP BY tc.constraint_name
                HAVING COUNT(cu.column_name) = 3
                """.trimIndent(),
                Int::class.java,
            ) ?: 0
        assertTrue(count > 0, "Missing UNIQUE(game_session_id, round_number, attempt_number) on injection_attempts")
    }

    // ── Foreign keys ─────────────────────────────────────────────────────────

    @Test
    fun `system_prompt_versions has FK to game_sessions`() {
        assertTrue(foreignKeyExists("system_prompt_versions", "game_sessions"))
    }

    @Test
    fun `injection_attempts has FK to game_sessions`() {
        assertTrue(foreignKeyExists("injection_attempts", "game_sessions"))
    }

    @Test
    fun `injection_attempts has FK to system_prompt_versions`() {
        assertTrue(foreignKeyExists("injection_attempts", "system_prompt_versions"))
    }

    // ── Indices ──────────────────────────────────────────────────────────────

    @Test
    fun `idx_injection_attempts_session index exists`() {
        assertTrue(indexExists("idx_injection_attempts_session"), "Missing index: idx_injection_attempts_session")
    }

    @Test
    fun `idx_system_prompt_session index exists`() {
        assertTrue(indexExists("idx_system_prompt_session"), "Missing index: idx_system_prompt_session")
    }

    // ── Default values ───────────────────────────────────────────────────────

    @Test
    fun `game_sessions current_round defaults to 1`() {
        val default =
            jdbc.queryForObject(
                "SELECT column_default FROM information_schema.columns WHERE table_name = 'game_sessions' AND column_name = 'current_round'",
                String::class.java,
            )
        assertEquals("1", default, "current_round should default to 1")
    }

    @Test
    fun `game_sessions version defaults to 0`() {
        val default =
            jdbc.queryForObject(
                "SELECT column_default FROM information_schema.columns WHERE table_name = 'game_sessions' AND column_name = 'version'",
                String::class.java,
            )
        assertEquals("0", default, "version should default to 0")
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun fetchColumns(table: String): Set<String> =
        jdbc
            .queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_name = ?",
                String::class.java,
                table,
            ).toSet()

    private fun foreignKeyExists(
        fromTable: String,
        toTable: String,
    ): Boolean =
        (
            jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM information_schema.referential_constraints rc
                JOIN information_schema.table_constraints tc
                  ON rc.constraint_name = tc.constraint_name
                JOIN information_schema.table_constraints tc2
                  ON rc.unique_constraint_name = tc2.constraint_name
                WHERE tc.table_name = ? AND tc2.table_name = ?
                """.trimIndent(),
                Int::class.java,
                fromTable,
                toTable,
            ) ?: 0
        ) > 0

    private fun indexExists(indexName: String): Boolean =
        (
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes WHERE indexname = ?",
                Int::class.java,
                indexName,
            ) ?: 0
        ) > 0
}
