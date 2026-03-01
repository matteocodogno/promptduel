package com.aiscream.promptduel.domain

import java.time.Instant
import java.util.UUID

/**
 * Pure domain representation of a game session.
 *
 * This is intentionally separate from the JPA entity (task 3.3).
 * The [version] field maps to the optimistic-lock `@Version` column
 * on the JPA entity to prevent concurrent update conflicts.
 */
data class GameSession(
    val id: UUID,
    val status: GameStatus,
    val roundStatus: RoundStatus,
    val currentRound: Int,
    val jailbreakerId: UUID?,
    val guardianId: UUID?,
    val version: Long,
    val createdAt: Instant,
    val updatedAt: Instant,
)
