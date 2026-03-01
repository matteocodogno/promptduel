package com.aiscream.promptduel.infrastructure.persistence.entity

import com.aiscream.promptduel.domain.GameSession
import com.aiscream.promptduel.domain.GameStatus
import com.aiscream.promptduel.domain.RoundStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "game_sessions")
class GameSessionEntity(
    @Id
    val id: UUID,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    var status: GameStatus,
    @Enumerated(EnumType.STRING)
    @Column(name = "round_status", nullable = false, length = 30)
    var roundStatus: RoundStatus,
    @Column(name = "current_round", nullable = false)
    var currentRound: Int,
    @Column(name = "jailbreaker_id")
    val jailbreakerId: UUID?,
    @Column(name = "guardian_id")
    val guardianId: UUID?,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0,
) {
    fun toDomain(): GameSession =
        GameSession(
            id = id,
            status = status,
            roundStatus = roundStatus,
            currentRound = currentRound,
            jailbreakerId = jailbreakerId,
            guardianId = guardianId,
            version = version,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    companion object {
        fun fromDomain(session: GameSession): GameSessionEntity =
            GameSessionEntity(
                id = session.id,
                status = session.status,
                roundStatus = session.roundStatus,
                currentRound = session.currentRound,
                jailbreakerId = session.jailbreakerId,
                guardianId = session.guardianId,
                createdAt = session.createdAt,
                updatedAt = session.updatedAt,
                version = session.version,
            )
    }
}
