package com.aiscream.promptduel.infrastructure.persistence

import com.aiscream.promptduel.domain.GameSession
import com.aiscream.promptduel.domain.GameStatus
import com.aiscream.promptduel.domain.RoundStatus
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
class GameSessionRepositoryTest {
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

    private fun aGameSession(
        id: UUID = UUID.randomUUID(),
        status: GameStatus = GameStatus.WAITING_FOR_PLAYERS,
        roundStatus: RoundStatus = RoundStatus.ACTIVE,
        currentRound: Int = 1,
        jailbreakerId: UUID? = null,
        guardianId: UUID? = null,
    ) = GameSession(
        id = id,
        status = status,
        roundStatus = roundStatus,
        currentRound = currentRound,
        jailbreakerId = jailbreakerId,
        guardianId = guardianId,
        version = 0L,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )

    @Test
    fun `save persists a new game session and findById retrieves it`() {
        val session = aGameSession()

        sessionRepository.save(session)

        val found = sessionRepository.findById(session.id)
        assertNotNull(found)
        assertEquals(session.id, found!!.id)
        assertEquals(GameStatus.WAITING_FOR_PLAYERS, found.status)
        assertEquals(RoundStatus.ACTIVE, found.roundStatus)
        assertEquals(1, found.currentRound)
    }

    @Test
    fun `findById returns null for unknown id`() {
        val result = sessionRepository.findById(UUID.randomUUID())
        assertNull(result)
    }

    @Test
    fun `save assigns both player ids correctly`() {
        val jailbreakerId = UUID.randomUUID()
        val guardianId = UUID.randomUUID()
        val session = aGameSession(jailbreakerId = jailbreakerId, guardianId = guardianId)

        sessionRepository.save(session)

        val found = sessionRepository.findById(session.id)!!
        assertEquals(jailbreakerId, found.jailbreakerId)
        assertEquals(guardianId, found.guardianId)
    }

    @Test
    fun `update persists new status and roundStatus`() {
        val session = aGameSession()
        sessionRepository.save(session)

        val updated = session.copy(status = GameStatus.IN_PROGRESS, roundStatus = RoundStatus.EVALUATING)
        sessionRepository.update(updated)

        val found = sessionRepository.findById(session.id)!!
        assertEquals(GameStatus.IN_PROGRESS, found.status)
        assertEquals(RoundStatus.EVALUATING, found.roundStatus)
    }

    @Test
    fun `update persists new currentRound`() {
        val session = aGameSession()
        sessionRepository.save(session)

        val updated =
            session.copy(
                status = GameStatus.IN_PROGRESS,
                roundStatus = RoundStatus.ACTIVE,
                currentRound = 2,
            )
        sessionRepository.update(updated)

        val found = sessionRepository.findById(session.id)!!
        assertEquals(2, found.currentRound)
    }

    @Test
    fun `SessionRepository does not expose delete operations`() {
        val methods = SessionRepository::class.java.methods.map { it.name }
        assert("delete" !in methods) { "delete must not be exposed on SessionRepository" }
        assert("deleteById" !in methods) { "deleteById must not be exposed on SessionRepository" }
        assert("deleteAll" !in methods) { "deleteAll must not be exposed on SessionRepository" }
    }
}
