package com.hospitaldischargecompanion.core.demo

import java.time.Instant
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

interface ClinicalClock {
    fun now(): Instant
}

interface IdGenerator {
    fun nextId(prefix: String): String
}

@Singleton
class SystemClinicalClock @Inject constructor() : ClinicalClock {
    override fun now(): Instant = Instant.now()
}

@Singleton
class SequentialIdGenerator @Inject constructor() : IdGenerator {
    private val counter = AtomicLong(1000L)

    override fun nextId(prefix: String): String = "$prefix-${counter.incrementAndGet()}"
}
