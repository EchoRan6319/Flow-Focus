package com.echoran.flowfocus.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.echoran.flowfocus.data.model.WhitelistedAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WhitelistedAppDao {
    @Query("SELECT * FROM whitelisted_apps ORDER BY appName ASC")
    fun getAllWhitelistedApps(): Flow<List<WhitelistedAppEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM whitelisted_apps WHERE packageName = :packageName)")
    suspend fun isAppWhitelisted(packageName: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: WhitelistedAppEntity)

    @Delete
    suspend fun delete(app: WhitelistedAppEntity)
}
