package com.hospitaldischargecompanion.core.discharge

import com.google.common.truth.Truth.assertThat
import com.hospitaldischargecompanion.core.demo.ClinicalSeedFactory
import com.hospitaldischargecompanion.core.demo.DemoNetworkSimulator
import com.hospitaldischargecompanion.core.demo.DomainError
import com.hospitaldischargecompanion.core.demo.DomainResult
import com.hospitaldischargecompanion.core.demo.MockClinicalStore
import com.hospitaldischargecompanion.core.demo.SimulatedFailureMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class DischargeEpisodeRepositoryTest {

    private lateinit var store: MockClinicalStore
    private lateinit var simulator: DemoNetworkSimulator
    private lateinit var repository: DischargeEpisodeRepository

    @Before
    fun setUp() {
        store = MockClinicalStore()
        simulator = DemoNetworkSimulator().apply { setLatencyMs(0L) }
        repository = DefaultDischargeEpisodeRepository(MockDischargeApi(store, simulator))
    }

    @Test
    fun seedsThreeDistinctDischargeCasesWithUniqueInviteCodes() = runTest {
        val episodes = repository.observeAllEpisodes().first()
        assertThat(episodes).hasSize(3)
        assertThat(episodes.map { it.caseType }).containsExactly(
            EpisodeCaseType.HEART_FAILURE,
            EpisodeCaseType.POST_SURGERY,
            EpisodeCaseType.PNEUMONIA
        )
        assertThat(episodes.map { it.careCircleInviteCode }.distinct()).hasSize(3)
        episodes.forEach { episode ->
            assertThat(episode.redFlags).isNotEmpty()
        }
    }

    @Test
    fun returnsEpisodeNotFoundForUnknownIdentifier() = runTest {
        val result = repository.getEpisode("ep-unknown")
        assertThat(result).isEqualTo(DomainResult.Failure(DomainError.EpisodeNotFound("ep-unknown")))
    }

    @Test
    fun pullsFhirSummaryAndRespectsEhrFailureModes() = runTest {
        val ok = repository.pullFhirSummary(ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID)
        assertThat(ok).isInstanceOf(DomainResult.Success::class.java)
        val (patient, encounter) = (ok as DomainResult.Success).data
        assertThat(patient.resourceType).isEqualTo("Patient")
        assertThat(encounter.resourceType).isEqualTo("Encounter")
        assertThat(encounter.patientRef).isEqualTo("Patient/${patient.id}")

        simulator.setFailureMode(SimulatedFailureMode.EHR_UNAUTHORIZED_401)
        val unauthorized = repository.pullFhirSummary(ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID)
        assertThat(unauthorized).isEqualTo(DomainResult.Failure(DomainError.EhrUnauthorized))
    }

    @Test
    fun resetAllRestoresModifiedConsentState() = runTest {
        repository.setConsentSigned(ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID, false)
        assertThat(
            repository.observeEpisode(ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID).first()?.consentSigned
        ).isFalse()

        store.resetAll()
        assertThat(
            repository.observeEpisode(ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID).first()?.consentSigned
        ).isTrue()
    }
}
