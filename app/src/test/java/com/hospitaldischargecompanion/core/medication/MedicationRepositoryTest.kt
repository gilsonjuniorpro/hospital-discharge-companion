package com.hospitaldischargecompanion.core.medication

import com.google.common.truth.Truth.assertThat
import com.hospitaldischargecompanion.core.demo.ClinicalSeedFactory
import com.hospitaldischargecompanion.core.demo.DemoNetworkSimulator
import com.hospitaldischargecompanion.core.demo.DomainError
import com.hospitaldischargecompanion.core.demo.DomainResult
import com.hospitaldischargecompanion.core.demo.MockClinicalStore
import com.hospitaldischargecompanion.testing.DeterministicIdGenerator
import com.hospitaldischargecompanion.testing.FixedClinicalClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class MedicationRepositoryTest {

    private lateinit var store: MockClinicalStore
    private lateinit var repository: MedicationRepository

    @Before
    fun setUp() {
        store = MockClinicalStore()
        val simulator = DemoNetworkSimulator().apply { setLatencyMs(0L) }
        repository = DefaultMedicationRepository(
            MockMedicationApi(
                store = store,
                networkSimulator = simulator,
                clock = FixedClinicalClock(),
                idGenerator = DeterministicIdGenerator()
            )
        )
    }

    @Test
    fun recordsDoseWithDualAttributionAndDeduplicatesIdempotencyKey() = runTest {
        val first = repository.recordDose(
            medicationId = "med-hf-furosemide",
            action = DoseActionType.TAKEN,
            attributionLabel = "Confirmed by Maria on behalf of João",
            idempotencyKey = "idem-dose-1"
        )
        val duplicate = repository.recordDose(
            medicationId = "med-hf-furosemide",
            action = DoseActionType.TAKEN,
            attributionLabel = "Confirmed by Maria on behalf of João",
            idempotencyKey = "idem-dose-1"
        )

        assertThat(first).isEqualTo(duplicate)
        val logs = repository.observeDoseLogs(ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID).first()
        assertThat(logs).hasSize(1)
        assertThat(logs.first().attributionLabel).contains("Maria")
    }

    @Test
    fun rejectsDiscontinuedMedicationAndComputesAdherencePercent() = runTest {
        val stopped = repository.recordDose(
            medicationId = "med-hf-ibuprofen",
            action = DoseActionType.TAKEN,
            attributionLabel = "João Silva"
        )
        assertThat(stopped).isEqualTo(
            DomainResult.Failure(DomainError.MedicationDoseWindowClosed("med-hf-ibuprofen"))
        )

        repository.recordDose("med-hf-furosemide", DoseActionType.TAKEN, "João Silva", idempotencyKey = "k1")
        repository.recordDose("med-hf-entresto", DoseActionType.TAKEN, "João Silva", idempotencyKey = "k2")
        repository.recordDose("med-hf-carvedilol", DoseActionType.TAKEN, "João Silva", idempotencyKey = "k3")

        val adherence = repository.observeAdherence(ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID).first()
        assertThat(adherence.totalScheduledActive).isEqualTo(3)
        assertThat(adherence.takenCount).isEqualTo(3)
        assertThat(adherence.adherencePercent).isEqualTo(100)
    }

    @Test
    fun attachesPillBoxPhotoAndProcessesRefill() = runTest {
        val photoResult = repository.attachMockPillBoxPhoto("med-hf-furosemide", "Morning pill organizer verified")
        assertThat((photoResult as DomainResult.Success).data.hasPillBoxAttachment).isTrue()

        val refillResult = repository.requestRefill("med-hf-furosemide")
        assertThat((refillResult as DomainResult.Success).data.refillStatus).isEqualTo(RefillStatus.IN_STOCK)
        assertThat(refillResult.data.daysSupplyRemaining).isEqualTo(30)
    }
}
