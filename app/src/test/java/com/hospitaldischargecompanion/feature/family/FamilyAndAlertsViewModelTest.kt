package com.hospitaldischargecompanion.feature.family

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.hospitaldischargecompanion.core.alerts.AlertStatus
import com.hospitaldischargecompanion.core.alerts.DefaultAlertRepository
import com.hospitaldischargecompanion.core.alerts.EscalationStage
import com.hospitaldischargecompanion.core.alerts.MockAlertApi
import com.hospitaldischargecompanion.core.carecircle.DefaultCareCircleRepository
import com.hospitaldischargecompanion.core.carecircle.MockCareCircleApi
import com.hospitaldischargecompanion.core.demo.ClinicalSeedFactory
import com.hospitaldischargecompanion.core.demo.DemoNetworkSimulator
import com.hospitaldischargecompanion.core.demo.DemoSessionState
import com.hospitaldischargecompanion.core.demo.MockClinicalStore
import com.hospitaldischargecompanion.core.demo.UserRole
import com.hospitaldischargecompanion.core.discharge.DefaultCarePlanRepository
import com.hospitaldischargecompanion.core.discharge.DefaultDischargeEpisodeRepository
import com.hospitaldischargecompanion.core.discharge.MockCarePlanApi
import com.hospitaldischargecompanion.core.discharge.MockDischargeApi
import com.hospitaldischargecompanion.feature.alerts.AlertsViewModel
import com.hospitaldischargecompanion.testing.DeterministicIdGenerator
import com.hospitaldischargecompanion.testing.FakeDemoSessionRepository
import com.hospitaldischargecompanion.testing.FixedClinicalClock
import com.hospitaldischargecompanion.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FamilyAndAlertsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun familyEscalatesAndAlertsViewModelAdvancesEscalationLadder() = runTest {
        val store = MockClinicalStore()
        val sim = DemoNetworkSimulator().apply { setLatencyMs(0L) }
        val clock = FixedClinicalClock()
        val ids = DeterministicIdGenerator()
        val session = FakeDemoSessionRepository(DemoSessionState(activeRole = UserRole.FAMILY))
        val epRepo = DefaultDischargeEpisodeRepository(MockDischargeApi(store, sim))
        val careRepo = DefaultCarePlanRepository(MockCarePlanApi(store, sim, clock, ids))
        val alertRepo = DefaultAlertRepository(MockAlertApi(store, sim, clock, ids))
        val circleRepo = DefaultCareCircleRepository(MockCareCircleApi(store, sim, ids))

        val familyVm = FamilyHomeViewModel(session, epRepo, careRepo, alertRepo, circleRepo)
        val alertsVm = AlertsViewModel(
            savedStateHandle = SavedStateHandle(mapOf("episodeId" to ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID)),
            sessionRepository = session,
            episodeRepository = epRepo,
            alertRepository = alertRepo
        )

        val j1 = launch { familyVm.uiState.collect() }
        val j2 = launch { alertsVm.uiState.collect() }
        advanceUntilIdle()

        familyVm.escalateOnBehalfOfPatient("Shortness of breath worsening")
        advanceUntilIdle()

        assertThat(familyVm.uiState.value.feedbackBanner).contains("Escalated")
        val escalated = alertsVm.uiState.value.alerts.first { it.id == "alert-hf-weight" }
        assertThat(escalated.status).isEqualTo(AlertStatus.ESCALATED)
        assertThat(escalated.escalationStage).isEqualTo(EscalationStage.PHYSICIAN_REVIEW)

        j1.cancel()
        j2.cancel()
    }
}
