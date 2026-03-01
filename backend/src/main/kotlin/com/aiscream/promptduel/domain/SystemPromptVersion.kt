package com.aiscream.promptduel.domain

import java.time.Instant
import java.util.UUID

data class SystemPromptVersion(
    val id: UUID,
    val gameSessionId: UUID,
    val roundNumber: Int,
    val versionNumber: Int,
    val content: String,
    val createdAt: Instant,
)
