package com.hospitaldischargecompanion.core.di

import com.hospitaldischargecompanion.core.messaging.DefaultMessagingRepository
import com.hospitaldischargecompanion.core.messaging.MessagingDataSource
import com.hospitaldischargecompanion.core.messaging.MessagingRepository
import com.hospitaldischargecompanion.core.messaging.MockMessagingApi
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MessagingModule {
    @Binds
    @Singleton
    abstract fun bindMessagingDataSource(impl: MockMessagingApi): MessagingDataSource

    @Binds
    @Singleton
    abstract fun bindMessagingRepository(impl: DefaultMessagingRepository): MessagingRepository
}
