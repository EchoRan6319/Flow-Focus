package com.echoran.flowfocus.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.echoran.flowfocus.data.model.FocusSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {
    @Query("SELECT * FROM focus_sessions WHERE startTime >= :start AND startTime <= :end ORDER BY startTime DESC")
    fun getSessionsInRange(start: Long, end: Long): Flow<List<FocusSessionEntity>>

    @Insert
    suspend fun insert(session: FocusSessionEntity)
}
