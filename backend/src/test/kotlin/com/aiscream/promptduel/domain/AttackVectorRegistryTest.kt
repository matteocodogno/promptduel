package com.aiscream.promptduel.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AttackVectorRegistryTest {
    private val registry: AttackVectorRegistry = InMemoryAttackVectorRegistry()

    // ── getAllVectors ─────────────────────────────────────────────────────────

    @Test
    fun `getAllVectors returns exactly four vectors`() {
        assertEquals(4, registry.getAllVectors().size)
    }

    @Test
    fun `getAllVectors returns vectors in round order`() {
        val rounds = registry.getAllVectors().map { it.roundNumber }
        assertEquals(listOf(1, 2, 3, 4), rounds)
    }

    // ── getVector ─────────────────────────────────────────────────────────────

    @Test
    fun `getVector round 1 is Direct Override`() {
        val v = registry.getVector(1)
        assertEquals(1, v.roundNumber)
        assertEquals("Direct Override", v.name)
    }

    @Test
    fun `getVector round 2 is Role Confusion`() {
        val v = registry.getVector(2)
        assertEquals(2, v.roundNumber)
        assertEquals("Role Confusion", v.name)
    }

    @Test
    fun `getVector round 3 is Context Manipulation`() {
        val v = registry.getVector(3)
        assertEquals(3, v.roundNumber)
        assertEquals("Context Manipulation", v.name)
    }

    @Test
    fun `getVector round 4 is Indirect Injection`() {
        val v = registry.getVector(4)
        assertEquals(4, v.roundNumber)
        assertEquals("Indirect Injection", v.name)
    }

    @Test
    fun `getVector throws for round 0`() {
        assertThrows(IllegalArgumentException::class.java) { registry.getVector(0) }
    }

    @Test
    fun `getVector throws for round 5`() {
        assertThrows(IllegalArgumentException::class.java) { registry.getVector(5) }
    }

    // ── vector content completeness ───────────────────────────────────────────

    @Test
    fun `every vector has a non-blank description`() {
        registry.getAllVectors().forEach { v ->
            assertFalse(v.description.isBlank(), "Round ${v.roundNumber} description is blank")
        }
    }

    @Test
    fun `every vector has a non-blank tier-1 hint`() {
        registry.getAllVectors().forEach { v ->
            assertFalse(v.tier1Hint.isBlank(), "Round ${v.roundNumber} tier1Hint is blank")
        }
    }

    @Test
    fun `every vector has a non-blank tier-2 hint example`() {
        registry.getAllVectors().forEach { v ->
            assertFalse(v.tier2HintExample.isBlank(), "Round ${v.roundNumber} tier2HintExample is blank")
        }
    }

    // ── getBaseSystemPrompt ───────────────────────────────────────────────────

    @Test
    fun `getBaseSystemPrompt returns a non-blank string`() {
        assertFalse(registry.getBaseSystemPrompt().isBlank())
    }

    @Test
    fun `getBaseSystemPrompt is intentionally short and weak`() {
        // The base prompt is deliberately minimal so round-1 can defeat it immediately.
        assertTrue(
            registry.getBaseSystemPrompt().length < 300,
            "Base system prompt should be intentionally short/weak",
        )
    }

    // ── getVulnerableCodeSample ───────────────────────────────────────────────

    @Test
    fun `getVulnerableCodeSample returns a non-blank code snippet`() {
        assertFalse(registry.getVulnerableCodeSample().isBlank())
    }

    @Test
    fun `getVulnerableCodeSample contains SQL injection vulnerability`() {
        val code = registry.getVulnerableCodeSample()
        assertNotNull(code)
        // The sample must actually contain string concatenation into a SQL query
        // so players can see the vulnerability.
        assertTrue(
            code.contains("SELECT") || code.contains("select"),
            "Vulnerable code sample must contain a SQL query",
        )
    }
}
