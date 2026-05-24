package com.example.timetable.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private val keyMinutesToTokyo = intPreferencesKey("minutes_to_tokyo_station")
    private val keyMinutesToTochigi = intPreferencesKey("minutes_to_tochigi_station")

    val settings: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            minutesToTokyoStation = prefs[keyMinutesToTokyo] ?: DEFAULT_MINUTES_TO_TOKYO,
            minutesToTochigiStation = prefs[keyMinutesToTochigi] ?: DEFAULT_MINUTES_TO_TOCHIGI,
        )
    }

    suspend fun setMinutesToTokyo(minutes: Int) {
        context.dataStore.edit { it[keyMinutesToTokyo] = minutes.coerceIn(0, 180) }
    }

    suspend fun setMinutesToTochigi(minutes: Int) {
        context.dataStore.edit { it[keyMinutesToTochigi] = minutes.coerceIn(0, 180) }
    }

    companion object {
        const val DEFAULT_MINUTES_TO_TOKYO = 30
        const val DEFAULT_MINUTES_TO_TOCHIGI = 10
    }
}

data class UserSettings(
    val minutesToTokyoStation: Int,
    val minutesToTochigiStation: Int,
) {
    fun minutesToBoardingStationFor(direction: DirectionKey): Int = when (direction) {
        // 東京行きの便は栃木駅発 → 栃木駅までの所要時間
        DirectionKey.ToTokyo -> minutesToTochigiStation
        // 栃木行きの便は東京駅発 → 東京駅までの所要時間
        DirectionKey.ToTochigi -> minutesToTokyoStation
    }
}
