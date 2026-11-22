package com.hospitaldischargecompanion.core.education

import com.hospitaldischargecompanion.core.demo.ClinicalSeedFactory
import com.hospitaldischargecompanion.core.demo.DemoNetworkSimulator
import com.hospitaldischargecompanion.core.demo.DomainError
import com.hospitaldischargecompanion.core.demo.DomainResult
import com.hospitaldischargecompanion.core.demo.MockClinicalStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

interface EducationDataSource {
    fun observeAllModules(): Flow<List<EducationModuleItem>>
    suspend fun markOpened(moduleId: String): DomainResult<EducationModuleItem>
    suspend fun markCompleted(moduleId: String): DomainResult<EducationModuleItem>
    suspend fun submitQuizAnswer(moduleId: String, selectedOptionIndex: Int): DomainResult<Pair<EducationModuleItem, Boolean>>
}

@Singleton
class MockEducationApi @Inject constructor(
    store: MockClinicalStore,
    private val networkSimulator: DemoNetworkSimulator
) : EducationDataSource {

    private fun seedModules(): List<EducationModuleItem> = listOf(
        EducationModuleItem(
            id = "edu-hf-weight",
            episodeId = ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID,
            title = "Daily Dry Weight & Fluid Zone Action Plan",
            estimatedMinutes = 4,
            plainLanguageBody = "Weigh yourself every morning after using the bathroom and before eating breakfast, wearing similar light clothes. Call your care team if your weight jumps 2 kg (4.4 lb) in 2 days or your ankles swell.",
            clinicalDetailBody = "HFrEF decompensation surveillance: euvolemic discharge dry weight 76.5 kg. Rapid >2 kg / 48h weight gain signals subclinical congestion warranting temporary loop diuretic titration and BMP renal/electrolyte check.",
            isHighRiskTopic = true,
            teachBackQuiz = TeachBackQuiz(
                question = "When should you measure your daily weight and what change should trigger a care-team alert?",
                options = listOf(
                    "Morning before breakfast; gain of 2 kg or more in 2 days",
                    "Right after dinner; any 0.2 kg fluctuation",
                    "Once a week at clinic visits only"
                ),
                correctOptionIndex = 0,
                explanation = "Consistent morning weights detect early fluid retention before severe shortness of breath develops."
            ),
            progress = ModuleProgress.OPENED
        ),
        EducationModuleItem(
            id = "edu-hf-sodium",
            episodeId = ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID,
            title = "Low-Sodium Eating (<2,000 mg/day) & Hidden Salt",
            estimatedMinutes = 5,
            plainLanguageBody = "Skip canned soups, cured meats, and restaurant sauces. Flavor food with lemon, garlic, and herbs instead of table salt or potassium salt substitutes.",
            clinicalDetailBody = "Dietary sodium restriction (<2 g/day) and 1.5 L fluid target reduce preload and diuretic resistance. Avoid KCl salt substitutes while on ARNI (Sacubitril/Valsartan) due to hyperkalemia risk.",
            isHighRiskTopic = false
        ),
        EducationModuleItem(
            id = "edu-ps-wound",
            episodeId = ClinicalSeedFactory.POST_SURGERY_EPISODE_ID,
            title = "Laparoscopic Incision Care & Infection Warning Signs",
            estimatedMinutes = 3,
            plainLanguageBody = "Keep adhesive strips dry and let them fall off naturally. Pat incisions gently after showering — do not soak in baths or pools until cleared by your surgeon.",
            clinicalDetailBody = "Surgical site infection (SSI) monitoring window days 2–14. Escalate for erythema >1 cm, purulent exudate, wound dehiscence, or fever >=38.0 °C.",
            isHighRiskTopic = true,
            teachBackQuiz = TeachBackQuiz(
                question = "Which sign around a healing laparoscopic incision requires prompt care-team review?",
                options = listOf(
                    "Spreading redness, warmth, pus, or fever above 38.0 °C",
                    "Mild itching as Steri-Strips peel at day 7",
                    "Slight bruising fading over a few days"
                ),
                correctOptionIndex = 0,
                explanation = "Spreading redness, drainage, or fever may indicate early post-operative infection."
            )
        ),
        EducationModuleItem(
            id = "edu-pn-breathing",
            episodeId = ClinicalSeedFactory.PNEUMONIA_EPISODE_ID,
            title = "Completing Antibiotics & Lung Recovery Breathing",
            estimatedMinutes = 4,
            plainLanguageBody = "Take every dose of your oral antibiotic even after you feel better. Use your incentive spirometer 10 times every 2 hours while awake and drink water unless restricted.",
            clinicalDetailBody = "Post-CAP step-down protocol: complete full 5-day beta-lactam course, monitor resting SpO2 >=94% on room air, and reinforce airway clearance to prevent atelectasis.",
            isHighRiskTopic = true,
            teachBackQuiz = TeachBackQuiz(
                question = "What should you do if your fever resolves after 2 days of oral antibiotics?",
                options = listOf(
                    "Finish the entire prescribed antibiotic course",
                    "Stop early to save leftover pills",
                    "Take half doses every other day"
                ),
                correctOptionIndex = 0,
                explanation = "Completing the prescribed course prevents bacterial relapse and readmission."
            )
        )
    )

    private val _modules = MutableStateFlow(seedModules())

    init {
        store.registerResetCallback { _modules.value = seedModules() }
    }

    override fun observeAllModules(): Flow<List<EducationModuleItem>> = _modules.asStateFlow()

    override suspend fun markOpened(moduleId: String): DomainResult<EducationModuleItem> =
        updateModule(moduleId) {
            if (it.progress == ModuleProgress.NOT_STARTED) it.copy(progress = ModuleProgress.OPENED) else it
        }

    override suspend fun markCompleted(moduleId: String): DomainResult<EducationModuleItem> =
        updateModule(moduleId) { it.copy(progress = ModuleProgress.COMPLETED) }

    override suspend fun submitQuizAnswer(
        moduleId: String,
        selectedOptionIndex: Int
    ): DomainResult<Pair<EducationModuleItem, Boolean>> = networkSimulator.execute {
        val current = _modules.value.firstOrNull { it.id == moduleId }
            ?: return@execute DomainResult.Failure(DomainError.ValidationFailed("moduleId", "Education module not found"))
        val quiz = current.teachBackQuiz
            ?: return@execute DomainResult.Failure(DomainError.ValidationFailed("quiz", "Module has no quiz"))
        val isCorrect = selectedOptionIndex == quiz.correctOptionIndex
        val updated = current.copy(
            quizPassed = current.quizPassed || isCorrect,
            progress = if (isCorrect) ModuleProgress.COMPLETED else ModuleProgress.OPENED
        )
        _modules.update { list -> list.map { if (it.id == moduleId) updated else it } }
        DomainResult.Success(updated to isCorrect)
    }

    private suspend fun updateModule(
        moduleId: String,
        transform: (EducationModuleItem) -> EducationModuleItem
    ): DomainResult<EducationModuleItem> = networkSimulator.execute {
        var updated: EducationModuleItem? = null
        _modules.update { list ->
            list.map {
                if (it.id == moduleId) transform(it).also { m -> updated = m } else it
            }
        }
        updated?.let { DomainResult.Success(it) }
            ?: DomainResult.Failure(DomainError.ValidationFailed("moduleId", "Education module not found"))
    }
}
