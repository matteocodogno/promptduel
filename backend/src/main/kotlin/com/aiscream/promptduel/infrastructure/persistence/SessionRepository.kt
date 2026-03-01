package com.aiscream.promptduel.infrastructure.persistence

import com.aiscream.promptduel.domain.GameSession
import java.util.UUID

interface SessionRepository {
    fun save(session: GameSession): GameSession

    fun findById(gameId: UUID): GameSession?

    fun findByPlayerId(playerId: UUID): GameSession?

    fun update(session: GameSession): GameSession
}
