package com.echoran.flowfocus.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.echoran.flowfocus.R
import com.echoran.flowfocus.ui.screens.TimerMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TimerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null
    private var timeRemaining = 0L
    private var currentMode = TimerMode.POMODORO

    companion object {
        const val CHANNEL_ID = "TimerServiceChannel"
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val timeToStart = intent?.getLongExtra("TIME_REMAINING", 0L) ?: 0L
        val isStrictMode = intent?.getBooleanExtra("STRICT_MODE", false) ?: false
        val modeStr = intent?.getStringExtra("TIMER_MODE") ?: TimerMode.POMODORO.name
        currentMode = TimerMode.valueOf(modeStr)

        when (action) {
            "START" -> {
                StrictModeAccessibilityService.isSessionActive = isStrictMode
                startTimer(timeToStart)
            }
            "STOP" -> {
                StrictModeAccessibilityService.isSessionActive = false
                stopTimer()
            }
        }

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("心流番茄")
            .setContentText(if (currentMode == TimerMode.POMODORO) "专注中..." else "正计时中...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        
        startForeground(NOTIFICATION_ID, notification)
        
        return START_NOT_STICKY
    }

    private fun startTimer(initialTime: Long) {
        timerJob?.cancel()
        timeRemaining = initialTime
        timerJob = serviceScope.launch {
            while (true) {
                if (currentMode == TimerMode.POMODORO) {
                    if (timeRemaining > 0) {
                        delay(1000)
                        timeRemaining--
                    } else {
                        break
                    }
                } else {
                    // Stopwatch counts up
                    delay(1000)
                    timeRemaining++
                }
                updateNotification()
            }
            stopSelf()
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateNotification() {
        val minutes = timeRemaining / 60
        val seconds = timeRemaining % 60
        val timeString = String.format("%02d:%02d", minutes, seconds)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("心流番茄")
            .setContentText(if (currentMode == TimerMode.POMODORO) "专注中: $timeString" else "正计时: $timeString")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        StrictModeAccessibilityService.isSessionActive = false
        timerJob?.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Timer Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}
