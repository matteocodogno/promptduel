package com.aiscream.promptduel.infrastructure.persistence

import com.aiscream.promptduel.domain.InjectionAttempt
import java.util.UUID

interface RoundRepository {
    fun save(attempt: InjectionAttempt): InjectionAttempt

    fun findAllByGameSessionId(gameId: UUID): List<InjectionAttempt>
}
