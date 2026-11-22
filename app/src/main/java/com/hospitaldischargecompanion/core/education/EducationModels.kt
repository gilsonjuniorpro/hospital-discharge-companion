package com.hospitaldischargecompanion.core.education

enum class ModuleProgress {
    NOT_STARTED,
    OPENED,
    COMPLETED
}

data class TeachBackQuiz(
    val question: String,
    val options: List<String>,
    val correctOptionIndex: Int,
    val explanation: String
)

data class EducationModuleItem(
    val id: String,
    val episodeId: String,
    val title: String,
    val estimatedMinutes: Int,
    val plainLanguageBody: String,
    val clinicalDetailBody: String,
    val isHighRiskTopic: Boolean,
    val teachBackQuiz: TeachBackQuiz? = null,
    val progress: ModuleProgress = ModuleProgress.NOT_STARTED,
    val quizPassed: Boolean = false
)
