package com.aiscream.promptduel.infrastructure.persistence.entity

import com.aiscream.promptduel.domain.SystemPromptVersion
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "system_prompt_versions")
class SystemPromptVersionEntity(
    @Id
    val id: UUID,
    @Column(name = "game_session_id", nullable = false)
    val gameSessionId: UUID,
    @Column(name = "round_number", nullable = false)
    val roundNumber: Int,
    @Column(name = "version_number", nullable = false)
    val versionNumber: Int,
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    val content: String,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,
) {
    fun toDomain(): SystemPromptVersion =
        SystemPromptVersion(
            id = id,
            gameSessionId = gameSessionId,
            roundNumber = roundNumber,
            versionNumber = versionNumber,
            content = content,
            createdAt = createdAt,
        )

    companion object {
        fun fromDomain(version: SystemPromptVersion): SystemPromptVersionEntity =
            SystemPromptVersionEntity(
                id = version.id,
                gameSessionId = version.gameSessionId,
                roundNumber = version.roundNumber,
                versionNumber = version.versionNumber,
                content = version.content,
                createdAt = version.createdAt,
            )
    }
}
