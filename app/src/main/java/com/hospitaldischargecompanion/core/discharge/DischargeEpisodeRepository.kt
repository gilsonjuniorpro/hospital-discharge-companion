package com.hospitaldischargecompanion.core.discharge

import com.hospitaldischargecompanion.core.demo.DomainResult
import com.hospitaldischargecompanion.core.ehr.EncounterDto
import com.hospitaldischargecompanion.core.ehr.PatientDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface DischargeEpisodeRepository {
    fun observeAllEpisodes(): Flow<List<DischargeEpisode>>
    fun observeEpisode(episodeId: String): Flow<DischargeEpisode?>
    suspend fun getEpisode(episodeId: String): DomainResult<DischargeEpisode>
    suspend fun pullFhirSummary(episodeId: String): DomainResult<Pair<PatientDto, EncounterDto>>
    suspend fun setConsentSigned(episodeId: String, signed: Boolean): DomainResult<DischargeEpisode>
}

@Singleton
class DefaultDischargeEpisodeRepository @Inject constructor(
    private val dataSource: DischargeDataSource
) : DischargeEpisodeRepository {

    override fun observeAllEpisodes(): Flow<List<DischargeEpisode>> =
        dataSource.observeEpisodes()

    override fun observeEpisode(episodeId: String): Flow<DischargeEpisode?> =
        dataSource.observeEpisodes().map { list -> list.firstOrNull { it.id == episodeId } }

    override suspend fun getEpisode(episodeId: String): DomainResult<DischargeEpisode> =
        dataSource.fetchEpisode(episodeId)

    override suspend fun pullFhirSummary(episodeId: String): DomainResult<Pair<PatientDto, EncounterDto>> =
        dataSource.fetchFhirSummary(episodeId)

    override suspend fun setConsentSigned(episodeId: String, signed: Boolean): DomainResult<DischargeEpisode> =
        dataSource.updateConsent(episodeId, signed)
}
