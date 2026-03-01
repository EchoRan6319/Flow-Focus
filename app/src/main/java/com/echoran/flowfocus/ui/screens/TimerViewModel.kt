package com.echoran.flowfocus.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echoran.flowfocus.data.model.FocusSessionEntity
import com.echoran.flowfocus.data.repository.FocusSessionRepository
import com.echoran.flowfocus.service.TimerService
import com.echoran.flowfocus.service.StrictModeAccessibilityService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.echoran.flowfocus.data.repository.SettingsRepository

enum class TimerMode {
    POMODORO, STOPWATCH
}

enum class TimerState {
    IDLE, RUNNING, PAUSED
}

@HiltViewModel
class TimerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val focusSessionRepository: FocusSessionRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    private var sessionStartTime: Long = 0L

    private val _timeRemaining = MutableStateFlow(25 * 60L) // 25 mins default for Pomodoro
    val timeRemaining: StateFlow<Long> = _timeRemaining.asStateFlow()

    private val _timerState = MutableStateFlow(TimerState.IDLE)
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private val _timerMode = MutableStateFlow(TimerMode.POMODORO)
    val timerMode: StateFlow<TimerMode> = _timerMode.asStateFlow()

    val isStrictModeEnabled = settingsRepository.isStrictModeEnabled

    private var timerJob: Job? = null

    fun toggleTimer() {
        when (_timerState.value) {
            TimerState.IDLE, TimerState.PAUSED -> startTimer()
            TimerState.RUNNING -> pauseTimer()
        }
    }

    private fun startTimer() {
        _timerState.value = TimerState.RUNNING
        sessionStartTime = System.currentTimeMillis()
        
        viewModelScope.launch {
            val isStrict = settingsRepository.isStrictModeEnabled.first()
            val currentMode = _timerMode.value
            // Start Foreground Service
            val intent = Intent(context, TimerService::class.java).apply {
                action = "START"
                putExtra("TIME_REMAINING", _timeRemaining.value)
                putExtra("STRICT_MODE", isStrict)
                putExtra("TIMER_MODE", currentMode.name)
            }
            context.startService(intent)
        }

        timerJob = viewModelScope.launch {
            while (_timerState.value == TimerState.RUNNING) {
                delay(1000L)
                if (_timerMode.value == TimerMode.POMODORO) {
                    if (_timeRemaining.value > 0) {
                        _timeRemaining.value -= 1
                    } else {
                        stopTimer()
                        // TODO: Handle Pomodoro finish (e.g. notify service, play sound)
                    }
                } else {
                    // Stopwatch mode counts up
                    _timeRemaining.value += 1
                }
            }
        }
    }

    private fun pauseTimer() {
        _timerState.value = TimerState.PAUSED
        timerJob?.cancel()

        // Stop Foreground Service
        val intent = Intent(context, TimerService::class.java).apply {
            action = "STOP"
        }
        context.startService(intent)
    }

    fun stopTimer() {
        if (_timerState.value != TimerState.IDLE) {
            recordSession()
        }
        _timerState.value = TimerState.IDLE
        timerJob?.cancel()
        resetTimeForCurrentMode()

        // Stop Foreground Service
        val intent = Intent(context, TimerService::class.java).apply {
            action = "STOP"
        }
        context.startService(intent)
    }

    fun switchMode(mode: TimerMode) {
        if (_timerState.value == TimerState.RUNNING) return // Prevent switch while running
        _timerMode.value = mode
        resetTimeForCurrentMode()
    }

    private fun resetTimeForCurrentMode() {
        _timeRemaining.value = if (_timerMode.value == TimerMode.POMODORO) 25 * 60L else 0L
    }

    private fun recordSession() {
        val endTime = System.currentTimeMillis()
        val durationMillis = endTime - sessionStartTime
        val actualDurationMinutes = (durationMillis / (1000 * 60)).toInt()

        // Rule 1: Minimum duration must be 5 minutes
        if (actualDurationMinutes < 5) {
            android.util.Log.d("TimerViewModel", "Session too short ($actualDurationMinutes min), not recording")
            return
        }

        // Rule 2: Pomodoro must reach the end
        if (_timerMode.value == TimerMode.POMODORO && _timeRemaining.value > 0) {
            android.util.Log.d("TimerViewModel", "Pomodoro interrupted, not recording")
            return
        }

        viewModelScope.launch {
            val isStrict = settingsRepository.isStrictModeEnabled.first()
            val session = FocusSessionEntity(
                startTime = sessionStartTime,
                endTime = endTime,
                durationMinutes = if (_timerMode.value == TimerMode.POMODORO) 25 else actualDurationMinutes,
                isStrict = isStrict,
                category = "专注" // Default category for now
            )
            focusSessionRepository.insertFocusSession(session)
        }
    }

    // Handled in Settings now
    // fun setStrictModeEnabled(enabled: Boolean) { ... }


    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_GENERIC)
        val expectedName = context.packageName + "/" + StrictModeAccessibilityService::class.java.name
        
        // This is a common way to check, though not 100% robust on all ROMs
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return enabled?.contains(expectedName) == true
    }

    private fun canDrawOverlays(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    private fun openOverlaySettings() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            data = android.net.Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
