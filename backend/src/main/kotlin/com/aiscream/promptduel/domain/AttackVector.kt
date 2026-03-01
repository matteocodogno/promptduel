package com.aiscream.promptduel.domain

data class AttackVector(
    val roundNumber: Int,
    val name: String,
    val description: String,
    val tier1Hint: String,
    val tier2HintExample: String,
)

interface AttackVectorRegistry {
    fun getVector(roundNumber: Int): AttackVector

    fun getAllVectors(): List<AttackVector>

    fun getBaseSystemPrompt(): String

    fun getVulnerableCodeSample(): String
}
