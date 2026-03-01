package com.aiscream.promptduel.infrastructure.persistence

import com.aiscream.promptduel.domain.SystemPromptVersion
import java.util.UUID

interface SystemPromptRepository {
    fun save(version: SystemPromptVersion): SystemPromptVersion

    fun findLatestByGameSessionId(gameId: UUID): SystemPromptVersion?

    fun findAllByGameSessionId(gameId: UUID): List<SystemPromptVersion>
}
