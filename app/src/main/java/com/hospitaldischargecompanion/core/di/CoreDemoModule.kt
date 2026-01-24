package com.hospitaldischargecompanion.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.hospitaldischargecompanion.core.demo.ClinicalClock
import com.hospitaldischargecompanion.core.demo.DataStoreDemoSessionRepository
import com.hospitaldischargecompanion.core.demo.DemoSessionRepository
import com.hospitaldischargecompanion.core.demo.IdGenerator
import com.hospitaldischargecompanion.core.demo.SequentialIdGenerator
import com.hospitaldischargecompanion.core.demo.SystemClinicalClock
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.hdcSessionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "hdc_demo_session"
)

@Module
@InstallIn(SingletonComponent::class)
abstract class CoreDemoBindingsModule {
    @Binds
    @Singleton
    abstract fun bindClock(impl: SystemClinicalClock): ClinicalClock

    @Binds
    @Singleton
    abstract fun bindIdGenerator(impl: SequentialIdGenerator): IdGenerator

    @Binds
    @Singleton
    abstract fun bindDemoSessionRepository(impl: DataStoreDemoSessionRepository): DemoSessionRepository
}

@Module
@InstallIn(SingletonComponent::class)
object CoreDemoProvidesModule {
    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.hdcSessionDataStore
}
