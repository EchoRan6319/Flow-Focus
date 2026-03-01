package com.echoran.flowfocus.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.echoran.flowfocus.R

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    taskId: Long? = null,
    onExit: () -> Unit = {},
    viewModel: TimerViewModel = hiltViewModel(),
    whiteNoiseViewModel: WhiteNoiseViewModel = hiltViewModel()
) {
    val timeRemaining by viewModel.timeRemaining.collectAsState()
    val totalTime by viewModel.totalTime.collectAsState()
    val timerState by viewModel.timerState.collectAsState()
    val timerMode by viewModel.timerMode.collectAsState()
    val activeTaskName by viewModel.activeTaskName.collectAsState()
    
    val currentTrack by whiteNoiseViewModel.currentTrack.collectAsState()
    val isPlaying by whiteNoiseViewModel.isPlaying.collectAsState()
    val availableTracks by whiteNoiseViewModel.availableTracks.collectAsState()

    var showNoiseSheet by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            if (event is TimerViewModel.TimerNavigationEvent.Exit) {
                onExit()
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Task Title
        activeTaskName?.let {
            Text(
                text = "正在专注：$it",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Timer Display
        TimerDisplay(timeRemaining = timeRemaining, totalTime = totalTime)

        Spacer(modifier = Modifier.height(48.dp))

        // Controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { viewModel.toggleTimer() },
                shape = CircleShape,
                modifier = Modifier.size(80.dp)
            ) {
                Text(
                    text = if (timerState == TimerState.RUNNING) "暂停" else "开始",
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (timerState != TimerState.IDLE) {
                Button(
                    onClick = { viewModel.stopTimer() },
                    shape = CircleShape,
                    modifier = Modifier.size(64.dp)
                ) {
                    Text("停止")
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))

        // White Noise Button
        Button(
            onClick = { showNoiseSheet = true },
            modifier = Modifier.padding(16.dp),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Icon(
                if (isPlaying) Icons.Filled.Warning else Icons.Filled.Info, // Placeholder Icons
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(currentTrack?.name ?: "白噪音: 未开启")
        }

    }

    if (showNoiseSheet) {
        ModalBottomSheet(
            onDismissRequest = { showNoiseSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "选择白噪音",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                availableTracks.forEach { track ->
                    val isSelected = currentTrack == track
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                whiteNoiseViewModel.playTrack(track)
                            }
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = track.name,
                            fontSize = 18.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        if (isSelected && isPlaying) {
                            Text("播放中", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                        } else if (isSelected) {
                            Text("已暂停", color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp)
                        }
                    }
                }

                if (currentTrack != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { whiteNoiseViewModel.stop() },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text("关闭白噪音")
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun TimerDisplay(timeRemaining: Long, totalTime: Long) {
    val progress = if (totalTime > 0) timeRemaining.toFloat() / totalTime else 1f
    
    val minutes = timeRemaining / 60
    val seconds = timeRemaining % 60
    val timeString = String.format("%02d:%02d", minutes, seconds)

    androidx.compose.foundation.layout.Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(240.dp)
    ) {
        // Circular Progress
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxSize(),
            strokeWidth = 8.dp,
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )

        // Time Text
        Text(
            text = timeString,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
