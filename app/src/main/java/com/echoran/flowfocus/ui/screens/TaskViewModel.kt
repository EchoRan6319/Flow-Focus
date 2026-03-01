package com.echoran.flowfocus.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echoran.flowfocus.data.model.TaskEntity
import com.echoran.flowfocus.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    val allTasks: Flow<List<TaskEntity>> = taskRepository.getAllTasks()

    fun addTask(title: String, description: String, timerMode: String = "POMODORO", pomodoroDuration: Int = 25) {
        viewModelScope.launch {
            // Get current tasks to determine new position
            val currentTasks = allTasks.firstOrNull() ?: emptyList()
            val nextPosition = if (currentTasks.isEmpty()) 0 else currentTasks.maxOf { it.position } + 1
            
            val newTask = TaskEntity(
                title = title,
                description = description,
                timerMode = timerMode,
                pomodoroDuration = pomodoroDuration,
                position = nextPosition,
                createdAt = System.currentTimeMillis()
            )
            taskRepository.insertTask(newTask)
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.deleteTask(task)
        }
    }

    fun moveTaskUp(task: TaskEntity, tasks: List<TaskEntity>) {
        val index = tasks.indexOf(task)
        if (index > 0) {
            val previewTask = tasks[index - 1]
            viewModelScope.launch {
                taskRepository.updateTaskPosition(task.id, previewTask.position)
                taskRepository.updateTaskPosition(previewTask.id, task.position)
            }
        }
    }

    fun moveTaskDown(task: TaskEntity, tasks: List<TaskEntity>) {
        val index = tasks.indexOf(task)
        if (index < tasks.size - 1) {
            val nextTask = tasks[index + 1]
            viewModelScope.launch {
                taskRepository.updateTaskPosition(task.id, nextTask.position)
                taskRepository.updateTaskPosition(nextTask.id, task.position)
            }
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.updateTask(task)
        }
    }
}
