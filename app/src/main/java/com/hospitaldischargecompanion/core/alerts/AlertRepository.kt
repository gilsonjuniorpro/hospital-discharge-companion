package com.hospitaldischargecompanion.core.alerts

import com.hospitaldischargecompanion.core.demo.DomainResult
import com.hospitaldischargecompanion.core.demo.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface AlertRepository {
    fun observeAllAlerts(): Flow<List<ClinicalAlert>>
    fun observeEpisodeAlerts(episodeId: String): Flow<List<ClinicalAlert>>
    fun observeNurseTriageQueue(): Flow<List<ClinicalAlert>>
    fun observePhysicianEscalatedQueue(): Flow<List<ClinicalAlert>>
    suspend fun raiseAlert(
        episodeId: String,
        type: AlertType,
        severity: AlertSeverity,
        escalationStage: EscalationStage,
        title: String,
        summary: String,
        patientGuidance: String,
        actorName: String,
        actorRole: UserRole,
        reasonCode: String
    ): DomainResult<ClinicalAlert>

    suspend fun acknowledge(alertId: String, actorName: String, actorRole: UserRole): DomainResult<ClinicalAlert>
    suspend fun snooze(alertId: String, actorName: String, actorRole: UserRole, reasonCode: String): DomainResult<ClinicalAlert>
    suspend fun resolve(alertId: String, actorName: String, actorRole: UserRole, resolutionNote: String): DomainResult<ClinicalAlert>
    suspend fun escalate(alertId: String, actorName: String, actorRole: UserRole, reasonCode: String): DomainResult<ClinicalAlert>
}

@Singleton
class DefaultAlertRepository @Inject constructor(
    private val dataSource: AlertDataSource
) : AlertRepository {

    override fun observeAllAlerts(): Flow<List<ClinicalAlert>> = dataSource.observeAllAlerts()

    override fun observeEpisodeAlerts(episodeId: String): Flow<List<ClinicalAlert>> =
        dataSource.observeAllAlerts().map { list -> list.filter { it.episodeId == episodeId } }

    override fun observeNurseTriageQueue(): Flow<List<ClinicalAlert>> =
        dataSource.observeAllAlerts().map { list ->
            list.filter { it.isActive }
                .sortedByDescending { it.severity.ordinal }
        }

    override fun observePhysicianEscalatedQueue(): Flow<List<ClinicalAlert>> =
        dataSource.observeAllAlerts().map { list ->
            list.filter {
                it.isActive && (
                    it.severity == AlertSeverity.CRITICAL ||
                        it.severity == AlertSeverity.URGENT ||
                        it.escalationStage == EscalationStage.PHYSICIAN_REVIEW ||
                        it.escalationStage == EscalationStage.ADVISE_ER_COPY_ONLY
                    )
            }.sortedByDescending { it.severity.ordinal }
        }

    override suspend fun raiseAlert(
        episodeId: String,
        type: AlertType,
        severity: AlertSeverity,
        escalationStage: EscalationStage,
        title: String,
        summary: String,
        patientGuidance: String,
        actorName: String,
        actorRole: UserRole,
        reasonCode: String
    ): DomainResult<ClinicalAlert> = dataSource.createAlert(
        episodeId = episodeId,
        type = type,
        severity = severity,
        escalationStage = escalationStage,
        title = title,
        summary = summary,
        patientGuidance = patientGuidance,
        actorName = actorName,
        actorRole = actorRole,
        reasonCode = reasonCode
    )

    override suspend fun acknowledge(alertId: String, actorName: String, actorRole: UserRole): DomainResult<ClinicalAlert> =
        dataSource.acknowledgeAlert(alertId, actorName, actorRole)

    override suspend fun snooze(alertId: String, actorName: String, actorRole: UserRole, reasonCode: String): DomainResult<ClinicalAlert> =
        dataSource.snoozeAlert(alertId, actorName, actorRole, reasonCode)

    override suspend fun resolve(alertId: String, actorName: String, actorRole: UserRole, resolutionNote: String): DomainResult<ClinicalAlert> =
        dataSource.resolveAlert(alertId, actorName, actorRole, resolutionNote)

    override suspend fun escalate(alertId: String, actorName: String, actorRole: UserRole, reasonCode: String): DomainResult<ClinicalAlert> =
        dataSource.escalateAlert(alertId, actorName, actorRole, reasonCode)
}
