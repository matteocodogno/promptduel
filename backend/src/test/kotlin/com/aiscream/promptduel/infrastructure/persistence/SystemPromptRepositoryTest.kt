package com.aiscream.promptduel.infrastructure.persistence

import com.aiscream.promptduel.domain.GameSession
import com.aiscream.promptduel.domain.GameStatus
import com.aiscream.promptduel.domain.RoundStatus
import com.aiscream.promptduel.domain.SystemPromptVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
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
import java.time.Instant
import java.util.UUID

@Testcontainers
@SpringBootTest(webEnvironment = NONE)
@ActiveProfiles("test")
class SystemPromptRepositoryTest {
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
    lateinit var sessionRepository: SessionRepository

    @Autowired
    lateinit var systemPromptRepository: SystemPromptRepository

    private fun persistSession(id: UUID = UUID.randomUUID()): UUID {
        sessionRepository.save(
            GameSession(
                id = id,
                status = GameStatus.WAITING_FOR_PLAYERS,
                roundStatus = RoundStatus.ACTIVE,
                currentRound = 1,
                jailbreakerId = null,
                guardianId = null,
                version = 0L,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            ),
        )
        return id
    }

    private fun aPromptVersion(
        gameSessionId: UUID,
        roundNumber: Int = 1,
        versionNumber: Int = 1,
        content: String = "You are a code review assistant.",
    ) = SystemPromptVersion(
        id = UUID.randomUUID(),
        gameSessionId = gameSessionId,
        roundNumber = roundNumber,
        versionNumber = versionNumber,
        content = content,
        createdAt = Instant.now(),
    )

    @Test
    fun `save persists a prompt version and findLatest returns it`() {
        val gameId = persistSession()
        val version = aPromptVersion(gameSessionId = gameId)

        systemPromptRepository.save(version)

        val found = systemPromptRepository.findLatestByGameSessionId(gameId)
        assertNotNull(found)
        assertEquals(version.id, found!!.id)
        assertEquals(version.content, found.content)
    }

    @Test
    fun `findLatest returns null when no prompts exist for session`() {
        val gameId = persistSession()
        val result = systemPromptRepository.findLatestByGameSessionId(gameId)
        assertNull(result)
    }

    @Test
    fun `findLatest returns the highest versionNumber for the session`() {
        val gameId = persistSession()
        systemPromptRepository.save(aPromptVersion(gameSessionId = gameId, versionNumber = 1, content = "v1"))
        systemPromptRepository.save(aPromptVersion(gameSessionId = gameId, versionNumber = 2, content = "v2"))
        systemPromptRepository.save(aPromptVersion(gameSessionId = gameId, versionNumber = 3, content = "v3"))

        val latest = systemPromptRepository.findLatestByGameSessionId(gameId)!!
        assertEquals(3, latest.versionNumber)
        assertEquals("v3", latest.content)
    }

    @Test
    fun `findAllByGameSessionId returns all versions in order`() {
        val gameId = persistSession()
        systemPromptRepository.save(aPromptVersion(gameSessionId = gameId, versionNumber = 1))
        systemPromptRepository.save(aPromptVersion(gameSessionId = gameId, versionNumber = 2))

        val all = systemPromptRepository.findAllByGameSessionId(gameId)
        assertEquals(2, all.size)
        assertEquals(1, all[0].versionNumber)
        assertEquals(2, all[1].versionNumber)
    }

    @Test
    fun `findAllByGameSessionId returns empty list for unknown session`() {
        val all = systemPromptRepository.findAllByGameSessionId(UUID.randomUUID())
        assertEquals(emptyList<SystemPromptVersion>(), all)
    }

    @Test
    fun `SystemPromptRepository does not expose delete operations`() {
        val methods = SystemPromptRepository::class.java.methods.map { it.name }
        assert("delete" !in methods) { "delete must not be exposed on SystemPromptRepository" }
        assert("deleteById" !in methods) { "deleteById must not be exposed on SystemPromptRepository" }
        assert("deleteAll" !in methods) { "deleteAll must not be exposed on SystemPromptRepository" }
    }
}
