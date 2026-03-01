package com.echoran.flowfocus.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "whitelisted_apps")
data class WhitelistedAppEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val addedAt: Long
)
