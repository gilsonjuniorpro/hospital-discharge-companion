package com.hospitaldischargecompanion.core.di

import com.hospitaldischargecompanion.core.carecircle.CareCircleDataSource
import com.hospitaldischargecompanion.core.carecircle.CareCircleRepository
import com.hospitaldischargecompanion.core.carecircle.DefaultCareCircleRepository
import com.hospitaldischargecompanion.core.carecircle.MockCareCircleApi
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CareCircleModule {
    @Binds
    @Singleton
    abstract fun bindCareCircleDataSource(impl: MockCareCircleApi): CareCircleDataSource

    @Binds
    @Singleton
    abstract fun bindCareCircleRepository(impl: DefaultCareCircleRepository): CareCircleRepository
}
