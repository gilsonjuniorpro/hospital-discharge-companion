package com.hospitaldischargecompanion.testing

import com.hospitaldischargecompanion.core.demo.ClinicalClock
import com.hospitaldischargecompanion.core.demo.IdGenerator
import java.time.Duration
import java.time.Instant

class FixedClinicalClock(
    private var current: Instant = Instant.parse("2026-06-15T12:00:00Z")
) : ClinicalClock {
    override fun now(): Instant = current

    fun advance(duration: Duration) {
        current = current.plus(duration)
    }
}

class DeterministicIdGenerator : IdGenerator {
    private var sequence = 0

    override fun nextId(prefix: String): String = "$prefix-${++sequence}"
}
