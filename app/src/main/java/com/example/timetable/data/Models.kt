package com.example.timetable.data

import java.time.LocalTime

enum class DirectionKey(val jsonKey: String) {
    ToTokyo("toTokyo"),
    ToTochigi("toTochigi"),
}

data class Train(
    val departure: LocalTime,
    val transferArrival: LocalTime?,
    val transferDeparture: LocalTime?,
    val arrival: LocalTime?,
) {
    val hasFullJourney: Boolean
        get() = transferDeparture != null && arrival != null
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
