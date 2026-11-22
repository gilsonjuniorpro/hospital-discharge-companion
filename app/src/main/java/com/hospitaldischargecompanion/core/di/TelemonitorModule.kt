package com.hospitaldischargecompanion.core.di

import com.hospitaldischargecompanion.core.telemonitor.DefaultTelemonitoringRepository
import com.hospitaldischargecompanion.core.telemonitor.MockTelemonitoringApi
import com.hospitaldischargecompanion.core.telemonitor.MockWearableIntegrationHook
import com.hospitaldischargecompanion.core.telemonitor.TelemonitoringDataSource
import com.hospitaldischargecompanion.core.telemonitor.TelemonitoringRepository
import com.hospitaldischargecompanion.core.telemonitor.WearableIntegrationHook
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TelemonitorModule {
    @Binds
    @Singleton
    abstract fun bindTelemonitoringDataSource(impl: MockTelemonitoringApi): TelemonitoringDataSource

    @Binds
    @Singleton
    abstract fun bindTelemonitoringRepository(impl: DefaultTelemonitoringRepository): TelemonitoringRepository

    @Binds
    @Singleton
    abstract fun bindWearableHook(impl: MockWearableIntegrationHook): WearableIntegrationHook
}
