package com.akaitigo.podflow.model

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class EpisodeStatusTest {

    @ParameterizedTest(name = "{0} -> {1} should be allowed")
    @CsvSource(
        "PLANNING, GUEST_COORDINATION",
        "PLANNING, RECORDING",
        "GUEST_COORDINATION, RECORDING",
        "RECORDING, EDITING",
        "EDITING, REVIEW",
        "REVIEW, EDITING",
        "REVIEW, PUBLISHED",
    )
    fun `valid transitions are allowed`(from: EpisodeStatus, to: EpisodeStatus) {
        assertTrue(from.canTransitionTo(to)) {
            "$from -> $to should be a valid transition"
        }
    }

    @ParameterizedTest(name = "{0} -> {1} should be rejected")
    @CsvSource(
        "PLANNING, EDITING",
        "PLANNING, REVIEW",
        "PLANNING, PUBLISHED",
        "GUEST_COORDINATION, PLANNING",
        "GUEST_COORDINATION, EDITING",
        "RECORDING, PLANNING",
        "RECORDING, PUBLISHED",
        "EDITING, PLANNING",
        "EDITING, RECORDING",
        "REVIEW, PLANNING",
        "REVIEW, RECORDING",
        "PUBLISHED, PLANNING",
        "PUBLISHED, RECORDING",
        "PUBLISHED, EDITING",
    )
    fun `invalid transitions are rejected`(from: EpisodeStatus, to: EpisodeStatus) {
        assertFalse(from.canTransitionTo(to)) {
            "$from -> $to should not be a valid transition"
        }
    }

    @Test
    fun `published has no allowed transitions`() {
        assertTrue(EpisodeStatus.PUBLISHED.allowedTransitions().isEmpty())
    }

    @Test
    fun `planning allows guest coordination and recording`() {
        val allowed = EpisodeStatus.PLANNING.allowedTransitions()
        assertTrue(allowed.contains(EpisodeStatus.GUEST_COORDINATION))
        assertTrue(allowed.contains(EpisodeStatus.RECORDING))
    }

    @Test
    fun `self transition is not allowed for any status`() {
        EpisodeStatus.entries.forEach { status ->
            assertFalse(status.canTransitionTo(status)) {
                "$status -> $status (self transition) should not be allowed"
            }
        }
    }
}
