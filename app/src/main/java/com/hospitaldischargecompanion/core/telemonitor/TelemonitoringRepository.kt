package com.hospitaldischargecompanion.core.telemonitor

import com.hospitaldischargecompanion.core.demo.DomainResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface TelemonitoringRepository {
    fun observeCheckIns(episodeId: String): Flow<List<VitalCheckIn>>
    fun observeAllCheckIns(): Flow<List<VitalCheckIn>>
    fun observeBaseline(episodeId: String): Flow<VitalBaseline?>
    fun observeWearables(): Flow<List<WearableDeviceSummary>>
    suspend fun submitCheckIn(request: NewCheckInRequest): DomainResult<VitalCheckIn>
    suspend fun syncWearableStub(): List<WearableDeviceSummary>
}

@Singleton
class DefaultTelemonitoringRepository @Inject constructor(
    private val dataSource: TelemonitoringDataSource,
    private val wearableHook: WearableIntegrationHook
) : TelemonitoringRepository {

    override fun observeCheckIns(episodeId: String): Flow<List<VitalCheckIn>> =
        dataSource.observeAllCheckIns().map { list ->
            list.filter { it.episodeId == episodeId }.sortedBy { it.dayNumber }
        }

    override fun observeAllCheckIns(): Flow<List<VitalCheckIn>> =
        dataSource.observeAllCheckIns()

    override fun observeBaseline(episodeId: String): Flow<VitalBaseline?> =
        dataSource.observeBaselines().map { list -> list.firstOrNull { it.episodeId == episodeId } }

    override fun observeWearables(): Flow<List<WearableDeviceSummary>> =
        wearableHook.observeDevices()

    override suspend fun submitCheckIn(request: NewCheckInRequest): DomainResult<VitalCheckIn> =
        dataSource.submitCheckIn(request)

    override suspend fun syncWearableStub(): List<WearableDeviceSummary> =
        wearableHook.simulateBluetoothSync()
}
