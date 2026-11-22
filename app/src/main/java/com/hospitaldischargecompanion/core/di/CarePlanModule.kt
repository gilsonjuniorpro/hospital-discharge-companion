package com.hospitaldischargecompanion.core.di

import com.hospitaldischargecompanion.core.discharge.CarePlanDataSource
import com.hospitaldischargecompanion.core.discharge.CarePlanRepository
import com.hospitaldischargecompanion.core.discharge.DefaultCarePlanRepository
import com.hospitaldischargecompanion.core.discharge.MockCarePlanApi
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CarePlanModule {
    @Binds
    @Singleton
    abstract fun bindCarePlanDataSource(impl: MockCarePlanApi): CarePlanDataSource

    @Binds
    @Singleton
    abstract fun bindCarePlanRepository(impl: DefaultCarePlanRepository): CarePlanRepository
}
