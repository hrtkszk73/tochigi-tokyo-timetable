package com.example.timetable

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.timetable.ui.MainScreen
import com.example.timetable.ui.TimetableAppTheme
import com.example.timetable.ui.TimetableViewModel
import com.example.timetable.ui.UiState

class MainActivity : ComponentActivity() {

    private val viewModel: TimetableViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TimetableAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val state by viewModel.uiState.collectAsState()
                    when (val s = state) {
                        UiState.Loading -> Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("読み込み中…")
                        }
                        is UiState.Ready -> MainScreen(
                            state = s,
                            onSelectTab = viewModel::selectTab,
                            onSelectTimetableDirection = viewModel::selectTimetableDirection,
                            onUpdateMinutesToTokyo = viewModel::updateMinutesToTokyoStation,
                            onUpdateMinutesToTochigi = viewModel::updateMinutesToTochigiStation,
                        )
                    }
                }
            }
        }
    }
}
