package com.hospitaldischargecompanion.core.discharge

import com.hospitaldischargecompanion.core.demo.DemoNetworkSimulator
import com.hospitaldischargecompanion.core.demo.DomainError
import com.hospitaldischargecompanion.core.demo.DomainResult
import com.hospitaldischargecompanion.core.demo.MockClinicalStore
import com.hospitaldischargecompanion.core.ehr.EncounterDto
import com.hospitaldischargecompanion.core.ehr.PatientDto
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface DischargeDataSource {
    fun observeEpisodes(): Flow<List<DischargeEpisode>>
    suspend fun fetchEpisode(episodeId: String): DomainResult<DischargeEpisode>
    suspend fun fetchFhirSummary(episodeId: String): DomainResult<Pair<PatientDto, EncounterDto>>
    suspend fun updateConsent(episodeId: String, signed: Boolean): DomainResult<DischargeEpisode>
}

@Singleton
class MockDischargeApi @Inject constructor(
    private val store: MockClinicalStore,
    private val networkSimulator: DemoNetworkSimulator
) : DischargeDataSource {

    override fun observeEpisodes(): Flow<List<DischargeEpisode>> = store.episodes

    override suspend fun fetchEpisode(episodeId: String): DomainResult<DischargeEpisode> =
        networkSimulator.execute {
            val found = store.getEpisode(episodeId)
            if (found != null) {
                DomainResult.Success(found)
            } else {
                DomainResult.Failure(DomainError.EpisodeNotFound(episodeId))
            }
        }

    override suspend fun fetchFhirSummary(episodeId: String): DomainResult<Pair<PatientDto, EncounterDto>> =
        networkSimulator.execute(isEhrOperation = true, resourceId = "Encounter/$episodeId") {
            val ep = store.getEpisode(episodeId)
                ?: return@execute DomainResult.Failure(DomainError.EpisodeNotFound(episodeId))
            val patientDto = PatientDto(
                id = ep.patientId,
                fullName = ep.patientName,
                ageYears = ep.patientAge,
                mrnMasked = ep.mrnMasked
            )
            val encounterDto = EncounterDto(
                id = ep.id,
                patientRef = "Patient/${ep.patientId}",
                caseCode = ep.caseType.name,
                diagnosisDisplay = ep.diagnosisTitle,
                clinicalSummary = ep.diagnosisSummary,
                dischargeDateIso = ep.dischargeDateIso,
                hospitalName = ep.hospitalName,
                unitDisplay = ep.hospitalUnit,
                attendingDisplay = ep.attendingPhysician,
                primaryNurseDisplay = ep.primaryNurse,
                riskCode = ep.riskTier.name,
                riskRationale = ep.riskRationale
            )
            DomainResult.Success(patientDto to encounterDto)
        }

    override suspend fun updateConsent(episodeId: String, signed: Boolean): DomainResult<DischargeEpisode> =
        networkSimulator.execute {
            val updated = store.updateEpisode(episodeId) { it.copy(consentSigned = signed) }
            if (updated != null) {
                DomainResult.Success(updated)
            } else {
                DomainResult.Failure(DomainError.EpisodeNotFound(episodeId))
            }
        }
}
