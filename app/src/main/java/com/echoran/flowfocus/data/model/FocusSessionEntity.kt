package com.echoran.flowfocus.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long? = null,
    val startTime: Long,
    val endTime: Long,
    val durationMinutes: Int,
    val isStrict: Boolean = false,
    val category: String = "未分类"
)
