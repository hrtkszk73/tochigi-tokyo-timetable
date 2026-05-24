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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.timetable.data.Direction
import com.example.timetable.data.DirectionKey
import com.example.timetable.data.Train
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    state: UiState.Ready,
    onSelectTab: (TopTab) -> Unit,
    onSelectTimetableDirection: (DirectionKey) -> Unit,
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
            TopTabs(
                selected = state.selectedTab,
                onSelect = onSelectTab,
            )

            when (val content = state.content) {
                is TabContent.Next -> NextTrainsContent(
                    content = content,
                    notes = state.timetable.notes,
                    validFrom = state.timetable.validFrom,
                    now = state.now,
                )
                is TabContent.FullTimetable -> FullTimetableContent(
                    content = content,
                    notes = state.timetable.notes,
                    validFrom = state.timetable.validFrom,
                    selectedDirection = state.timetableDirection,
                    onSelectDirection = onSelectTimetableDirection,
                )
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
private fun TopTabs(
    selected: TopTab,
    onSelect: (TopTab) -> Unit,
) {
    val tabs = TopTab.values().toList()
    val selectedIndex = tabs.indexOf(selected).coerceAtLeast(0)
    TabRow(selectedTabIndex = selectedIndex) {
        tabs.forEachIndexed { index, tab ->
            Tab(
                selected = index == selectedIndex,
                onClick = { onSelect(tab) },
                text = { Text(tab.title) },
            )
        }
    }
}

// =================== Next trains tab ===================

@Composable
private fun NextTrainsContent(
    content: TabContent.Next,
    notes: List<String>,
    validFrom: String,
    now: LocalDateTime,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ClockAndOffsetInfo(
                now = now,
                offsetMinutes = content.offsetMinutes,
                direction = content.direction,
            )
        }

        item {
            if (content.nextTrain != null) {
                NextTrainCard(
                    train = content.nextTrain,
                    direction = content.direction,
                    now = now,
                )
            } else {
                NoTrainCard()
            }
        }

        if (content.followingTrains.isNotEmpty()) {
            item {
                Text(
                    text = "以降の便",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                )
            }
            items(content.followingTrains) { train ->
                UpcomingTrainCard(train = train, direction = content.direction)
            }
        }

        item {
            NoteCard(notes = notes, validFrom = validFrom)
        }
    }
}

@Composable
private fun ClockAndOffsetInfo(
    now: LocalDateTime,
    offsetMinutes: Int,
    direction: Direction,
) {
    val timeFmt = remember { DateTimeFormatter.ofPattern("HH:mm:ss") }
    val hhmm = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val boardingStation = direction.stations.first()
    val effective = now.toLocalTime().plusMinutes(offsetMinutes.toLong())
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
                text = "${boardingStation}まで${offsetMinutes}分 → 到着予想 ${effective.format(hhmm)} 以降の便を表示",
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
    val rows = train.stopTimes.mapIndexed { idx, time ->
        direction.stations[idx] to (time?.format(hhmm) ?: nullStr)
    }
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
            if (index < rows.lastIndex && index < segmentLabels.size) {
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

// =================== Timetable tab ===================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullTimetableContent(
    content: TabContent.FullTimetable,
    notes: List<String>,
    validFrom: String,
    selectedDirection: DirectionKey,
    onSelectDirection: (DirectionKey) -> Unit,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(selectedDirection, content.highlightedIndex) {
        val target = content.highlightedIndex
        if (target != null) {
            listState.scrollToItem(index = (target + 1).coerceAtMost(content.trains.size))
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TimetableDirectionSelector(
            selected = selectedDirection,
            onSelect = onSelectDirection,
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                TimetableHeaderRow(direction = content.direction)
                HorizontalDivider()
            }

            itemsIndexed(content.trains) { index, train ->
                TimetableRow(
                    train = train,
                    highlighted = index == content.highlightedIndex,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            item {
                Spacer(Modifier.height(16.dp))
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    NoteCard(notes = notes, validFrom = validFrom)
                }
            }
        }
    }
}

@Composable
private fun TimetableDirectionSelector(
    selected: DirectionKey,
    onSelect: (DirectionKey) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DirectionSegmentRow(
            label = "新幹線",
            options = listOf(
                DirectionKey.ToTokyo to "栃木→東京",
                DirectionKey.ToTochigi to "東京→栃木",
            ),
            selected = selected,
            onSelect = onSelect,
        )
        DirectionSegmentRow(
            label = "特急",
            options = listOf(
                DirectionKey.TobuToAsakusa to "栃木→浅草",
                DirectionKey.TobuToTochigi to "浅草→栃木",
            ),
            selected = selected,
            onSelect = onSelect,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DirectionSegmentRow(
    label: String,
    options: List<Pair<DirectionKey, String>>,
    selected: DirectionKey,
    onSelect: (DirectionKey) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            modifier = Modifier.width(56.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
            options.forEachIndexed { index, (key, optLabel) ->
                SegmentedButton(
                    selected = key == selected,
                    onClick = { onSelect(key) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                ) { Text(optLabel) }
            }
        }
    }
}

@Composable
private fun TimetableHeaderRow(direction: Direction) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        direction.stations.forEach { name ->
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .weight(1f)
                    .wrapContentWidth(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun TimetableRow(
    train: Train,
    highlighted: Boolean,
) {
    val hhmm = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val rowBg = if (highlighted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val textColor = if (highlighted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val nullStr = "—"
    val cells = train.stopTimes.map { it?.format(hhmm) ?: nullStr }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBg)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        cells.forEach { time ->
            Text(
                text = time,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (highlighted) FontWeight.SemiBold else FontWeight.Normal,
                color = textColor,
                modifier = Modifier
                    .weight(1f)
                    .wrapContentWidth(Alignment.CenterHorizontally),
            )
        }
    }
}
