package com.hospitaldischargecompanion.core.telemonitor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface WearableIntegrationHook {
    fun observeDevices(): Flow<List<WearableDeviceSummary>>
    suspend fun simulateBluetoothSync(): List<WearableDeviceSummary>
}

@Singleton
class MockWearableIntegrationHook @Inject constructor() : WearableIntegrationHook {
    private val _devices = MutableStateFlow(
        listOf(
            WearableDeviceSummary(
                id = "ble-bp-01",
                name = "Omron BLE Upper-Arm Cuff (Demo Stub)",
                modality = "Blood Pressure & Pulse",
                status = WearableConnectionStatus.PAIRED_MOCK_DEMO,
                lastReadingLabel = "142/86 mmHg · 82 bpm"
            ),
            WearableDeviceSummary(
                id = "ble-scale-02",
                name = "Withings Home Dry-Weight Scale (Demo Stub)",
                modality = "Body Weight (kg)",
                status = WearableConnectionStatus.DISCONNECTED,
                lastReadingLabel = "77.6 kg"
            )
        )
    )

    override fun observeDevices(): Flow<List<WearableDeviceSummary>> = _devices.asStateFlow()

    override suspend fun simulateBluetoothSync(): List<WearableDeviceSummary> {
        val synced = _devices.value.map { it.copy(status = WearableConnectionStatus.PAIRED_MOCK_DEMO) }
        _devices.value = synced
        return synced
    }
}
