package com.aiscream.promptduel.infrastructure.persistence

import com.aiscream.promptduel.infrastructure.persistence.entity.GameSessionEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface GameSessionJpaRepository : JpaRepository<GameSessionEntity, UUID> {
    fun findByJailbreakerIdOrGuardianId(
        jailbreakerId: UUID,
        guardianId: UUID,
    ): GameSessionEntity?
}
