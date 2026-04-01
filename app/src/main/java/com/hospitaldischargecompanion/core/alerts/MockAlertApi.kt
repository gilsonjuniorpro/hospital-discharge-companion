package com.hospitaldischargecompanion.core.alerts

import com.hospitaldischargecompanion.core.demo.ClinicalClock
import com.hospitaldischargecompanion.core.demo.DemoNetworkSimulator
import com.hospitaldischargecompanion.core.demo.DomainError
import com.hospitaldischargecompanion.core.demo.DomainResult
import com.hospitaldischargecompanion.core.demo.IdGenerator
import com.hospitaldischargecompanion.core.demo.MockClinicalStore
import com.hospitaldischargecompanion.core.demo.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

interface AlertDataSource {
    fun observeAllAlerts(): Flow<List<ClinicalAlert>>
    suspend fun createAlert(
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

    suspend fun acknowledgeAlert(alertId: String, actorName: String, actorRole: UserRole): DomainResult<ClinicalAlert>
    suspend fun snoozeAlert(alertId: String, actorName: String, actorRole: UserRole, reasonCode: String): DomainResult<ClinicalAlert>
    suspend fun resolveAlert(alertId: String, actorName: String, actorRole: UserRole, resolutionNote: String): DomainResult<ClinicalAlert>
    suspend fun escalateAlert(alertId: String, actorName: String, actorRole: UserRole, reasonCode: String): DomainResult<ClinicalAlert>
}

@Singleton
class MockAlertApi @Inject constructor(
    store: MockClinicalStore,
    private val networkSimulator: DemoNetworkSimulator,
    private val clock: ClinicalClock,
    private val idGenerator: IdGenerator
) : AlertDataSource {

    private val _alerts = MutableStateFlow(AlertSeedFactory.createAlerts())

    init {
        store.registerResetCallback {
            _alerts.value = AlertSeedFactory.createAlerts()
        }
    }

    override fun observeAllAlerts(): Flow<List<ClinicalAlert>> = _alerts.asStateFlow()

    override suspend fun createAlert(
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
    ): DomainResult<ClinicalAlert> = networkSimulator.execute {
        val now = clock.now().toString()
        val alert = ClinicalAlert(
            id = idGenerator.nextId("alert"),
            episodeId = episodeId,
            type = type,
            severity = severity,
            status = if (escalationStage == EscalationStage.PHYSICIAN_REVIEW) AlertStatus.ESCALATED else AlertStatus.OPEN,
            escalationStage = escalationStage,
            title = title,
            summary = summary,
            patientGuidance = patientGuidance,
            createdAtIso = now,
            auditTrail = listOf(
                AlertAuditEntry(
                    timestampIso = now,
                    actorName = actorName,
                    actorRole = actorRole,
                    actionSummary = "Created $severity alert ($escalationStage)",
                    reasonCode = reasonCode
                )
            )
        )
        _alerts.update { listOf(alert) + it }
        DomainResult.Success(alert)
    }

    override suspend fun acknowledgeAlert(
        alertId: String,
        actorName: String,
        actorRole: UserRole
    ): DomainResult<ClinicalAlert> = mutateAlert(alertId) { current ->
        current.copy(
            status = AlertStatus.ACKNOWLEDGED,
            auditTrail = current.auditTrail + AlertAuditEntry(
                timestampIso = clock.now().toString(),
                actorName = actorName,
                actorRole = actorRole,
                actionSummary = "Acknowledged alert for clinical follow-up",
                reasonCode = "ACKNOWLEDGED"
            )
        )
    }

    override suspend fun snoozeAlert(
        alertId: String,
        actorName: String,
        actorRole: UserRole,
        reasonCode: String
    ): DomainResult<ClinicalAlert> = mutateAlert(alertId) { current ->
        current.copy(
            status = AlertStatus.SNOOZED,
            auditTrail = current.auditTrail + AlertAuditEntry(
                timestampIso = clock.now().toString(),
                actorName = actorName,
                actorRole = actorRole,
                actionSummary = "Snoozed alert pending scheduled recheck",
                reasonCode = reasonCode
            )
        )
    }

    override suspend fun resolveAlert(
        alertId: String,
        actorName: String,
        actorRole: UserRole,
        resolutionNote: String
    ): DomainResult<ClinicalAlert> = mutateAlert(alertId) { current ->
        current.copy(
            status = AlertStatus.RESOLVED,
            auditTrail = current.auditTrail + AlertAuditEntry(
                timestampIso = clock.now().toString(),
                actorName = actorName,
                actorRole = actorRole,
                actionSummary = "Resolved: ${resolutionNote.ifBlank { "Clinical review completed" }}",
                reasonCode = "RESOLVED"
            )
        )
    }

    override suspend fun escalateAlert(
        alertId: String,
        actorName: String,
        actorRole: UserRole,
        reasonCode: String
    ): DomainResult<ClinicalAlert> = mutateAlert(alertId) { current ->
        val nextStage = current.escalationStage.nextStage()
        val nextSeverity = when (nextStage) {
            EscalationStage.ADVISE_ER_COPY_ONLY -> AlertSeverity.CRITICAL
            EscalationStage.PHYSICIAN_REVIEW -> if (current.severity == AlertSeverity.CRITICAL) AlertSeverity.CRITICAL else AlertSeverity.URGENT
            else -> current.severity
        }
        val guidance = if (nextStage == EscalationStage.ADVISE_ER_COPY_ONLY) {
            "Advise ER / call local emergency services immediately (educational UI copy only; no real emergency dispatch)."
        } else {
            current.patientGuidance
        }
        current.copy(
            status = AlertStatus.ESCALATED,
            severity = nextSeverity,
            escalationStage = nextStage,
            patientGuidance = guidance,
            auditTrail = current.auditTrail + AlertAuditEntry(
                timestampIso = clock.now().toString(),
                actorName = actorName,
                actorRole = actorRole,
                actionSummary = "Escalated from ${current.escalationStage} to $nextStage",
                reasonCode = reasonCode
            )
        )
    }

    private suspend fun mutateAlert(
        alertId: String,
        transform: (ClinicalAlert) -> ClinicalAlert
    ): DomainResult<ClinicalAlert> = networkSimulator.execute {
        val current = _alerts.value.firstOrNull { it.id == alertId }
            ?: return@execute DomainResult.Failure(
                DomainError.ValidationFailed("alertId", "Alert $alertId not found")
            )
        if (current.status == AlertStatus.RESOLVED) {
            return@execute DomainResult.Failure(DomainError.AlertAlreadyResolved(alertId))
        }
        val updated = transform(current)
        _alerts.update { list -> list.map { if (it.id == alertId) updated else it } }
        DomainResult.Success(updated)
    }
}
