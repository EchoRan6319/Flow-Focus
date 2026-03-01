package com.echoran.flowfocus.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val timerMode: String = "POMODORO", // "POMODORO" or "STOPWATCH"
    val pomodoroDuration: Int = 25, // in minutes
    val position: Int = 0, // for manual sorting
    val createdAt: Long = System.currentTimeMillis()
)
