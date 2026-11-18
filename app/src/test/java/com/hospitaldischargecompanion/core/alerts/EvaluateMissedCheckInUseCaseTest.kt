package com.hospitaldischargecompanion.core.alerts

import com.google.common.truth.Truth.assertThat
import com.hospitaldischargecompanion.core.demo.ClinicalSeedFactory
import com.hospitaldischargecompanion.core.demo.DemoNetworkSimulator
import com.hospitaldischargecompanion.core.demo.DomainResult
import com.hospitaldischargecompanion.core.demo.MockClinicalStore
import com.hospitaldischargecompanion.core.telemonitor.TelemonitorSeedFactory
import com.hospitaldischargecompanion.testing.DeterministicIdGenerator
import com.hospitaldischargecompanion.testing.FixedClinicalClock
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Duration
import java.time.Instant

class EvaluateMissedCheckInUseCaseTest {

    @Test
    fun raisesUrgentAlertOnlyWhenHighRiskEpisodeExceeds24HourWindow() = runTest {
        val store = MockClinicalStore()
        val sim = DemoNetworkSimulator().apply { setLatencyMs(0L) }
        val clock = FixedClinicalClock(Instant.parse("2026-06-14T20:00:00Z"))
        val ids = DeterministicIdGenerator()
        val alertRepo = DefaultAlertRepository(MockAlertApi(store, sim, clock, ids))
        val useCase = EvaluateMissedCheckInUseCase(alertRepo, clock)

        val hfEpisode = store.getEpisode(ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID)!!
        val hfCheckIns = TelemonitorSeedFactory.createCheckIns().filter { it.episodeId == hfEpisode.id }

        // 11h40m after last reading (2026-06-14T08:20:00Z) -> within 24h window
        val withinWindow = useCase.evaluate(hfEpisode, hfCheckIns)
        assertThat((withinWindow as DomainResult.Success).data).isNull()

        // Advance 14 hours -> 25h40m elapsed -> overdue!
        clock.advance(Duration.ofHours(14))
        val overdue = useCase.evaluate(hfEpisode, hfCheckIns)
        val alert = (overdue as DomainResult.Success).data
        assertThat(alert).isNotNull()
        assertThat(alert?.severity).isEqualTo(AlertSeverity.URGENT)
        assertThat(alert?.title).contains("Missed telemonitoring check-in")
    }
}
