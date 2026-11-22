package com.hospitaldischargecompanion.core.demo

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DemoNetworkSimulatorTest {

    @Test
    fun returnsSimulatedTimeoutAndUnavailableForAllOperations() = runTest {
        val simulator = DemoNetworkSimulator().apply { setLatencyMs(0L) }

        simulator.setFailureMode(SimulatedFailureMode.TIMEOUT)
        val timeoutResult = simulator.execute { DomainResult.Success("ok") }
        assertThat(timeoutResult).isEqualTo(DomainResult.Failure(DomainError.NetworkTimeout))

        simulator.setFailureMode(SimulatedFailureMode.NETWORK_UNAVAILABLE)
        val offlineResult = simulator.execute { DomainResult.Success("ok") }
        assertThat(offlineResult).isEqualTo(DomainResult.Failure(DomainError.NetworkUnavailable))
    }

    @Test
    fun appliesEhrFailuresOnlyToEhrOperations() = runTest {
        val simulator = DemoNetworkSimulator().apply { setLatencyMs(0L) }
        simulator.setFailureMode(SimulatedFailureMode.EHR_UNAUTHORIZED_401)

        val localResult = simulator.execute(isEhrOperation = false) { DomainResult.Success("local") }
        val ehrResult = simulator.execute(isEhrOperation = true) { DomainResult.Success("remote") }

        assertThat(localResult).isEqualTo(DomainResult.Success("local"))
        assertThat(ehrResult).isEqualTo(DomainResult.Failure(DomainError.EhrUnauthorized))

        simulator.setFailureMode(SimulatedFailureMode.EHR_CONFLICT_409)
        val conflictResult = simulator.execute(isEhrOperation = true, resourceId = "obs-123") {
            DomainResult.Success("remote")
        }
        assertThat(conflictResult).isInstanceOf(DomainResult.Failure::class.java)
        val error = (conflictResult as DomainResult.Failure).error
        assertThat(error).isInstanceOf(DomainError.EhrSyncConflict::class.java)
        assertThat(error.code).isEqualTo("EHR_CONFLICT_409")
    }

    @Test
    fun roleCapabilitiesAndLocaleParsingMatchSpecification() {
        assertThat(UserRole.PATIENT.isClinician).isFalse()
        assertThat(UserRole.FAMILY.canReopenTasks).isFalse()
        assertThat(UserRole.NURSE.canReopenTasks).isTrue()
        assertThat(UserRole.NURSE.canModifyCarePlan).isFalse()
        assertThat(UserRole.PHYSICIAN.canModifyCarePlan).isTrue()

        assertThat(AppLocale.fromTag("pt-BR")).isEqualTo(AppLocale.PT_BR)
        assertThat(AppLocale.fromTag("es")).isEqualTo(AppLocale.ES)
        assertThat(AppLocale.fromTag("fr")).isEqualTo(AppLocale.FR)
        assertThat(AppLocale.fromTag("unknown")).isEqualTo(AppLocale.SYSTEM)
    }
}
