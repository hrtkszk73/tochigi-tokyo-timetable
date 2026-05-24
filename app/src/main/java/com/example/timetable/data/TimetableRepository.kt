package com.example.timetable.data

import android.content.Context
import org.json.JSONObject
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class TimetableRepository(private val context: Context) {

    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun load(): Timetable {
        val raw = context.assets.open("timetable.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        val root = JSONObject(raw)

        val notes = root.getJSONArray("notes").let { arr ->
            (0 until arr.length()).map { arr.getString(it) }
        }

        val byDirection = DirectionKey.values()
            .filter { root.has(it.jsonKey) }
            .associateWith { key -> parseDirection(key, root.getJSONObject(key.jsonKey)) }

        return Timetable(
            notes = notes,
            validFrom = root.optString("validFrom"),
            source = root.optString("source"),
            byDirection = byDirection,
        )
    }

    private fun parseDirection(key: DirectionKey, obj: JSONObject): Direction {
        val stations = obj.getJSONArray("stations").let { arr ->
            (0 until arr.length()).map { arr.getString(it) }
        }
        val segmentLabels = obj.getJSONArray("segmentLabels").let { arr ->
            (0 until arr.length()).map { arr.getString(it) }
        }
        val trainsArr = obj.getJSONArray("trains")
        val trains = (0 until trainsArr.length()).map { i ->
            val t = trainsArr.getJSONObject(i)
            val stopTimesArr = t.getJSONArray("stopTimes")
            val stopTimes = (0 until stopTimesArr.length()).map { j ->
                parseTime(stopTimesArr.optString(j, null))
            }
            Train(stopTimes = stopTimes)
        }
        return Direction(
            key = key,
            label = obj.getString("label"),
            stations = stations,
            segmentLabels = segmentLabels,
            trains = trains,
        )
    }

    private fun parseTime(s: String?): LocalTime? {
        if (s.isNullOrBlank() || s == "-" || s == "null") return null
        return LocalTime.parse(s, timeFormatter)
    }
}
