package com.hospitaldischargecompanion.core.demo

import com.google.common.truth.Truth.assertThat
import com.hospitaldischargecompanion.core.alerts.DefaultAlertRepository
import com.hospitaldischargecompanion.core.alerts.EvaluateAlertRulesUseCase
import com.hospitaldischargecompanion.core.alerts.MockAlertApi
import com.hospitaldischargecompanion.core.discharge.DefaultCarePlanRepository
import com.hospitaldischargecompanion.core.discharge.EhrSyncStatus
import com.hospitaldischargecompanion.core.discharge.MockCarePlanApi
import com.hospitaldischargecompanion.core.ehr.DefaultEhrSyncRepository
import com.hospitaldischargecompanion.core.ehr.MockEhrSyncApi
import com.hospitaldischargecompanion.core.medication.DefaultMedicationRepository
import com.hospitaldischargecompanion.core.medication.MockMedicationApi
import com.hospitaldischargecompanion.core.telemonitor.DefaultTelemonitoringRepository
import com.hospitaldischargecompanion.core.telemonitor.MockTelemonitoringApi
import com.hospitaldischargecompanion.core.telemonitor.MockWearableIntegrationHook
import com.hospitaldischargecompanion.testing.DeterministicIdGenerator
import com.hospitaldischargecompanion.testing.FixedClinicalClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DemoScenarioRunnerTest {

    @Test
    fun executesAllSixDemoScenarioPresetsDeterministically() = runTest {
        val store = MockClinicalStore()
        val sim = DemoNetworkSimulator().apply { setLatencyMs(0L) }
        val clock = FixedClinicalClock()
        val ids = DeterministicIdGenerator()

        val careRepo = DefaultCarePlanRepository(MockCarePlanApi(store, sim, clock, ids))
        val medRepo = DefaultMedicationRepository(MockMedicationApi(store, sim, clock, ids))
        val teleRepo = DefaultTelemonitoringRepository(MockTelemonitoringApi(store, sim, clock, ids), MockWearableIntegrationHook())
        val alertRepo = DefaultAlertRepository(MockAlertApi(store, sim, clock, ids))
        val ehrRepo = DefaultEhrSyncRepository(MockEhrSyncApi(store, alertRepo, sim, clock, ids))

        val runner = DemoScenarioRunner(
            store, sim, careRepo, medRepo, teleRepo, EvaluateAlertRulesUseCase(alertRepo), ehrRepo, ids
        )
        val epId = ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID

        runner.apply(DemoScenarioPreset.FULL_ADHERENCE)
        assertThat(medRepo.observeAdherence(epId).first().adherencePercent).isEqualTo(100)

        runner.apply(DemoScenarioPreset.MISSED_DOSE)
        assertThat(alertRepo.observeEpisodeAlerts(epId).first().any { it.title.contains("Furosemide") }).isTrue()

        runner.apply(DemoScenarioPreset.CRITICAL_BP)
        assertThat(alertRepo.observePhysicianEscalatedQueue().first().any { it.episodeId == epId }).isTrue()

        runner.apply(DemoScenarioPreset.EHR_CONFLICT_409)
        assertThat(store.getEpisode(epId)?.ehrSyncStatus).isEqualTo(EhrSyncStatus.CONFLICT)

        runner.apply(DemoScenarioPreset.CAREGIVER_CONFIRMED_MEDS)
        assertThat(medRepo.observeMedications(epId).first().first { it.id == "med-hf-furosemide" }.hasPillBoxAttachment).isTrue()

        runner.apply(DemoScenarioPreset.PHYSICIAN_AMENDED_PLAN)
        assertThat(store.getEpisode(epId)?.carePlanRevision).isEqualTo(2)
    }
}
