package com.hospitaldischargecompanion.core.demo

sealed class DomainError(val code: String, val detail: String) {
    data class EpisodeNotFound(val episodeId: String) :
        DomainError("EPISODE_NOT_FOUND", "Discharge episode $episodeId was not found")

    data class NotAssignedToRole(val role: UserRole, val episodeId: String) :
        DomainError("NOT_ASSIGNED_TO_ROLE", "Role $role is not assigned to episode $episodeId")

    data class TaskAlreadyCompleted(val taskId: String) :
        DomainError("TASK_ALREADY_COMPLETED", "Care plan task $taskId is already completed")

    data class InvalidCarePlanTransition(val taskId: String, val fromState: String, val toState: String) :
        DomainError("INVALID_CARE_PLAN_TRANSITION", "Cannot move task $taskId from $fromState to $toState")

    data class MedicationDoseWindowClosed(val medicationId: String) :
        DomainError("DOSE_WINDOW_CLOSED", "Dose window closed for medication $medicationId")

    data class VitalOutOfRange(val metric: String, val value: String, val acceptedRange: String) :
        DomainError("VITAL_OUT_OF_RANGE", "$metric value $value is outside valid device range ($acceptedRange)")

    data class AlertAlreadyResolved(val alertId: String) :
        DomainError("ALERT_ALREADY_RESOLVED", "Clinical alert $alertId has already been resolved")

    data class UnauthorizedRoleAction(val role: UserRole, val action: String) :
        DomainError("UNAUTHORIZED_ROLE_ACTION", "Role $role is not permitted to perform $action")

    data class EhrSyncConflict(val resourceId: String, val reason: String) :
        DomainError("EHR_CONFLICT_409", "HTTP 409 conflict on $resourceId: $reason")

    data object EhrUnauthorized :
        DomainError("EHR_UNAUTHORIZED_401", "HTTP 401 unauthorized EHR token")

    data object NetworkTimeout :
        DomainError("NETWORK_TIMEOUT", "Simulated network timeout reached")

    data object NetworkUnavailable :
        DomainError("NETWORK_UNAVAILABLE", "Simulated offline or unreachable endpoint")

    data class ValidationFailed(val field: String, val reason: String) :
        DomainError("VALIDATION_FAILED", "$field: $reason")
}

sealed interface DomainResult<out T> {
    data class Success<T>(val data: T) : DomainResult<T>
    data class Failure(val error: DomainError) : DomainResult<Nothing>
}

inline fun <T, R> DomainResult<T>.map(transform: (T) -> R): DomainResult<R> = when (this) {
    is DomainResult.Success -> DomainResult.Success(transform(data))
    is DomainResult.Failure -> this
}

fun <T> DomainResult<T>.getOrNull(): T? = (this as? DomainResult.Success)?.data
