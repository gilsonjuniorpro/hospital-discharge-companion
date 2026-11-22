package com.hospitaldischargecompanion.core.messaging

import com.google.common.truth.Truth.assertThat
import com.hospitaldischargecompanion.core.demo.ClinicalSeedFactory
import com.hospitaldischargecompanion.core.demo.DemoNetworkSimulator
import com.hospitaldischargecompanion.core.demo.DomainError
import com.hospitaldischargecompanion.core.demo.DomainResult
import com.hospitaldischargecompanion.core.demo.MockClinicalStore
import com.hospitaldischargecompanion.core.demo.UserRole
import com.hospitaldischargecompanion.testing.DeterministicIdGenerator
import com.hospitaldischargecompanion.testing.FixedClinicalClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class MessagingRepositoryTest {

    private lateinit var repository: MessagingRepository

    @Before
    fun setUp() {
        val store = MockClinicalStore()
        val simulator = DemoNetworkSimulator().apply { setLatencyMs(0L) }
        repository = DefaultMessagingRepository(
            MockMessagingApi(store, simulator, FixedClinicalClock(), DeterministicIdGenerator())
        )
    }

    @Test
    fun blocksClinicalNoteFlagForPatientAndAllowsClinicianNotes() = runTest {
        val denied = repository.sendMessage(
            episodeId = ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID,
            senderName = "João Silva",
            senderRole = UserRole.PATIENT,
            body = "Trying to post clinical note",
            isClinicalNote = true
        )
        assertThat(denied).isEqualTo(
            DomainResult.Failure(DomainError.UnauthorizedRoleAction(UserRole.PATIENT, "mark_clinical_note"))
        )

        val allowed = repository.sendMessage(
            episodeId = ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID,
            senderName = "RN Clara Mendes",
            senderRole = UserRole.NURSE,
            body = "Clinical Note: Requested 20:00 repeat weight",
            isClinicalNote = true
        )
        assertThat((allowed as DomainResult.Success).data.isClinicalNote).isTrue()
        assertThat(repository.observeThread(ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID).first().size).isEqualTo(4)
        assertThat(repository.getQuickReplyTemplates(UserRole.PATIENT).none { it.clinicianOnly }).isTrue()
        assertThat(repository.getQuickReplyTemplates(UserRole.NURSE).any { it.clinicianOnly }).isTrue()
    }
}
