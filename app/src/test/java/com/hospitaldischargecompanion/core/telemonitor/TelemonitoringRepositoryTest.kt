package com.hospitaldischargecompanion.core.telemonitor

import com.google.common.truth.Truth.assertThat
import com.hospitaldischargecompanion.core.demo.ClinicalSeedFactory
import com.hospitaldischargecompanion.core.demo.DemoNetworkSimulator
import com.hospitaldischargecompanion.core.demo.DomainError
import com.hospitaldischargecompanion.core.demo.DomainResult
import com.hospitaldischargecompanion.core.demo.MockClinicalStore
import com.hospitaldischargecompanion.core.discharge.EhrSyncStatus
import com.hospitaldischargecompanion.testing.DeterministicIdGenerator
import com.hospitaldischargecompanion.testing.FixedClinicalClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class TelemonitoringRepositoryTest {

    private lateinit var store: MockClinicalStore
    private lateinit var repository: TelemonitoringRepository

    @Before
    fun setUp() {
        store = MockClinicalStore()
        val simulator = DemoNetworkSimulator().apply { setLatencyMs(0L) }
        repository = DefaultTelemonitoringRepository(
            dataSource = MockTelemonitoringApi(
                store = store,
                networkSimulator = simulator,
                clock = FixedClinicalClock(),
                idGenerator = DeterministicIdGenerator()
            ),
            wearableHook = MockWearableIntegrationHook()
        )
    }

    @Test
    fun rejectsOutOfDeviceRangeVitalsWithDomainError() = runTest {
        val result = repository.submitCheckIn(
            NewCheckInRequest(
                episodeId = ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID,
                systolicMmHg = 320,
                diastolicMmHg = 80,
                heartRateBpm = 78,
                spo2Percent = 96,
                weightKg = 77.0,
                temperatureC = 36.7,
                painScore = 1,
                dyspneaSeverity = 1,
                swellingSeverity = 1,
                freeTextNotes = "",
                recordedByLabel = "João Silva",
                idempotencyKey = "bad-bp"
            )
        )
        assertThat(result).isInstanceOf(DomainResult.Failure::class.java)
        assertThat((result as DomainResult.Failure).error).isInstanceOf(DomainError.VitalOutOfRange::class.java)
    }

    @Test
    fun submitsValidCheckInMarksEhrPendingAndDeduplicatesRetry() = runTest {
        val request = NewCheckInRequest(
            episodeId = ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID,
            systolicMmHg = 172,
            diastolicMmHg = 104,
            heartRateBpm = 94,
            spo2Percent = 93,
            weightKg = 78.8,
            temperatureC = 36.8,
            painScore = 2,
            dyspneaSeverity = 3,
            swellingSeverity = 3,
            freeTextNotes = "Short of breath climbing stairs",
            recordedByLabel = "João Silva",
            idempotencyKey = "chk-idem-1"
        )

        val first = repository.submitCheckIn(request)
        val retry = repository.submitCheckIn(request)

        assertThat(first).isEqualTo(retry)
        val saved = (first as DomainResult.Success).data
        assertThat(saved.isBloodPressureAbnormal).isTrue()
        assertThat(store.getEpisode(ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID)?.ehrSyncStatus)
            .isEqualTo(EhrSyncStatus.PENDING)
        assertThat(repository.observeCheckIns(ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID).first()).hasSize(4)
    }
}
