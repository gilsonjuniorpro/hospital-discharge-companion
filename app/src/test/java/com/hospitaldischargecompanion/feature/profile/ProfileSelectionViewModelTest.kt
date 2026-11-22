package com.hospitaldischargecompanion.feature.profile

import com.google.common.truth.Truth.assertThat
import com.hospitaldischargecompanion.core.demo.AppLocale
import com.hospitaldischargecompanion.core.demo.ClinicalSeedFactory
import com.hospitaldischargecompanion.core.demo.DemoNetworkSimulator
import com.hospitaldischargecompanion.core.demo.MockClinicalStore
import com.hospitaldischargecompanion.core.demo.UserRole
import com.hospitaldischargecompanion.core.discharge.DefaultDischargeEpisodeRepository
import com.hospitaldischargecompanion.core.discharge.MockDischargeApi
import com.hospitaldischargecompanion.testing.FakeDemoSessionRepository
import com.hospitaldischargecompanion.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileSelectionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun loadsSeededEpisodesAndPersistsSelectedRoleAndEpisode() = runTest {
        val sessionRepo = FakeDemoSessionRepository()
        val store = MockClinicalStore()
        val simulator = DemoNetworkSimulator().apply { setLatencyMs(0L) }
        val episodeRepo = DefaultDischargeEpisodeRepository(MockDischargeApi(store, simulator))
        val viewModel = ProfileSelectionViewModel(sessionRepo, episodeRepo)

        val collector = launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isLoading).isFalse()
        assertThat(viewModel.uiState.value.episodes).hasSize(3)

        viewModel.selectEpisode(ClinicalSeedFactory.POST_SURGERY_EPISODE_ID)
        viewModel.selectLocale(AppLocale.PT_BR)
        advanceUntilIdle()

        assertThat(sessionRepo.current.selectedEpisodeId).isEqualTo(ClinicalSeedFactory.POST_SURGERY_EPISODE_ID)
        assertThat(sessionRepo.current.locale).isEqualTo(AppLocale.PT_BR)

        var navigatedRole: UserRole? = null
        var navigatedEpisode: String? = null
        viewModel.selectRole(UserRole.NURSE) { role, ep ->
            navigatedRole = role
            navigatedEpisode = ep
        }
        advanceUntilIdle()

        assertThat(sessionRepo.current.activeRole).isEqualTo(UserRole.NURSE)
        assertThat(navigatedRole).isEqualTo(UserRole.NURSE)
        assertThat(navigatedEpisode).isEqualTo(ClinicalSeedFactory.POST_SURGERY_EPISODE_ID)
        collector.cancel()
    }
}
