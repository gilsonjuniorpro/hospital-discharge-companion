package com.hospitaldischargecompanion.core.di

import com.hospitaldischargecompanion.core.medication.DefaultMedicationRepository
import com.hospitaldischargecompanion.core.medication.MedicationDataSource
import com.hospitaldischargecompanion.core.medication.MedicationRepository
import com.hospitaldischargecompanion.core.medication.MockMedicationApi
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MedicationModule {
    @Binds
    @Singleton
    abstract fun bindMedicationDataSource(impl: MockMedicationApi): MedicationDataSource

    @Binds
    @Singleton
    abstract fun bindMedicationRepository(impl: DefaultMedicationRepository): MedicationRepository
}
