package com.hospitaldischargecompanion.core.telemonitor

import com.hospitaldischargecompanion.core.demo.ClinicalSeedFactory

object TelemonitorSeedFactory {
    fun createBaselines(): List<VitalBaseline> = listOf(
        VitalBaseline(
            episodeId = ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID,
            dryWeightKg = 76.5,
            targetSystolicMaxMmHg = 140,
            targetSystolicMinMmHg = 95,
            targetSpo2MinPercent = 94,
            targetTempMaxC = 37.8
        ),
        VitalBaseline(
            episodeId = ClinicalSeedFactory.POST_SURGERY_EPISODE_ID,
            dryWeightKg = 64.0,
            targetSystolicMaxMmHg = 140,
            targetSystolicMinMmHg = 95,
            targetSpo2MinPercent = 95,
            targetTempMaxC = 37.8
        ),
        VitalBaseline(
            episodeId = ClinicalSeedFactory.PNEUMONIA_EPISODE_ID,
            dryWeightKg = 71.2,
            targetSystolicMaxMmHg = 145,
            targetSystolicMinMmHg = 95,
            targetSpo2MinPercent = 94,
            targetTempMaxC = 37.8
        )
    )

    fun createCheckIns(): List<VitalCheckIn> = listOf(
        VitalCheckIn(
            id = "chk-hf-d0",
            episodeId = ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID,
            dayNumber = 0,
            timestampIso = "2026-06-12T14:00:00Z",
            systolicMmHg = 126,
            diastolicMmHg = 76,
            heartRateBpm = 74,
            spo2Percent = 96,
            weightKg = 76.5,
            temperatureC = 36.6,
            painScore = 1,
            dyspneaSeverity = 1,
            swellingSeverity = 1,
            freeTextNotes = "Discharge baseline vitals at hospital step-down unit.",
            recordedByLabel = "RN Clara Mendes",
            idempotencyKey = "seed-hf-d0"
        ),
        VitalCheckIn(
            id = "chk-hf-d1",
            episodeId = ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID,
            dayNumber = 1,
            timestampIso = "2026-06-13T08:15:00Z",
            systolicMmHg = 132,
            diastolicMmHg = 80,
            heartRateBpm = 78,
            spo2Percent = 96,
            weightKg = 76.9,
            temperatureC = 36.7,
            painScore = 1,
            dyspneaSeverity = 1,
            swellingSeverity = 1,
            freeTextNotes = "Slept flat with two pillows; morning furosemide taken.",
            recordedByLabel = "João Silva",
            idempotencyKey = "seed-hf-d1"
        ),
        VitalCheckIn(
            id = "chk-hf-d2",
            episodeId = ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID,
            dayNumber = 2,
            timestampIso = "2026-06-14T08:20:00Z",
            systolicMmHg = 142,
            diastolicMmHg = 86,
            heartRateBpm = 82,
            spo2Percent = 95,
            weightKg = 77.6,
            temperatureC = 36.6,
            painScore = 1,
            dyspneaSeverity = 2,
            swellingSeverity = 2,
            freeTextNotes = "Mild ankle puffiness in evening after salty soup.",
            recordedByLabel = "Maria Silva (Caregiver) on behalf of João",
            idempotencyKey = "seed-hf-d2"
        ),
        VitalCheckIn(
            id = "chk-ps-d1",
            episodeId = ClinicalSeedFactory.POST_SURGERY_EPISODE_ID,
            dayNumber = 1,
            timestampIso = "2026-06-14T09:10:00Z",
            systolicMmHg = 122,
            diastolicMmHg = 74,
            heartRateBpm = 76,
            spo2Percent = 98,
            weightKg = 64.1,
            temperatureC = 37.1,
            painScore = 4,
            dyspneaSeverity = 0,
            swellingSeverity = 1,
            freeTextNotes = "Incisions clean and dry; tolerating soft low-fat breakfast.",
            recordedByLabel = "Maria Oliveira",
            idempotencyKey = "seed-ps-d1"
        ),
        VitalCheckIn(
            id = "chk-pn-d3",
            episodeId = ClinicalSeedFactory.PNEUMONIA_EPISODE_ID,
            dayNumber = 3,
            timestampIso = "2026-06-14T08:30:00Z",
            systolicMmHg = 128,
            diastolicMmHg = 78,
            heartRateBpm = 80,
            spo2Percent = 95,
            weightKg = 71.1,
            temperatureC = 37.0,
            painScore = 1,
            dyspneaSeverity = 1,
            swellingSeverity = 0,
            freeTextNotes = "Cough improving; afebrile on oral amoxicillin-clavulanate.",
            recordedByLabel = "Arthur Bennett",
            idempotencyKey = "seed-pn-d3"
        )
    )
}
