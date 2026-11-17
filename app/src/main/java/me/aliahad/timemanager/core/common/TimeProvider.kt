package me.aliahad.timemanager.core.common

import android.os.SystemClock
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Abstraction over wall-clock and monotonic time for deterministic testing.
 */
interface TimeProvider {
    fun nowMillis(): Long
    fun today(): LocalDate
    fun nowTime(): LocalTime
    fun nowDateTime(): LocalDateTime
    fun zoneId(): ZoneId
    fun elapsedRealtimeMillis(): Long
}

class SystemTimeProvider(
    private val clock: Clock = Clock.systemDefaultZone()
) : TimeProvider {
    override fun nowMillis(): Long = clock.millis()
    override fun today(): LocalDate = LocalDate.now(clock)
    override fun nowTime(): LocalTime = LocalTime.now(clock)
    override fun nowDateTime(): LocalDateTime = LocalDateTime.now(clock)
    override fun zoneId(): ZoneId = clock.zone
    override fun elapsedRealtimeMillis(): Long = SystemClock.elapsedRealtime()
}
