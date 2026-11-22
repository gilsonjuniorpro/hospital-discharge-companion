package com.hospitaldischargecompanion.core.carecircle

import com.google.common.truth.Truth.assertThat
import com.hospitaldischargecompanion.core.demo.ClinicalSeedFactory
import com.hospitaldischargecompanion.core.demo.DemoNetworkSimulator
import com.hospitaldischargecompanion.core.demo.DomainResult
import com.hospitaldischargecompanion.core.demo.MockClinicalStore
import com.hospitaldischargecompanion.testing.DeterministicIdGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class CareCircleRepositoryTest {

    private lateinit var repository: CareCircleRepository

    @Before
    fun setUp() {
        val store = MockClinicalStore()
        val simulator = DemoNetworkSimulator().apply { setLatencyMs(0L) }
        repository = DefaultCareCircleRepository(
            MockCareCircleApi(store, simulator, DeterministicIdGenerator())
        )
    }

    @Test
    fun acceptsValidInviteCodeUpdatesPermissionAndRevokesAccess() = runTest {
        val badCode = repository.acceptByCode("INVALID", "Ana", "Daughter")
        assertThat(badCode).isInstanceOf(DomainResult.Failure::class.java)

        val accepted = repository.acceptByCode("HDC-HF-402", "Ana Silva", "Daughter")
        val member = (accepted as DomainResult.Success).data
        assertThat(member.status).isEqualTo(CareCircleMemberStatus.ACTIVE)
        assertThat(member.dualAttribution("João")).isEqualTo("Ana Silva (Daughter) on behalf of João")

        val escalatedPerm = repository.updatePermission(member.id, CaregiverPermission.ESCALATE)
        assertThat((escalatedPerm as DomainResult.Success).data.permission).isEqualTo(CaregiverPermission.ESCALATE)

        val revoked = repository.revoke(member.id)
        assertThat((revoked as DomainResult.Success).data.status).isEqualTo(CareCircleMemberStatus.REVOKED)
        assertThat(repository.observeCareCircle(ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID).first().any { it.id == member.id }).isTrue()
    }
}
