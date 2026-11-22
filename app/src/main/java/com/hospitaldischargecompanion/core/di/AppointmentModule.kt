package com.hospitaldischargecompanion.core.di

import com.hospitaldischargecompanion.core.appointments.AppointmentDataSource
import com.hospitaldischargecompanion.core.appointments.AppointmentRepository
import com.hospitaldischargecompanion.core.appointments.DefaultAppointmentRepository
import com.hospitaldischargecompanion.core.appointments.MockAppointmentApi
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppointmentModule {
    @Binds
    @Singleton
    abstract fun bindAppointmentDataSource(impl: MockAppointmentApi): AppointmentDataSource

    @Binds
    @Singleton
    abstract fun bindAppointmentRepository(impl: DefaultAppointmentRepository): AppointmentRepository
}
