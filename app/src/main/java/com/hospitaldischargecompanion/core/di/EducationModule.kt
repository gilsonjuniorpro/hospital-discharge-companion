package com.hospitaldischargecompanion.core.di

import com.hospitaldischargecompanion.core.education.DefaultEducationRepository
import com.hospitaldischargecompanion.core.education.EducationDataSource
import com.hospitaldischargecompanion.core.education.EducationRepository
import com.hospitaldischargecompanion.core.education.MockEducationApi
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EducationModule {
    @Binds
    @Singleton
    abstract fun bindEducationDataSource(impl: MockEducationApi): EducationDataSource

    @Binds
    @Singleton
    abstract fun bindEducationRepository(impl: DefaultEducationRepository): EducationRepository
}
