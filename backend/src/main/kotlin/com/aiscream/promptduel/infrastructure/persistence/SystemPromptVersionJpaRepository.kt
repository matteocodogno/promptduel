package com.aiscream.promptduel.infrastructure.persistence

import com.aiscream.promptduel.infrastructure.persistence.entity.SystemPromptVersionEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface SystemPromptVersionJpaRepository : JpaRepository<SystemPromptVersionEntity, UUID> {
    fun findAllByGameSessionIdOrderByVersionNumberAsc(gameSessionId: UUID): List<SystemPromptVersionEntity>

    @Query(
        "SELECT s FROM SystemPromptVersionEntity s WHERE s.gameSessionId = :gameSessionId " +
            "ORDER BY s.versionNumber DESC LIMIT 1",
    )
    fun findLatestByGameSessionId(gameSessionId: UUID): SystemPromptVersionEntity?
}
