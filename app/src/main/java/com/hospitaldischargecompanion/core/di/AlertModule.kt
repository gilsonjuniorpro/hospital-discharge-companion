package com.hospitaldischargecompanion.core.di

import com.hospitaldischargecompanion.core.alerts.AlertDataSource
import com.hospitaldischargecompanion.core.alerts.AlertRepository
import com.hospitaldischargecompanion.core.alerts.DefaultAlertRepository
import com.hospitaldischargecompanion.core.alerts.MockAlertApi
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AlertModule {
    @Binds
    @Singleton
    abstract fun bindAlertDataSource(impl: MockAlertApi): AlertDataSource

    @Binds
    @Singleton
    abstract fun bindAlertRepository(impl: DefaultAlertRepository): AlertRepository
}
