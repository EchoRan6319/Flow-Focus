package com.echoran.flowfocus.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.echoran.flowfocus.data.model.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY position ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("UPDATE tasks SET position = :position WHERE id = :id")
    suspend fun updateTaskPosition(id: Long, position: Int)

    @Insert
    suspend fun insert(task: TaskEntity)

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)
}
