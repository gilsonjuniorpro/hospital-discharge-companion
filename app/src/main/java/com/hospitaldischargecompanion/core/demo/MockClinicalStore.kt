package com.hospitaldischargecompanion.core.demo

import com.hospitaldischargecompanion.core.discharge.DischargeEpisode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shared in-memory source of truth across Patient, Family, Nurse, and Physician roles.
 * In Phase 2, individual repositories swap this mock store for remote FHIR/EHR adapters.
 */
@Singleton
class MockClinicalStore @Inject constructor() {
    private val _episodes = MutableStateFlow(ClinicalSeedFactory.createEpisodes())
    val episodes: StateFlow<List<DischargeEpisode>> = _episodes.asStateFlow()

    private val resetCallbacks = mutableListOf<() -> Unit>()

    fun registerResetCallback(callback: () -> Unit) {
        resetCallbacks.add(callback)
    }

    fun getEpisode(episodeId: String): DischargeEpisode? =
        _episodes.value.firstOrNull { it.id == episodeId }

    fun updateEpisode(episodeId: String, transform: (DischargeEpisode) -> DischargeEpisode): DischargeEpisode? {
        var updated: DischargeEpisode? = null
        _episodes.update { list ->
            list.map { episode ->
                if (episode.id == episodeId) {
                    transform(episode).also { updated = it }
                } else {
                    episode
                }
            }
        }
        return updated
    }

    fun resetAll() {
        _episodes.value = ClinicalSeedFactory.createEpisodes()
        resetCallbacks.forEach { it.invoke() }
    }
}
