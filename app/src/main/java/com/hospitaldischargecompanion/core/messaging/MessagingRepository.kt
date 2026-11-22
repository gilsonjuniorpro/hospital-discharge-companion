package com.hospitaldischargecompanion.core.messaging

import com.hospitaldischargecompanion.core.demo.DomainResult
import com.hospitaldischargecompanion.core.demo.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface MessagingRepository {
    fun observeThread(episodeId: String): Flow<List<EpisodeMessage>>
    suspend fun sendMessage(
        episodeId: String,
        senderName: String,
        senderRole: UserRole,
        body: String,
        isClinicalNote: Boolean = false
    ): DomainResult<EpisodeMessage>
    fun getQuickReplyTemplates(role: UserRole): List<QuickReplyTemplate>
}

@Singleton
class DefaultMessagingRepository @Inject constructor(
    private val dataSource: MessagingDataSource
) : MessagingRepository {

    override fun observeThread(episodeId: String): Flow<List<EpisodeMessage>> =
        dataSource.observeAllMessages().map { list -> list.filter { it.episodeId == episodeId } }

    override suspend fun sendMessage(
        episodeId: String,
        senderName: String,
        senderRole: UserRole,
        body: String,
        isClinicalNote: Boolean
    ): DomainResult<EpisodeMessage> = dataSource.sendMessage(
        episodeId = episodeId,
        senderName = senderName,
        senderRole = senderRole,
        body = body,
        isClinicalNote = isClinicalNote
    )

    override fun getQuickReplyTemplates(role: UserRole): List<QuickReplyTemplate> =
        dataSource.getTemplates(role)
}
