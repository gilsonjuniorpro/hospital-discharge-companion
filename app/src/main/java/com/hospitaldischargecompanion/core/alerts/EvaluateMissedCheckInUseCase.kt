package com.hospitaldischargecompanion.core.alerts

import com.hospitaldischargecompanion.core.demo.ClinicalClock
import com.hospitaldischargecompanion.core.demo.DomainResult
import com.hospitaldischargecompanion.core.demo.UserRole
import com.hospitaldischargecompanion.core.discharge.DischargeEpisode
import com.hospitaldischargecompanion.core.discharge.RiskTier
import com.hospitaldischargecompanion.core.telemonitor.VitalCheckIn
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Evaluates whether an episode has exceeded its risk-tier check-in window and raises
 * an automated telemonitoring alert when a check-in is overdue.
 */
@Singleton
class EvaluateMissedCheckInUseCase @Inject constructor(
    private val alertRepository: AlertRepository,
    private val clock: ClinicalClock
) {
    fun maxAllowedHours(riskTier: RiskTier): Long = when (riskTier) {
        RiskTier.HIGH -> 24L
        RiskTier.MEDIUM -> 36L
        RiskTier.LOW -> 48L
    }

    suspend fun evaluate(
        episode: DischargeEpisode,
        checkIns: List<VitalCheckIn>
    ): DomainResult<ClinicalAlert?> {
        val latest = checkIns.maxByOrNull { runCatching { Instant.parse(it.timestampIso) }.getOrDefault(Instant.EPOCH) }
        val lastInstant = latest?.let { runCatching { Instant.parse(it.timestampIso) }.getOrNull() }
            ?: return raiseMissedAlert(episode, hoursOverdue = maxAllowedHours(episode.riskTier))

        val elapsedHours = Duration.between(lastInstant, clock.now()).toHours()
        val threshold = maxAllowedHours(episode.riskTier)
        return if (elapsedHours >= threshold) {
            raiseMissedAlert(episode, elapsedHours)
        } else {
            DomainResult.Success(null)
        }
    }

    private suspend fun raiseMissedAlert(
        episode: DischargeEpisode,
        hoursOverdue: Long
    ): DomainResult<ClinicalAlert?> {
        val severity = if (episode.riskTier == RiskTier.HIGH) AlertSeverity.URGENT else AlertSeverity.WARNING
        return when (
            val res = alertRepository.raiseAlert(
                episodeId = episode.id,
                type = AlertType.CARE_PLAN_OVERDUE,
                severity = severity,
                escalationStage = EscalationStage.NURSE_TRIAGE,
                title = "Missed telemonitoring check-in (${hoursOverdue}h since last reading)",
                summary = "${episode.patientName} (${episode.riskTier} risk) exceeded the ${maxAllowedHours(episode.riskTier)}h check-in threshold.",
                patientGuidance = "Please log today's blood pressure, weight, and symptom check-in so your nurse can verify stability.",
                actorName = "Telemonitoring Watchdog",
                actorRole = UserRole.NURSE,
                reasonCode = "MISSED_CHECKIN_THRESHOLD"
            )
        ) {
            is DomainResult.Success -> DomainResult.Success(res.data)
            is DomainResult.Failure -> res
        }
    }
}
