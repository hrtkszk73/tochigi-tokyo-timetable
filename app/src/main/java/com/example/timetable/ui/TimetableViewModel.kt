package com.example.timetable.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.timetable.data.DirectionKey
import com.example.timetable.data.SettingsRepository
import com.example.timetable.data.Timetable
import com.example.timetable.data.TimetableRepository
import com.example.timetable.data.Train
import com.example.timetable.data.UserSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

class TimetableViewModel(app: Application) : AndroidViewModel(app) {

    private val timetableRepo = TimetableRepository(app)
    private val settingsRepo = SettingsRepository(app)

    private val timetable: Timetable = timetableRepo.load()

    private val nowFlow: StateFlow<LocalDateTime> = flow {
        while (true) {
            emit(LocalDateTime.now())
            delay(1_000L)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), LocalDateTime.now())

    private val _selectedDirection = MutableStateFlow(DirectionKey.ToTokyo)
    val selectedDirection: StateFlow<DirectionKey> = _selectedDirection.asStateFlow()

    val uiState: StateFlow<UiState> = combine(
        nowFlow,
        _selectedDirection,
        settingsRepo.settings,
    ) { now, direction, settings ->
        buildState(now, direction, settings)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), UiState.Loading)

    fun selectDirection(direction: DirectionKey) {
        _selectedDirection.value = direction
    }

    fun updateMinutesToTokyoStation(minutes: Int) {
        viewModelScope.launch { settingsRepo.setMinutesToTokyo(minutes) }
    }

    fun updateMinutesToTochigiStation(minutes: Int) {
        viewModelScope.launch { settingsRepo.setMinutesToTochigi(minutes) }
    }

    private fun buildState(
        now: LocalDateTime,
        directionKey: DirectionKey,
        settings: UserSettings,
    ): UiState {
        val direction = timetable.direction(directionKey)
        val isWeekend = now.dayOfWeek == DayOfWeek.SATURDAY || now.dayOfWeek == DayOfWeek.SUNDAY
        val offsetMinutes = settings.minutesToBoardingStationFor(directionKey)
        val effectiveNow = now.toLocalTime().plusMinutes(offsetMinutes.toLong())

        val upcoming = direction.trains
            .filter { it.departure.isAfter(effectiveNow) || it.departure == effectiveNow }
            .filter { it.hasFullJourney }
            .sortedBy { it.departure }

        return UiState.Ready(
            timetable = timetable,
            direction = direction,
            now = now,
            isWeekend = isWeekend,
            offsetMinutes = offsetMinutes,
            effectiveDepartureCutoff = effectiveNow,
            nextTrain = upcoming.firstOrNull(),
            followingTrains = upcoming.drop(1).take(3),
            settings = settings,
        )
    }
}

sealed interface UiState {
    data object Loading : UiState
    data class Ready(
        val timetable: Timetable,
        val direction: com.example.timetable.data.Direction,
        val now: LocalDateTime,
        val isWeekend: Boolean,
        val offsetMinutes: Int,
        val effectiveDepartureCutoff: LocalTime,
        val nextTrain: Train?,
        val followingTrains: List<Train>,
        val settings: UserSettings,
    ) : UiState
}
