package com.hospitaldischargecompanion.core.appointments

import com.hospitaldischargecompanion.core.demo.DomainResult
import com.hospitaldischargecompanion.core.demo.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface AppointmentRepository {
    fun observeAppointments(episodeId: String): Flow<List<AppointmentItem>>
    suspend fun markAttended(appointmentId: String): DomainResult<AppointmentItem>
    suspend fun markMissed(appointmentId: String, actorName: String): DomainResult<AppointmentItem>
    suspend fun requestReschedule(appointmentId: String, preferredSlot: String): DomainResult<AppointmentItem>
    suspend fun approveReschedule(appointmentId: String, actorRole: UserRole): DomainResult<AppointmentItem>
}

@Singleton
class DefaultAppointmentRepository @Inject constructor(
    private val dataSource: AppointmentDataSource
) : AppointmentRepository {
    override fun observeAppointments(episodeId: String): Flow<List<AppointmentItem>> =
        dataSource.observeAllAppointments().map { list -> list.filter { it.episodeId == episodeId } }

    override suspend fun markAttended(appointmentId: String): DomainResult<AppointmentItem> =
        dataSource.markAttended(appointmentId)

    override suspend fun markMissed(appointmentId: String, actorName: String): DomainResult<AppointmentItem> =
        dataSource.markMissed(appointmentId, actorName)

    override suspend fun requestReschedule(appointmentId: String, preferredSlot: String): DomainResult<AppointmentItem> =
        dataSource.requestReschedule(appointmentId, preferredSlot)

    override suspend fun approveReschedule(appointmentId: String, actorRole: UserRole): DomainResult<AppointmentItem> =
        dataSource.approveReschedule(appointmentId, actorRole)
}
