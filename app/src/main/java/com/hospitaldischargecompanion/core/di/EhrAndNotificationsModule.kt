package com.hospitaldischargecompanion.core.di

import com.hospitaldischargecompanion.core.ehr.DefaultEhrSyncRepository
import com.hospitaldischargecompanion.core.ehr.EhrSyncDataSource
import com.hospitaldischargecompanion.core.ehr.EhrSyncRepository
import com.hospitaldischargecompanion.core.ehr.MockEhrSyncApi
import com.hospitaldischargecompanion.core.notifications.MockNotificationCenterRepository
import com.hospitaldischargecompanion.core.notifications.NotificationCenterRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EhrAndNotificationsModule {
    @Binds
    @Singleton
    abstract fun bindEhrSyncDataSource(impl: MockEhrSyncApi): EhrSyncDataSource

    @Binds
    @Singleton
    abstract fun bindEhrSyncRepository(impl: DefaultEhrSyncRepository): EhrSyncRepository

    @Binds
    @Singleton
    abstract fun bindNotificationCenterRepository(impl: MockNotificationCenterRepository): NotificationCenterRepository
}
