package com.aiscream.promptduel.infrastructure.persistence

import com.aiscream.promptduel.domain.InjectionAttempt
import com.aiscream.promptduel.infrastructure.persistence.entity.InjectionAttemptEntity
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class JpaRoundRepository(
    private val jpa: InjectionAttemptJpaRepository,
) : RoundRepository {
    override fun save(attempt: InjectionAttempt): InjectionAttempt {
        val saved = jpa.save(InjectionAttemptEntity.fromDomain(attempt))
        return saved.toDomain()
    }

    override fun findAllByGameSessionId(gameId: UUID): List<InjectionAttempt> =
        jpa.findAllByGameSessionIdOrderByAttemptNumberAsc(gameId).map { it.toDomain() }
}
