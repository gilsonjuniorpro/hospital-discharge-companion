package com.hospitaldischargecompanion.feature.education

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.hospitaldischargecompanion.core.demo.DemoSessionRepository
import com.hospitaldischargecompanion.core.demo.DomainResult
import com.hospitaldischargecompanion.core.demo.UserRole
import com.hospitaldischargecompanion.core.education.EducationModuleItem
import com.hospitaldischargecompanion.core.education.EducationRepository
import com.hospitaldischargecompanion.core.navigation.HdcRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EducationUiState(
    val activeRole: UserRole = UserRole.PATIENT,
    val showClinicalDetail: Boolean = false,
    val modules: List<EducationModuleItem> = emptyList(),
    val quizFeedback: String? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class EducationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    sessionRepository: DemoSessionRepository,
    private val educationRepository: EducationRepository
) : ViewModel() {

    private val route = savedStateHandle.toRoute<HdcRoute.Education>()
    private val _showClinical = MutableStateFlow(false)
    private val _quizFeedback = MutableStateFlow<String?>(null)

    val uiState: StateFlow<EducationUiState> = combine(
        sessionRepository.sessionState,
        educationRepository.observeModules(route.episodeId),
        _showClinical,
        _quizFeedback
    ) { session, modules, showClinical, feedback ->
        val role = session.activeRole ?: UserRole.PATIENT
        EducationUiState(
            activeRole = role,
            showClinicalDetail = showClinical || role.isClinician,
            modules = modules,
            quizFeedback = feedback,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EducationUiState()
    )

    fun toggleClinicalDetail() {
        _showClinical.value = !_showClinical.value
    }

    fun markCompleted(moduleId: String) {
        viewModelScope.launch {
            educationRepository.markCompleted(moduleId)
        }
    }

    fun answerQuiz(moduleId: String, optionIndex: Int) {
        viewModelScope.launch {
            val res = educationRepository.submitQuizAnswer(moduleId, optionIndex)
            if (res is DomainResult.Success) {
                val (module, correct) = res.data
                _quizFeedback.value = if (correct) {
                    "Teach-back verified: ${module.teachBackQuiz?.explanation.orEmpty()}"
                } else {
                    "Not quite — review the guidance and try another answer."
                }
            }
        }
    }
}
