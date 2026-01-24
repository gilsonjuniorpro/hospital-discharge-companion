package com.hospitaldischargecompanion.core.demo

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DemoNetworkSimulator @Inject constructor() {
    private val _config = MutableStateFlow(DemoNetworkConfig())
    val config: StateFlow<DemoNetworkConfig> = _config.asStateFlow()

    fun setLatencyMs(latencyMs: Long) {
        _config.update { it.copy(latencyMs = latencyMs.coerceIn(0L, 4000L)) }
    }

    fun setFailureMode(mode: SimulatedFailureMode) {
        _config.update { it.copy(failureMode = mode) }
    }

    fun reset() {
        _config.value = DemoNetworkConfig()
    }

    suspend fun <T> execute(
        isEhrOperation: Boolean = false,
        resourceId: String = "bundle",
        block: suspend () -> DomainResult<T>
    ): DomainResult<T> {
        val current = _config.value
        if (current.latencyMs > 0L) {
            delay(current.latencyMs)
        }
        return when (current.failureMode) {
            SimulatedFailureMode.TIMEOUT -> DomainResult.Failure(DomainError.NetworkTimeout)
            SimulatedFailureMode.NETWORK_UNAVAILABLE -> DomainResult.Failure(DomainError.NetworkUnavailable)
            SimulatedFailureMode.EHR_UNAUTHORIZED_401 ->
                if (isEhrOperation) DomainResult.Failure(DomainError.EhrUnauthorized) else block()
            SimulatedFailureMode.EHR_CONFLICT_409 ->
                if (isEhrOperation) {
                    DomainResult.Failure(
                        DomainError.EhrSyncConflict(
                            resourceId = resourceId,
                            reason = "Local home observation disagrees with upstream EHR observation version"
                        )
                    )
                } else {
                    block()
                }
            SimulatedFailureMode.NONE -> block()
        }
    }
}
