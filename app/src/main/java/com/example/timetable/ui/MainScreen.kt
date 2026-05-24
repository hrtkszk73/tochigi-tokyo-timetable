package com.example.timetable.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.timetable.data.Direction
import com.example.timetable.data.DirectionKey
import com.example.timetable.data.Train
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    state: UiState.Ready,
    onSelectDirection: (DirectionKey) -> Unit,
    onUpdateMinutesToTokyo: (Int) -> Unit,
    onUpdateMinutesToTochigi: (Int) -> Unit,
) {
    var showSettings by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("栃木⇔東京 時刻表") },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "設定")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            DirectionTabs(
                selected = state.direction.key,
                onSelect = onSelectDirection,
            )

            if (state.isWeekend) {
                WeekendWarning()
            } else {
                Content(state = state)
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            settings = state.settings,
            onDismiss = { showSettings = false },
            onMinutesToTokyoChange = onUpdateMinutesToTokyo,
            onMinutesToTochigiChange = onUpdateMinutesToTochigi,
        )
    }
}

@Composable
private fun DirectionTabs(
    selected: DirectionKey,
    onSelect: (DirectionKey) -> Unit,
) {
    val tabs = listOf(
        DirectionKey.ToTokyo to "東京へ行く",
        DirectionKey.ToTochigi to "栃木へ帰る",
    )
    val selectedIndex = tabs.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    TabRow(selectedTabIndex = selectedIndex) {
        tabs.forEachIndexed { index, (key, label) ->
            Tab(
                selected = index == selectedIndex,
                onClick = { onSelect(key) },
                text = { Text(label) },
            )
        }
    }
}

@Composable
private fun Content(state: UiState.Ready) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ClockAndOffsetInfo(
                now = state.now,
                offsetMinutes = state.offsetMinutes,
                effectiveDeparture = state.effectiveDepartureCutoff,
                direction = state.direction,
            )
        }

        item {
            if (state.nextTrain != null) {
                NextTrainCard(
                    train = state.nextTrain,
                    direction = state.direction,
                    now = state.now,
                )
            } else {
                NoTrainCard()
            }
        }

        if (state.followingTrains.isNotEmpty()) {
            item {
                Text(
                    text = "以降の便",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                )
            }
            items(state.followingTrains) { train ->
                UpcomingTrainCard(train = train, direction = state.direction)
            }
        }

        item {
            NoteCard(notes = state.timetable.notes, validFrom = state.timetable.validFrom)
        }
    }
}

@Composable
private fun ClockAndOffsetInfo(
    now: LocalDateTime,
    offsetMinutes: Int,
    effectiveDeparture: LocalTime,
    direction: Direction,
) {
    val timeFmt = remember { DateTimeFormatter.ofPattern("HH:mm:ss") }
    val hhmm = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val boardingStation = direction.stations.first()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "現在 ${now.toLocalTime().format(timeFmt)}",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${boardingStation}まで${offsetMinutes}分 → 到着予想 ${effectiveDeparture.format(hhmm)} 以降の便を表示",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NextTrainCard(
    train: Train,
    direction: Direction,
    now: LocalDateTime,
) {
    val hhmm = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val minutesUntil = java.time.Duration.between(now.toLocalTime(), train.departure).toMinutes()
    val countdown = when {
        minutesUntil <= 0 -> "まもなく発車"
        minutesUntil < 60 -> "あと ${minutesUntil} 分"
        else -> "あと ${minutesUntil / 60} 時間 ${minutesUntil % 60} 分"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "次の便",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = train.departure.format(hhmm),
                    style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = countdown,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.height(16.dp))
            JourneyTimeline(train = train, direction = direction, emphasize = true)
        }
    }
}

@Composable
private fun UpcomingTrainCard(train: Train, direction: Direction) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            JourneyTimeline(train = train, direction = direction, emphasize = false)
        }
    }
}

@Composable
private fun JourneyTimeline(
    train: Train,
    direction: Direction,
    emphasize: Boolean,
) {
    val hhmm = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val nullStr = "—"
    val rows = listOf(
        direction.stations[0] to train.departure.format(hhmm),
        direction.stations[1] to (train.transferArrival?.format(hhmm) ?: nullStr),
        direction.stations[2] to (train.transferDeparture?.format(hhmm) ?: nullStr),
        direction.stations[3] to (train.arrival?.format(hhmm) ?: nullStr),
    )
    val segmentLabels = direction.segmentLabels

    Column {
        rows.forEachIndexed { index, (label, time) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(96.dp),
                )
                Text(
                    text = time,
                    style = if (emphasize && (index == 0 || index == rows.lastIndex)) {
                        MaterialTheme.typography.titleMedium
                    } else {
                        MaterialTheme.typography.bodyLarge
                    },
                    fontWeight = if (index == 0 || index == rows.lastIndex) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
            if (index < rows.lastIndex) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(20.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant),
                    )
                    Spacer(Modifier.width(18.dp))
                    Text(
                        text = segmentLabels[index],
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun NoTrainCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "本日の運行は終了しています",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "翌朝の便は明日になってから表示されます。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WeekendWarning() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Text(
                text = "土日祝のダイヤは未対応",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "このアプリは平日ダイヤのみ収録しています。土曜・日曜・祝日は実際のダイヤと異なるため、表示を停止しています。",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NoteCard(notes: List<String>, validFrom: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "データについて ($validFrom 時点)",
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(6.dp))
            notes.forEach { note ->
                Text(
                    text = "・$note",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
