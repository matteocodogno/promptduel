package com.aiscream.promptduel.infrastructure.persistence

import com.aiscream.promptduel.domain.SystemPromptVersion
import com.aiscream.promptduel.infrastructure.persistence.entity.SystemPromptVersionEntity
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class JpaSystemPromptRepository(
    private val jpa: SystemPromptVersionJpaRepository,
) : SystemPromptRepository {
    override fun save(version: SystemPromptVersion): SystemPromptVersion {
        val saved = jpa.save(SystemPromptVersionEntity.fromDomain(version))
        return saved.toDomain()
    }

    override fun findLatestByGameSessionId(gameId: UUID): SystemPromptVersion? =
        jpa.findLatestByGameSessionId(gameId)?.toDomain()

    override fun findAllByGameSessionId(gameId: UUID): List<SystemPromptVersion> =
        jpa.findAllByGameSessionIdOrderByVersionNumberAsc(gameId).map { it.toDomain() }
}
