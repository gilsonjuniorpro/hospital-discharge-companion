package com.hospitaldischargecompanion.core.discharge

import com.hospitaldischargecompanion.core.demo.ClinicalSeedFactory
import com.hospitaldischargecompanion.core.demo.UserRole

object CarePlanSeedFactory {
    fun createTasks(): List<CarePlanTask> = listOf(
        // Heart failure episode
        CarePlanTask(
            id = "task-hf-1",
            episodeId = ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID,
            dayNumber = 0,
            scheduledTimeLabel = "Day 0 · 18:00",
            title = "Review discharge medication reconciliation",
            instructions = "Verify increased Furosemide 40 mg dose and stop prior home NSAIDs.",
            type = CarePlanTaskType.EDUCATION,
            status = CarePlanTaskStatus.COMPLETED,
            completedByLabel = "João Silva"
        ),
        CarePlanTask(
            id = "task-hf-2",
            episodeId = ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID,
            dayNumber = 3,
            scheduledTimeLabel = "Day 3 · 08:00",
            title = "Morning dry weight & blood pressure check",
            instructions = "Weigh before breakfast after voiding; measure seated BP and pulse.",
            type = CarePlanTaskType.VITAL_CHECK,
            status = CarePlanTaskStatus.DUE
        ),
        CarePlanTask(
            id = "task-hf-3",
            episodeId = ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID,
            dayNumber = 3,
            scheduledTimeLabel = "Day 3 · 08:30",
            title = "Morning loop diuretic & GDMT doses",
            instructions = "Take Furosemide 40 mg, Sacubitril/Valsartan, and Carvedilol.",
            type = CarePlanTaskType.MEDICATION,
            status = CarePlanTaskStatus.DUE
        ),
        CarePlanTask(
            id = "task-hf-4",
            episodeId = ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID,
            dayNumber = 3,
            scheduledTimeLabel = "Day 3 · 12:30",
            title = "Low-sodium (<2,000 mg) & 1.5 L fluid log",
            instructions = "Track sodium intake and keep total daily fluids under 1.5 liters.",
            type = CarePlanTaskType.DIET,
            status = CarePlanTaskStatus.UPCOMING
        ),
        CarePlanTask(
            id = "task-hf-5",
            episodeId = ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID,
            dayNumber = 3,
            scheduledTimeLabel = "Day 3 · 16:00",
            title = "10-minute flat indoor walk",
            instructions = "Stop and rest immediately if short of breath or dizzy.",
            type = CarePlanTaskType.ACTIVITY,
            status = CarePlanTaskStatus.OVERDUE
        ),
        CarePlanTask(
            id = "task-hf-6",
            episodeId = ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID,
            dayNumber = 5,
            scheduledTimeLabel = "Day 5 · 09:00",
            title = "Basic metabolic panel (BMP) lab draw",
            instructions = "Check serum potassium and creatinine after diuretic uptitration.",
            type = CarePlanTaskType.LAB,
            status = CarePlanTaskStatus.UPCOMING
        ),
        CarePlanTask(
            id = "task-hf-7",
            episodeId = ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID,
            dayNumber = 7,
            scheduledTimeLabel = "Day 7 · 10:30",
            title = "Cardiology transition clinic visit",
            instructions = "Bring weight log and home blood pressure cuff for calibration.",
            type = CarePlanTaskType.APPOINTMENT,
            status = CarePlanTaskStatus.UPCOMING
        ),

        // Post-surgery episode
        CarePlanTask(
            id = "task-ps-1",
            episodeId = ClinicalSeedFactory.POST_SURGERY_EPISODE_ID,
            dayNumber = 2,
            scheduledTimeLabel = "Day 2 · 09:00",
            title = "Inspect laparoscopic incision dressings",
            instructions = "Keep Steri-Strips clean and dry; note any warmth, redness, or drainage.",
            type = CarePlanTaskType.WOUND_CARE,
            status = CarePlanTaskStatus.DUE
        ),
        CarePlanTask(
            id = "task-ps-2",
            episodeId = ClinicalSeedFactory.POST_SURGERY_EPISODE_ID,
            dayNumber = 2,
            scheduledTimeLabel = "Day 2 · 11:00",
            title = "Short hallway ambulation (3x daily)",
            instructions = "Gentle walking prevents post-operative blood clots; avoid lifting >5 kg.",
            type = CarePlanTaskType.ACTIVITY,
            status = CarePlanTaskStatus.UPCOMING
        ),
        CarePlanTask(
            id = "task-ps-3",
            episodeId = ClinicalSeedFactory.POST_SURGERY_EPISODE_ID,
            dayNumber = 10,
            scheduledTimeLabel = "Day 10 · 14:00",
            title = "Surgical outpatient incision check",
            instructions = "Clinic follow-up with Dr. Marcus Sterling.",
            type = CarePlanTaskType.APPOINTMENT,
            status = CarePlanTaskStatus.UPCOMING
        ),

        // Pneumonia episode
        CarePlanTask(
            id = "task-pn-1",
            episodeId = ClinicalSeedFactory.PNEUMONIA_EPISODE_ID,
            dayNumber = 4,
            scheduledTimeLabel = "Day 4 · 08:00",
            title = "Pulse oximetry & temperature check",
            instructions = "Measure resting SpO2 and oral temperature before morning antibiotic.",
            type = CarePlanTaskType.VITAL_CHECK,
            status = CarePlanTaskStatus.DUE
        ),
        CarePlanTask(
            id = "task-pn-2",
            episodeId = ClinicalSeedFactory.PNEUMONIA_EPISODE_ID,
            dayNumber = 4,
            scheduledTimeLabel = "Day 4 · 14:00",
            title = "Incentive spirometry breathing exercises",
            instructions = "10 slow deep breaths every 2 hours while awake to aid lung recovery.",
            type = CarePlanTaskType.EDUCATION,
            status = CarePlanTaskStatus.UPCOMING
        )
    )

    fun createRevisions(): List<CarePlanRevision> = listOf(
        CarePlanRevision(
            revisionNumber = 1,
            episodeId = ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID,
            authorName = "Dr. Elena Vance, MD",
            authorRole = UserRole.PHYSICIAN,
            timestampIso = "2026-06-12T15:30:00Z",
            changeSummary = "Initial post-discharge HFrEF care plan with daily weight/BP monitoring and day-5 BMP.",
            signedOff = true
        ),
        CarePlanRevision(
            revisionNumber = 1,
            episodeId = ClinicalSeedFactory.POST_SURGERY_EPISODE_ID,
            authorName = "Dr. Marcus Sterling, MD",
            authorRole = UserRole.PHYSICIAN,
            timestampIso = "2026-06-13T11:15:00Z",
            changeSummary = "Initial post-cholecystectomy wound care, analgesic taper, and ambulation schedule.",
            signedOff = true
        ),
        CarePlanRevision(
            revisionNumber = 1,
            episodeId = ClinicalSeedFactory.PNEUMONIA_EPISODE_ID,
            authorName = "Dr. Elena Vance, MD",
            authorRole = UserRole.PHYSICIAN,
            timestampIso = "2026-06-11T16:00:00Z",
            changeSummary = "Initial pneumonia home recovery plan with oral antibiotics and daily SpO2 check.",
            signedOff = true
        )
    )
}
