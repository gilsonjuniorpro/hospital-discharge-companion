package com.hospitaldischargecompanion.core.messaging

import com.hospitaldischargecompanion.core.demo.ClinicalClock
import com.hospitaldischargecompanion.core.demo.ClinicalSeedFactory
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

interface MessagingDataSource {
    fun observeAllMessages(): Flow<List<EpisodeMessage>>
    suspend fun sendMessage(
        episodeId: String,
        senderName: String,
        senderRole: UserRole,
        body: String,
        isClinicalNote: Boolean
    ): DomainResult<EpisodeMessage>
    fun getTemplates(role: UserRole): List<QuickReplyTemplate>
}

@Singleton
class MockMessagingApi @Inject constructor(
    store: MockClinicalStore,
    private val networkSimulator: DemoNetworkSimulator,
    private val clock: ClinicalClock,
    private val idGenerator: IdGenerator
) : MessagingDataSource {

    private fun seedMessages(): List<EpisodeMessage> = listOf(
        EpisodeMessage(
            id = "msg-hf-1",
            episodeId = ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID,
            senderName = "RN Clara Mendes",
            senderRole = UserRole.NURSE,
            body = "Welcome home João! Remember to log your morning dry weight before breakfast.",
            isClinicalNote = false,
            timestampIso = "2026-06-13T08:00:00Z",
            readByCareTeam = true
        ),
        EpisodeMessage(
            id = "msg-hf-2",
            episodeId = ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID,
            senderName = "Maria Silva",
            senderRole = UserRole.FAMILY,
            body = "Thanks Clara, we organized his morning pill box and weighed him at 77.6 kg today.",
            isClinicalNote = false,
            timestampIso = "2026-06-14T08:35:00Z",
            readByCareTeam = true
        ),
        EpisodeMessage(
            id = "msg-hf-3",
            episodeId = ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID,
            senderName = "RN Clara Mendes",
            senderRole = UserRole.NURSE,
            body = "Clinical Note: +1.1 kg above discharge dry weight. Reinforced low-sodium intake and monitoring day-3 BP/weight.",
            isClinicalNote = true,
            timestampIso = "2026-06-14T09:10:00Z",
            readByCareTeam = true
        ),
        EpisodeMessage(
            id = "msg-ps-1",
            episodeId = ClinicalSeedFactory.POST_SURGERY_EPISODE_ID,
            senderName = "RN David Park",
            senderRole = UserRole.NURSE,
            body = "Checking in on your port incisions today — let us know if redness or fever develops.",
            isClinicalNote = false,
            timestampIso = "2026-06-14T10:00:00Z",
            readByCareTeam = true
        )
    )

    private val _messages = MutableStateFlow(seedMessages())

    init {
        store.registerResetCallback { _messages.value = seedMessages() }
    }

    override fun observeAllMessages(): Flow<List<EpisodeMessage>> = _messages.asStateFlow()

    override suspend fun sendMessage(
        episodeId: String,
        senderName: String,
        senderRole: UserRole,
        body: String,
        isClinicalNote: Boolean
    ): DomainResult<EpisodeMessage> = networkSimulator.execute {
        if (body.isBlank()) {
            return@execute DomainResult.Failure(DomainError.ValidationFailed("body", "Message cannot be empty"))
        }
        if (isClinicalNote && !senderRole.canWriteClinicalNotes) {
            return@execute DomainResult.Failure(
                DomainError.UnauthorizedRoleAction(senderRole, "mark_clinical_note")
            )
        }
        val message = EpisodeMessage(
            id = idGenerator.nextId("msg"),
            episodeId = episodeId,
            senderName = senderName,
            senderRole = senderRole,
            body = body.trim(),
            isClinicalNote = isClinicalNote,
            timestampIso = clock.now().toString(),
            readByCareTeam = senderRole.isClinician
        )
        _messages.update { it + message }
        DomainResult.Success(message)
    }

    override fun getTemplates(role: UserRole): List<QuickReplyTemplate> = listOf(
        QuickReplyTemplate(
            id = "tpl-1",
            label = "Took morning meds",
            body = "Morning discharge medications have been taken as scheduled."
        ),
        QuickReplyTemplate(
            id = "tpl-2",
            label = "Need refill help",
            body = "We are running low on a discharge medication and need pharmacy refill support."
        ),
        QuickReplyTemplate(
            id = "tpl-3",
            label = "Request BP recheck",
            body = "Please sit quietly for 5 minutes and repeat blood pressure and weight check now.",
            clinicianOnly = true
        ),
        QuickReplyTemplate(
            id = "tpl-4",
            label = "Nursing triage note",
            body = "Clinical Note: Spoken with patient/caregiver; reviewed red flags and confirmed next telemonitoring check-in.",
            clinicianOnly = true
        )
    ).filter { !it.clinicianOnly || role.isClinician }
}
