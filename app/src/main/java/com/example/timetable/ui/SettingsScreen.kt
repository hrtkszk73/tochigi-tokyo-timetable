package com.example.timetable.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.timetable.data.UserSettings

@Composable
fun SettingsDialog(
    settings: UserSettings,
    onDismiss: () -> Unit,
    onMinutesToTokyoChange: (Int) -> Unit,
    onMinutesToTochigiChange: (Int) -> Unit,
) {
    var tokyoMinutes by remember { mutableFloatStateOf(settings.minutesToTokyoStation.toFloat()) }
    var tochigiMinutes by remember { mutableFloatStateOf(settings.minutesToTochigiStation.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("駅までの所要時間") },
        text = {
            Column {
                Text(
                    text = "現在地から駅まで何分かかるかを設定します。アプリはこの時間を足してから「次に乗れる電車」を判定します。",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(16.dp))

                MinuteSlider(
                    label = "東京駅まで",
                    minutes = tokyoMinutes,
                    onChange = { tokyoMinutes = it },
                )
                Spacer(Modifier.height(12.dp))
                MinuteSlider(
                    label = "栃木駅まで",
                    minutes = tochigiMinutes,
                    onChange = { tochigiMinutes = it },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onMinutesToTokyoChange(tokyoMinutes.toInt())
                onMinutesToTochigiChange(tochigiMinutes.toInt())
                onDismiss()
            }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        },
    )
}

@Composable
private fun MinuteSlider(
    label: String,
    minutes: Float,
    onChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(text = "${minutes.toInt()} 分", style = MaterialTheme.typography.bodyMedium)
        }
        Slider(
            value = minutes,
            onValueChange = onChange,
            valueRange = 0f..120f,
            steps = 0,
        )
    }
}
