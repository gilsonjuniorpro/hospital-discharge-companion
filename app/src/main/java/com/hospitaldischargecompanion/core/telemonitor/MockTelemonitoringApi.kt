package com.hospitaldischargecompanion.core.telemonitor

import com.hospitaldischargecompanion.core.demo.ClinicalClock
import com.hospitaldischargecompanion.core.demo.DemoNetworkSimulator
import com.hospitaldischargecompanion.core.demo.DomainError
import com.hospitaldischargecompanion.core.demo.DomainResult
import com.hospitaldischargecompanion.core.demo.IdGenerator
import com.hospitaldischargecompanion.core.demo.MockClinicalStore
import com.hospitaldischargecompanion.core.discharge.EhrSyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

interface TelemonitoringDataSource {
    fun observeAllCheckIns(): Flow<List<VitalCheckIn>>
    fun observeBaselines(): Flow<List<VitalBaseline>>
    suspend fun submitCheckIn(request: NewCheckInRequest): DomainResult<VitalCheckIn>
}

@Singleton
class MockTelemonitoringApi @Inject constructor(
    private val store: MockClinicalStore,
    private val networkSimulator: DemoNetworkSimulator,
    private val clock: ClinicalClock,
    private val idGenerator: IdGenerator
) : TelemonitoringDataSource {

    private val _checkIns = MutableStateFlow(TelemonitorSeedFactory.createCheckIns())
    private val _baselines = MutableStateFlow(TelemonitorSeedFactory.createBaselines())

    init {
        store.registerResetCallback {
            _checkIns.value = TelemonitorSeedFactory.createCheckIns()
            _baselines.value = TelemonitorSeedFactory.createBaselines()
        }
    }

    override fun observeAllCheckIns(): Flow<List<VitalCheckIn>> = _checkIns.asStateFlow()
    override fun observeBaselines(): Flow<List<VitalBaseline>> = _baselines.asStateFlow()

    override suspend fun submitCheckIn(request: NewCheckInRequest): DomainResult<VitalCheckIn> =
        networkSimulator.execute {
            val existing = _checkIns.value.firstOrNull { it.idempotencyKey == request.idempotencyKey }
            if (existing != null) {
                return@execute DomainResult.Success(existing)
            }
            val episode = store.getEpisode(request.episodeId)
                ?: return@execute DomainResult.Failure(DomainError.EpisodeNotFound(request.episodeId))

            validateDeviceRanges(request)?.let { return@execute DomainResult.Failure(it) }

            val entry = VitalCheckIn(
                id = idGenerator.nextId("chk"),
                episodeId = request.episodeId,
                dayNumber = episode.dischargeDayNumber,
                timestampIso = clock.now().toString(),
                systolicMmHg = request.systolicMmHg,
                diastolicMmHg = request.diastolicMmHg,
                heartRateBpm = request.heartRateBpm,
                spo2Percent = request.spo2Percent,
                weightKg = request.weightKg,
                temperatureC = request.temperatureC,
                painScore = request.painScore.coerceIn(0, 10),
                dyspneaSeverity = request.dyspneaSeverity.coerceIn(0, 5),
                swellingSeverity = request.swellingSeverity.coerceIn(0, 5),
                freeTextNotes = request.freeTextNotes.trim(),
                recordedByLabel = request.recordedByLabel,
                idempotencyKey = request.idempotencyKey
            )
            _checkIns.update { it + entry }
            store.updateEpisode(request.episodeId) { it.copy(ehrSyncStatus = EhrSyncStatus.PENDING) }
            DomainResult.Success(entry)
        }

    private fun validateDeviceRanges(r: NewCheckInRequest): DomainError? = when {
        r.systolicMmHg !in 50..260 -> DomainError.VitalOutOfRange("Systolic BP", "${r.systolicMmHg} mmHg", "50–260 mmHg")
        r.diastolicMmHg !in 30..160 -> DomainError.VitalOutOfRange("Diastolic BP", "${r.diastolicMmHg} mmHg", "30–160 mmHg")
        r.systolicMmHg <= r.diastolicMmHg -> DomainError.ValidationFailed("Blood Pressure", "Systolic must exceed diastolic")
        r.heartRateBpm !in 30..220 -> DomainError.VitalOutOfRange("Heart Rate", "${r.heartRateBpm} bpm", "30–220 bpm")
        r.spo2Percent !in 60..100 -> DomainError.VitalOutOfRange("SpO2", "${r.spo2Percent}%", "60–100%")
        r.weightKg !in 25.0..250.0 -> DomainError.VitalOutOfRange("Weight", "${r.weightKg} kg", "25.0–250.0 kg")
        r.temperatureC !in 34.0..42.5 -> DomainError.VitalOutOfRange("Temperature", "${r.temperatureC} °C", "34.0–42.5 °C")
        else -> null
    }
}
