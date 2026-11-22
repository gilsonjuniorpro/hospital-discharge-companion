package com.hospitaldischargecompanion.core.ehr

import com.hospitaldischargecompanion.core.discharge.EhrSyncStatus

enum class ConflictResolutionChoice {
    KEEP_LOCAL_HOME,
    ACCEPT_EHR_UPSTREAM,
    MERGE_WITH_CLINICAL_NOTE
}

data class EhrConflictRecord(
    val id: String,
    val episodeId: String,
    val resourceType: String,
    val localSummary: String,
    val ehrUpstreamSummary: String,
    val detectedAtIso: String,
    val resolvedChoice: ConflictResolutionChoice? = null
)

data class EhrSyncLogEntry(
    val id: String,
    val episodeId: String,
    val direction: String,
    val outcomeStatus: EhrSyncStatus,
    val detail: String,
    val timestampIso: String
)
