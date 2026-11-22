package com.hospitaldischargecompanion.core.education

import com.hospitaldischargecompanion.core.demo.DomainResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface EducationRepository {
    fun observeModules(episodeId: String): Flow<List<EducationModuleItem>>
    suspend fun markOpened(moduleId: String): DomainResult<EducationModuleItem>
    suspend fun markCompleted(moduleId: String): DomainResult<EducationModuleItem>
    suspend fun submitQuizAnswer(moduleId: String, selectedOptionIndex: Int): DomainResult<Pair<EducationModuleItem, Boolean>>
}

@Singleton
class DefaultEducationRepository @Inject constructor(
    private val dataSource: EducationDataSource
) : EducationRepository {

    override fun observeModules(episodeId: String): Flow<List<EducationModuleItem>> =
        dataSource.observeAllModules().map { list -> list.filter { it.episodeId == episodeId } }

    override suspend fun markOpened(moduleId: String): DomainResult<EducationModuleItem> =
        dataSource.markOpened(moduleId)

    override suspend fun markCompleted(moduleId: String): DomainResult<EducationModuleItem> =
        dataSource.markCompleted(moduleId)

    override suspend fun submitQuizAnswer(
        moduleId: String,
        selectedOptionIndex: Int
    ): DomainResult<Pair<EducationModuleItem, Boolean>> =
        dataSource.submitQuizAnswer(moduleId, selectedOptionIndex)
}
