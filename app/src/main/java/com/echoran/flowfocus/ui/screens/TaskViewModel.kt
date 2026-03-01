package com.echoran.flowfocus.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echoran.flowfocus.data.model.TaskEntity
import com.echoran.flowfocus.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    val allTasks: Flow<List<TaskEntity>> = taskRepository.getAllTasks()

    fun addTask(title: String, description: String) {
        viewModelScope.launch {
            val newTask = TaskEntity(
                title = title,
                description = description,
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
}
