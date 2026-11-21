package com.hospitaldischargecompanion.core.demo

import com.hospitaldischargecompanion.core.alerts.EvaluateAlertRulesUseCase
import com.hospitaldischargecompanion.core.discharge.CarePlanRepository
import com.hospitaldischargecompanion.core.discharge.CarePlanTaskType
import com.hospitaldischargecompanion.core.ehr.EhrSyncRepository
import com.hospitaldischargecompanion.core.medication.DoseActionType
import com.hospitaldischargecompanion.core.medication.MedicationRepository
import com.hospitaldischargecompanion.core.telemonitor.NewCheckInRequest
import com.hospitaldischargecompanion.core.telemonitor.TelemonitoringRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

enum class DemoScenarioPreset {
    FULL_ADHERENCE,
    MISSED_DOSE,
    CRITICAL_BP,
    EHR_CONFLICT_409,
    CAREGIVER_CONFIRMED_MEDS,
    PHYSICIAN_AMENDED_PLAN
}

@Singleton
class DemoScenarioRunner @Inject constructor(
    private val store: MockClinicalStore,
    private val networkSimulator: DemoNetworkSimulator,
    private val carePlanRepository: CarePlanRepository,
    private val medicationRepository: MedicationRepository,
    private val telemonitoringRepository: TelemonitoringRepository,
    private val evaluateAlertRules: EvaluateAlertRulesUseCase,
    private val ehrSyncRepository: EhrSyncRepository,
    private val idGenerator: IdGenerator
) {
    suspend fun apply(preset: DemoScenarioPreset): String {
        val epId = ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID
        val prevLatency = networkSimulator.config.value.latencyMs
        networkSimulator.setLatencyMs(0L)
        networkSimulator.setFailureMode(SimulatedFailureMode.NONE)
        store.resetAll()

        val summary = when (preset) {
            DemoScenarioPreset.FULL_ADHERENCE -> {
                listOf("med-hf-furosemide", "med-hf-entresto", "med-hf-carvedilol").forEach { id ->
                    medicationRepository.recordDose(id, DoseActionType.TAKEN, "João Silva", idempotencyKey = idGenerator.nextId("sc"))
                }
                carePlanRepository.completeTask("task-hf-2", "João Silva")
                carePlanRepository.completeTask("task-hf-3", "João Silva")
                "Loaded Full Adherence scenario (100% active doses taken)."
            }

            DemoScenarioPreset.MISSED_DOSE -> {
                medicationRepository.recordDose(
                    medicationId = "med-hf-furosemide",
                    action = DoseActionType.SKIPPED,
                    attributionLabel = "João Silva",
                    reasonOrNote = "Dizziness on standing",
                    idempotencyKey = idGenerator.nextId("sc")
                )
                evaluateAlertRules.evaluateMissedMedication(
                    episodeId = epId,
                    medicationName = "Furosemide 40 mg tablet",
                    attributionLabel = "João Silva",
                    reason = "Dizziness on standing"
                )
                "Loaded Missed Diuretic Dose scenario with nurse triage alert."
            }

            DemoScenarioPreset.CRITICAL_BP -> {
                val res = telemonitoringRepository.submitCheckIn(
                    NewCheckInRequest(
                        episodeId = epId,
                        systolicMmHg = 184,
                        diastolicMmHg = 110,
                        heartRateBpm = 102,
                        spo2Percent = 91,
                        weightKg = 78.9,
                        temperatureC = 36.8,
                        painScore = 2,
                        dyspneaSeverity = 4,
                        swellingSeverity = 3,
                        freeTextNotes = "Severe morning BP spike and shortness of breath.",
                        recordedByLabel = "João Silva",
                        idempotencyKey = idGenerator.nextId("sc")
                    )
                )
                if (res is DomainResult.Success) {
                    val base = telemonitoringRepository.observeBaseline(epId).first()
                    evaluateAlertRules.evaluateCheckIn(res.data, base)
                }
                "Loaded Critical High BP (184/110 mmHg) scenario routed to Physician queue."
            }

            DemoScenarioPreset.EHR_CONFLICT_409 -> {
                networkSimulator.setFailureMode(SimulatedFailureMode.EHR_CONFLICT_409)
                ehrSyncRepository.pushObservationsAndAdherence(epId)
                "Loaded HTTP 409 EHR Sync Conflict scenario ready for reconciliation."
            }

            DemoScenarioPreset.CAREGIVER_CONFIRMED_MEDS -> {
                medicationRepository.recordDose(
                    medicationId = "med-hf-furosemide",
                    action = DoseActionType.TAKEN,
                    attributionLabel = "Maria Silva (Spouse) on behalf of João Silva",
                    idempotencyKey = idGenerator.nextId("sc")
                )
                medicationRepository.attachMockPillBoxPhoto("med-hf-furosemide", "Weekly pill box verified by Maria Silva")
                "Loaded Caregiver Dual-Attribution & Pill-Box Photo scenario."
            }

            DemoScenarioPreset.PHYSICIAN_AMENDED_PLAN -> {
                carePlanRepository.amendCarePlan(
                    episodeId = epId,
                    actorRole = UserRole.PHYSICIAN,
                    authorName = "Dr. Elena Vance, MD",
                    changeSummary = "Added evening dry-weight check and stricter dyspnea watch",
                    newTaskTitle = "Evening dry-weight & BP recheck",
                    newTaskInstructions = "Weigh before dinner and log seated BP.",
                    newTaskType = CarePlanTaskType.VITAL_CHECK,
                    newRedFlag = "Sudden overnight weight jump > 1.5 kg"
                )
                "Loaded Physician Amended Care Plan v2 scenario."
            }
        }

        networkSimulator.setLatencyMs(prevLatency)
        return summary
    }
}
