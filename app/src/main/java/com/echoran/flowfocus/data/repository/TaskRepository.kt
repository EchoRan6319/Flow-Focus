package com.echoran.flowfocus.data.repository

import com.echoran.flowfocus.data.dao.TaskDao
import com.echoran.flowfocus.data.model.TaskEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao
) {

    fun getAllTasks(): Flow<List<TaskEntity>> {
        return taskDao.getAllTasks()
    }

    suspend fun insertTask(task: TaskEntity) {
        taskDao.insert(task)
    }

    suspend fun updateTask(task: TaskEntity) {
        taskDao.update(task)
    }

    suspend fun deleteTask(task: TaskEntity) {
        taskDao.delete(task)
    }

    suspend fun updateTaskPosition(id: Long, position: Int) {
        taskDao.updateTaskPosition(id, position)
    }
}
