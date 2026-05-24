package com.example.timetable.data

import java.time.LocalTime

enum class DirectionKey(val jsonKey: String) {
    ToTokyo("toTokyo"),
    ToTochigi("toTochigi"),
    TobuToAsakusa("tobuToAsakusa"),
    TobuToTochigi("tobuToTochigi"),
}

data class Train(
    val stopTimes: List<LocalTime?>,
) {
    val departure: LocalTime get() = stopTimes.first()!!
    val arrival: LocalTime? get() = stopTimes.last()
    val hasFullJourney: Boolean get() = stopTimes.all { it != null }
}

data class Direction(
    val key: DirectionKey,
    val label: String,
    val stations: List<String>,
    val segmentLabels: List<String>,
    val trains: List<Train>,
)

data class Timetable(
    val notes: List<String>,
    val validFrom: String,
    val source: String,
    val byDirection: Map<DirectionKey, Direction>,
) {
    fun direction(key: DirectionKey): Direction = byDirection.getValue(key)
}
