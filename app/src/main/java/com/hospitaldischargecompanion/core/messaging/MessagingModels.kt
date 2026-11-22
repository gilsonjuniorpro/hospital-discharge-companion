package com.hospitaldischargecompanion.core.messaging

import com.hospitaldischargecompanion.core.demo.UserRole

data class EpisodeMessage(
    val id: String,
    val episodeId: String,
    val senderName: String,
    val senderRole: UserRole,
    val body: String,
    val isClinicalNote: Boolean,
    val timestampIso: String,
    val readByCareTeam: Boolean = true
)

data class QuickReplyTemplate(
    val id: String,
    val label: String,
    val body: String,
    val clinicianOnly: Boolean = false
)
