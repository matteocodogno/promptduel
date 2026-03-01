package com.aiscream.promptduel.infrastructure.persistence

import com.aiscream.promptduel.infrastructure.persistence.entity.InjectionAttemptEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface InjectionAttemptJpaRepository : JpaRepository<InjectionAttemptEntity, UUID> {
    fun findAllByGameSessionIdOrderByAttemptNumberAsc(gameSessionId: UUID): List<InjectionAttemptEntity>
}
