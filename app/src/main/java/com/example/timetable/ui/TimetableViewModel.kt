package com.example.timetable.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.timetable.data.Direction
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime

enum class TopTab(val title: String) {
    NextToTokyo("東京へ行く"),
    NextToTochigi("栃木へ帰る"),
    Timetable("時刻表"),
}

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

    private val _selectedTab = MutableStateFlow(TopTab.NextToTokyo)
    private val _timetableDirection = MutableStateFlow(DirectionKey.ToTokyo)

    val uiState: StateFlow<UiState> = combine(
        nowFlow,
        _selectedTab,
        _timetableDirection,
        settingsRepo.settings,
    ) { now, tab, tableDir, settings ->
        buildState(now, tab, tableDir, settings)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), UiState.Loading)

    fun selectTab(tab: TopTab) {
        _selectedTab.value = tab
    }

    fun selectTimetableDirection(direction: DirectionKey) {
        _timetableDirection.value = direction
    }

    fun updateMinutesToTokyoStation(minutes: Int) {
        viewModelScope.launch { settingsRepo.setMinutesToTokyo(minutes) }
    }

    fun updateMinutesToTochigiStation(minutes: Int) {
        viewModelScope.launch { settingsRepo.setMinutesToTochigi(minutes) }
    }

    private fun buildState(
        now: LocalDateTime,
        tab: TopTab,
        timetableDirection: DirectionKey,
        settings: UserSettings,
    ): UiState {
        val content = when (tab) {
            TopTab.NextToTokyo -> buildNext(DirectionKey.ToTokyo, now, settings)
            TopTab.NextToTochigi -> buildNext(DirectionKey.ToTochigi, now, settings)
            TopTab.Timetable -> buildFullTimetable(timetableDirection, now)
        }

        return UiState.Ready(
            timetable = timetable,
            selectedTab = tab,
            timetableDirection = timetableDirection,
            now = now,
            settings = settings,
            content = content,
        )
    }

    private fun buildNext(
        directionKey: DirectionKey,
        now: LocalDateTime,
        settings: UserSettings,
    ): TabContent.Next {
        val direction = timetable.direction(directionKey)
        val offsetMinutes = settings.minutesToBoardingStationFor(directionKey)
        val effectiveNow = now.toLocalTime().plusMinutes(offsetMinutes.toLong())

        val upcoming = direction.trains
            .filter { it.departure.isAfter(effectiveNow) || it.departure == effectiveNow }
            .filter { it.hasFullJourney }
            .sortedBy { it.departure }

        return TabContent.Next(
            direction = direction,
            offsetMinutes = offsetMinutes,
            effectiveDepartureCutoff = effectiveNow,
            nextTrain = upcoming.firstOrNull(),
            followingTrains = upcoming.drop(1).take(3),
        )
    }

    private fun buildFullTimetable(
        directionKey: DirectionKey,
        now: LocalDateTime,
    ): TabContent.FullTimetable {
        val direction = timetable.direction(directionKey)
        val nowTime = now.toLocalTime()
        // 時刻表タブはオフセットを使わず、純粋に「現在時刻以降の最初の便」をハイライト
        val highlightedIndex = direction.trains
            .indexOfFirst { it.departure.isAfter(nowTime) || it.departure == nowTime }
            .takeIf { it >= 0 }

        return TabContent.FullTimetable(
            direction = direction,
            trains = direction.trains,
            highlightedIndex = highlightedIndex,
        )
    }
}

sealed interface TabContent {
    data class Next(
        val direction: Direction,
        val offsetMinutes: Int,
        val effectiveDepartureCutoff: LocalTime,
        val nextTrain: Train?,
        val followingTrains: List<Train>,
    ) : TabContent

    data class FullTimetable(
        val direction: Direction,
        val trains: List<Train>,
        val highlightedIndex: Int?,
    ) : TabContent
}

sealed interface UiState {
    data object Loading : UiState
    data class Ready(
        val timetable: Timetable,
        val selectedTab: TopTab,
        val timetableDirection: DirectionKey,
        val now: LocalDateTime,
        val settings: UserSettings,
        val content: TabContent,
    ) : UiState
}
