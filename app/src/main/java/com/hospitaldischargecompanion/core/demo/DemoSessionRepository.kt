package com.hospitaldischargecompanion.core.demo

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface DemoSessionRepository {
    val sessionState: Flow<DemoSessionState>
    suspend fun setActiveRole(role: UserRole?)
    suspend fun setSelectedEpisode(episodeId: String)
    suspend fun setLocale(locale: AppLocale)
    suspend fun setTemperatureUnit(unit: TemperatureUnit)
    suspend fun setQuietHoursEnabled(enabled: Boolean)
    suspend fun setCaregiverAlertsEnabled(enabled: Boolean)
    suspend fun resetSession()
}

@Singleton
class DataStoreDemoSessionRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : DemoSessionRepository {

    private object Keys {
        val ACTIVE_ROLE = stringPreferencesKey("active_role")
        val SELECTED_EPISODE = stringPreferencesKey("selected_episode")
        val APP_LOCALE = stringPreferencesKey("app_locale")
        val TEMP_UNIT = stringPreferencesKey("temp_unit")
        val QUIET_HOURS = booleanPreferencesKey("quiet_hours")
        val CAREGIVER_ALERTS = booleanPreferencesKey("caregiver_alerts")
    }

    override val sessionState: Flow<DemoSessionState> = dataStore.data.map { prefs ->
        DemoSessionState(
            activeRole = prefs[Keys.ACTIVE_ROLE]?.let { raw ->
                UserRole.entries.firstOrNull { it.name == raw }
            },
            selectedEpisodeId = prefs[Keys.SELECTED_EPISODE] ?: "ep-hf-001",
            locale = AppLocale.fromTag(prefs[Keys.APP_LOCALE] ?: AppLocale.EN.languageTag),
            temperatureUnit = prefs[Keys.TEMP_UNIT]?.let { raw ->
                TemperatureUnit.entries.firstOrNull { it.name == raw }
            } ?: TemperatureUnit.CELSIUS,
            quietHoursEnabled = prefs[Keys.QUIET_HOURS] ?: false,
            caregiverAlertsEnabled = prefs[Keys.CAREGIVER_ALERTS] ?: true
        )
    }

    override suspend fun setActiveRole(role: UserRole?) {
        dataStore.edit { prefs ->
            if (role == null) prefs.remove(Keys.ACTIVE_ROLE) else prefs[Keys.ACTIVE_ROLE] = role.name
        }
    }

    override suspend fun setSelectedEpisode(episodeId: String) {
        dataStore.edit { prefs ->
            prefs[Keys.SELECTED_EPISODE] = episodeId
        }
    }

    override suspend fun setLocale(locale: AppLocale) {
        dataStore.edit { prefs ->
            prefs[Keys.APP_LOCALE] = locale.languageTag
        }
    }

    override suspend fun setTemperatureUnit(unit: TemperatureUnit) {
        dataStore.edit { prefs ->
            prefs[Keys.TEMP_UNIT] = unit.name
        }
    }

    override suspend fun setQuietHoursEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.QUIET_HOURS] = enabled
        }
    }

    override suspend fun setCaregiverAlertsEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.CAREGIVER_ALERTS] = enabled
        }
    }

    override suspend fun resetSession() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.ACTIVE_ROLE)
            prefs[Keys.SELECTED_EPISODE] = "ep-hf-001"
            prefs[Keys.QUIET_HOURS] = false
            prefs[Keys.CAREGIVER_ALERTS] = true
        }
    }
}
