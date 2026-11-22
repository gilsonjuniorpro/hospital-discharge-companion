package com.hospitaldischargecompanion.core.carecircle

import com.hospitaldischargecompanion.core.demo.DomainResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface CareCircleRepository {
    fun observeCareCircle(episodeId: String): Flow<List<CareCircleMember>>
    suspend fun invite(episodeId: String, name: String, relationship: String, permission: CaregiverPermission): DomainResult<CareCircleMember>
    suspend fun acceptByCode(inviteCode: String, caregiverName: String, relationship: String): DomainResult<CareCircleMember>
    suspend fun updatePermission(memberId: String, permission: CaregiverPermission): DomainResult<CareCircleMember>
    suspend fun revoke(memberId: String): DomainResult<CareCircleMember>
}

@Singleton
class DefaultCareCircleRepository @Inject constructor(
    private val dataSource: CareCircleDataSource
) : CareCircleRepository {

    override fun observeCareCircle(episodeId: String): Flow<List<CareCircleMember>> =
        dataSource.observeAllMembers().map { list -> list.filter { it.episodeId == episodeId } }

    override suspend fun invite(
        episodeId: String,
        name: String,
        relationship: String,
        permission: CaregiverPermission
    ): DomainResult<CareCircleMember> = dataSource.inviteCaregiver(episodeId, name, relationship, permission)

    override suspend fun acceptByCode(
        inviteCode: String,
        caregiverName: String,
        relationship: String
    ): DomainResult<CareCircleMember> = dataSource.acceptByInviteCode(inviteCode, caregiverName, relationship)

    override suspend fun updatePermission(
        memberId: String,
        permission: CaregiverPermission
    ): DomainResult<CareCircleMember> = dataSource.updatePermission(memberId, permission)

    override suspend fun revoke(memberId: String): DomainResult<CareCircleMember> =
        dataSource.revokeMember(memberId)
}
