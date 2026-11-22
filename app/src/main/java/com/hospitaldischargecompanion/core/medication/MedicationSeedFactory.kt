package com.hospitaldischargecompanion.core.medication

import com.hospitaldischargecompanion.core.demo.ClinicalSeedFactory

object MedicationSeedFactory {
    fun createMedications(): List<MedicationItem> = listOf(
        // Heart Failure
        MedicationItem(
            id = "med-hf-furosemide",
            episodeId = ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID,
            name = "Furosemide",
            strengthAndForm = "40 mg tablet",
            scheduleLabel = "08:00 & 14:00 (Twice daily)",
            instructions = "Take in morning and early afternoon to prevent nighttime urination.",
            foodInteraction = "Pair with potassium-rich foods unless restricted; monitor dizziness on standing.",
            allergenNote = "Sulfonamide moiety — monitor for rash if sulfa sensitive.",
            changeStatus = MedicationChangeStatus.CHANGED,
            changeNote = "Increased from 20 mg once daily to 40 mg twice daily at discharge.",
            refillStatus = RefillStatus.REFILL_SOON,
            daysSupplyRemaining = 5
        ),
        MedicationItem(
            id = "med-hf-entresto",
            episodeId = ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID,
            name = "Sacubitril / Valsartan",
            strengthAndForm = "49/51 mg tablet",
            scheduleLabel = "08:00 & 20:00",
            instructions = "Guideline-directed heart failure therapy; check blood pressure before dose.",
            foodInteraction = "May be taken with or without food; avoid salt substitutes high in potassium.",
            allergenNote = "Do not combine with ACE inhibitors (36-hour washout required).",
            changeStatus = MedicationChangeStatus.NEW,
            changeNote = "Started during hospitalization after stopping Lisinopril.",
            refillStatus = RefillStatus.IN_STOCK,
            daysSupplyRemaining = 26
        ),
        MedicationItem(
            id = "med-hf-carvedilol",
            episodeId = ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID,
            name = "Carvedilol",
            strengthAndForm = "12.5 mg tablet",
            scheduleLabel = "08:00 & 20:00",
            instructions = "Take with food to slow absorption and reduce lightheadedness.",
            foodInteraction = "Take with meals.",
            allergenNote = null,
            changeStatus = MedicationChangeStatus.CONTINUED,
            changeNote = "Continued at prior home dose.",
            refillStatus = RefillStatus.IN_STOCK,
            daysSupplyRemaining = 18
        ),
        MedicationItem(
            id = "med-hf-ibuprofen",
            episodeId = ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID,
            name = "Ibuprofen (OTC)",
            strengthAndForm = "400 mg tablet",
            scheduleLabel = "Discontinued",
            instructions = "STOPPED at discharge. NSAIDs worsen fluid retention and kidney function in heart failure.",
            foodInteraction = null,
            allergenNote = null,
            changeStatus = MedicationChangeStatus.STOPPED,
            changeNote = "Discontinued due to HFrEF fluid retention risk.",
            refillStatus = RefillStatus.IN_STOCK,
            daysSupplyRemaining = 0,
            doseWindowClosed = true
        ),
        // Post-Surgery
        MedicationItem(
            id = "med-ps-acetaminophen",
            episodeId = ClinicalSeedFactory.POST_SURGERY_EPISODE_ID,
            name = "Acetaminophen",
            strengthAndForm = "500 mg caplet",
            scheduleLabel = "Every 6 hours as needed (max 3 g/day)",
            instructions = "First-line post-operative pain control; avoid combining with OTC cold meds.",
            foodInteraction = "Avoid alcohol during recovery.",
            allergenNote = null,
            changeStatus = MedicationChangeStatus.NEW,
            changeNote = "Scheduled multimodal analgesia for days 1–4.",
            refillStatus = RefillStatus.IN_STOCK,
            daysSupplyRemaining = 14
        ),
        MedicationItem(
            id = "med-ps-ondansetron",
            episodeId = ClinicalSeedFactory.POST_SURGERY_EPISODE_ID,
            name = "Ondansetron",
            strengthAndForm = "4 mg ODT",
            scheduleLabel = "Every 8 hours PRN nausea",
            instructions = "Dissolve on tongue if post-surgical nausea develops.",
            foodInteraction = null,
            allergenNote = null,
            changeStatus = MedicationChangeStatus.NEW,
            changeNote = "PRN nausea support post-anesthesia.",
            refillStatus = RefillStatus.OUT_OF_STOCK,
            daysSupplyRemaining = 0
        ),
        // Pneumonia
        MedicationItem(
            id = "med-pn-amoxclav",
            episodeId = ClinicalSeedFactory.PNEUMONIA_EPISODE_ID,
            name = "Amoxicillin / Clavulanate",
            strengthAndForm = "875/125 mg tablet",
            scheduleLabel = "08:00 & 20:00 (Complete full 5 days)",
            instructions = "Finish entire antibiotic course even if fever and cough improve.",
            foodInteraction = "Take at the start of a meal to minimize stomach upset.",
            allergenNote = "Beta-lactam antibiotic — no documented penicillin allergy.",
            changeStatus = MedicationChangeStatus.NEW,
            changeNote = "Oral step-down from inpatient IV therapy.",
            refillStatus = RefillStatus.IN_STOCK,
            daysSupplyRemaining = 4
        )
    )

    fun createInteractionWarnings(): List<MedicationInteractionWarning> = listOf(
        MedicationInteractionWarning(
            id = "warn-hf-1",
            episodeId = ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID,
            severityLabel = "High",
            title = "Avoid duplicate ACE inhibitor / NSAID therapy",
            detail = "Sacubitril/Valsartan is active. Verify prior Lisinopril and OTC Ibuprofen remain stopped to prevent angioedema and fluid retention."
        ),
        MedicationInteractionWarning(
            id = "warn-ps-1",
            episodeId = ClinicalSeedFactory.POST_SURGERY_EPISODE_ID,
            severityLabel = "Moderate",
            title = "Acetaminophen cumulative daily cap",
            detail = "Ensure combined prescription and OTC products stay below 3,000 mg per 24 hours."
        )
    )
}
