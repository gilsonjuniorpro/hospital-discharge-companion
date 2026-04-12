package com.hospitaldischargecompanion.core.carecircle

import com.hospitaldischargecompanion.core.demo.ClinicalSeedFactory
import com.hospitaldischargecompanion.core.demo.DemoNetworkSimulator
import com.hospitaldischargecompanion.core.demo.DomainError
import com.hospitaldischargecompanion.core.demo.DomainResult
import com.hospitaldischargecompanion.core.demo.IdGenerator
import com.hospitaldischargecompanion.core.demo.MockClinicalStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

interface CareCircleDataSource {
    fun observeAllMembers(): Flow<List<CareCircleMember>>
    suspend fun inviteCaregiver(
        episodeId: String,
        name: String,
        relationship: String,
        permission: CaregiverPermission
    ): DomainResult<CareCircleMember>
    suspend fun acceptByInviteCode(
        inviteCode: String,
        caregiverName: String,
        relationship: String
    ): DomainResult<CareCircleMember>
    suspend fun updatePermission(memberId: String, permission: CaregiverPermission): DomainResult<CareCircleMember>
    suspend fun revokeMember(memberId: String): DomainResult<CareCircleMember>
}

@Singleton
class MockCareCircleApi @Inject constructor(
    private val store: MockClinicalStore,
    private val networkSimulator: DemoNetworkSimulator,
    private val idGenerator: IdGenerator
) : CareCircleDataSource {

    private fun seedMembers(): List<CareCircleMember> = listOf(
        CareCircleMember(
            id = "cg-hf-maria",
            episodeId = ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID,
            name = "Maria Silva",
            relationship = "Spouse",
            permission = CaregiverPermission.ESCALATE,
            status = CareCircleMemberStatus.ACTIVE,
            inviteCodeUsed = "HDC-HF-402"
        ),
        CareCircleMember(
            id = "cg-hf-lucas",
            episodeId = ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID,
            name = "Lucas Silva",
            relationship = "Son",
            permission = CaregiverPermission.VIEW_ONLY,
            status = CareCircleMemberStatus.INVITED,
            inviteCodeUsed = "HDC-HF-402"
        ),
        CareCircleMember(
            id = "cg-ps-paulo",
            episodeId = ClinicalSeedFactory.POST_SURGERY_EPISODE_ID,
            name = "Paulo Oliveira",
            relationship = "Partner",
            permission = CaregiverPermission.ASSIST_MEDS_AND_CHECKINS,
            status = CareCircleMemberStatus.ACTIVE,
            inviteCodeUsed = "HDC-PS-719"
        )
    )

    private val _members = MutableStateFlow(seedMembers())

    init {
        store.registerResetCallback { _members.value = seedMembers() }
    }

    override fun observeAllMembers(): Flow<List<CareCircleMember>> = _members.asStateFlow()

    override suspend fun inviteCaregiver(
        episodeId: String,
        name: String,
        relationship: String,
        permission: CaregiverPermission
    ): DomainResult<CareCircleMember> = networkSimulator.execute {
        val episode = store.getEpisode(episodeId)
            ?: return@execute DomainResult.Failure(DomainError.EpisodeNotFound(episodeId))
        if (name.isBlank()) {
            return@execute DomainResult.Failure(DomainError.ValidationFailed("name", "Caregiver name required"))
        }
        val member = CareCircleMember(
            id = idGenerator.nextId("cg"),
            episodeId = episodeId,
            name = name.trim(),
            relationship = relationship.ifBlank { "Family" }.trim(),
            permission = permission,
            status = CareCircleMemberStatus.INVITED,
            inviteCodeUsed = episode.careCircleInviteCode
        )
        _members.update { it + member }
        DomainResult.Success(member)
    }

    override suspend fun acceptByInviteCode(
        inviteCode: String,
        caregiverName: String,
        relationship: String
    ): DomainResult<CareCircleMember> = networkSimulator.execute {
        val episode = store.episodes.value.firstOrNull {
            it.careCircleInviteCode.equals(inviteCode.trim(), ignoreCase = true)
        } ?: return@execute DomainResult.Failure(
            DomainError.ValidationFailed("inviteCode", "Invalid demo care-circle code")
        )
        val existingInvited = _members.value.firstOrNull {
            it.episodeId == episode.id && it.status == CareCircleMemberStatus.INVITED
        }
        if (existingInvited != null && caregiverName.isBlank()) {
            val activated = existingInvited.copy(status = CareCircleMemberStatus.ACTIVE)
            _members.update { list -> list.map { if (it.id == existingInvited.id) activated else it } }
            return@execute DomainResult.Success(activated)
        }
        val member = CareCircleMember(
            id = idGenerator.nextId("cg"),
            episodeId = episode.id,
            name = caregiverName.ifBlank { "Family Caregiver" }.trim(),
            relationship = relationship.ifBlank { "Family" }.trim(),
            permission = CaregiverPermission.ASSIST_MEDS_AND_CHECKINS,
            status = CareCircleMemberStatus.ACTIVE,
            inviteCodeUsed = episode.careCircleInviteCode
        )
        _members.update { it + member }
        DomainResult.Success(member)
    }

    override suspend fun updatePermission(
        memberId: String,
        permission: CaregiverPermission
    ): DomainResult<CareCircleMember> = networkSimulator.execute {
        var updated: CareCircleMember? = null
        _members.update { list ->
            list.map {
                if (it.id == memberId) it.copy(permission = permission).also { m -> updated = m } else it
            }
        }
        updated?.let { DomainResult.Success(it) }
            ?: DomainResult.Failure(DomainError.ValidationFailed("memberId", "Caregiver not found"))
    }

    override suspend fun revokeMember(memberId: String): DomainResult<CareCircleMember> =
        networkSimulator.execute {
            var updated: CareCircleMember? = null
            _members.update { list ->
                list.map {
                    if (it.id == memberId) it.copy(status = CareCircleMemberStatus.REVOKED).also { m -> updated = m } else it
                }
            }
            updated?.let { DomainResult.Success(it) }
                ?: DomainResult.Failure(DomainError.ValidationFailed("memberId", "Caregiver not found"))
        }
}
