package com.hospitaldischargecompanion.core.di

import com.hospitaldischargecompanion.core.discharge.DefaultDischargeEpisodeRepository
import com.hospitaldischargecompanion.core.discharge.DischargeDataSource
import com.hospitaldischargecompanion.core.discharge.DischargeEpisodeRepository
import com.hospitaldischargecompanion.core.discharge.MockDischargeApi
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DischargeModule {
    @Binds
    @Singleton
    abstract fun bindDischargeDataSource(impl: MockDischargeApi): DischargeDataSource

    @Binds
    @Singleton
    abstract fun bindDischargeEpisodeRepository(impl: DefaultDischargeEpisodeRepository): DischargeEpisodeRepository
}
