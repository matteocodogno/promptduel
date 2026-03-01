package com.aiscream.promptduel.infrastructure.persistence

import com.aiscream.promptduel.domain.GameSession
import com.aiscream.promptduel.infrastructure.persistence.entity.GameSessionEntity
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
class JpaSessionRepository(
    private val jpa: GameSessionJpaRepository,
) : SessionRepository {
    override fun save(session: GameSession): GameSession {
        val saved = jpa.save(GameSessionEntity.fromDomain(session))
        return saved.toDomain()
    }

    override fun findById(gameId: UUID): GameSession? = jpa.findById(gameId).orElse(null)?.toDomain()

    override fun findByPlayerId(playerId: UUID): GameSession? =
        jpa.findByJailbreakerIdOrGuardianId(playerId, playerId)?.toDomain()

    override fun update(session: GameSession): GameSession {
        val entity =
            jpa.findById(session.id).orElseThrow {
                IllegalArgumentException("GameSession not found: ${session.id}")
            }
        entity.status = session.status
        entity.roundStatus = session.roundStatus
        entity.currentRound = session.currentRound
        entity.updatedAt = Instant.now()
        return jpa.save(entity).toDomain()
    }
}
