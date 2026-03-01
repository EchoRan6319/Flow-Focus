package com.echoran.flowfocus.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.echoran.flowfocus.data.model.TaskEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    viewModel: TaskViewModel = hiltViewModel(),
    onNavigateToTimer: (Long) -> Unit // Optional navigation callback
) {
    val tasks by viewModel.allTasks.collectAsState(initial = emptyList())
    var showAddTaskDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text("待办清单", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            
            // Re-using the same spacing as Statistics/Settings
            Spacer(modifier = Modifier.height(16.dp))

            if (tasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无任务，点击下方按钮添加", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(tasks) { task ->
                        TaskItem(
                            task = task,
                            onDelete = { viewModel.deleteTask(task) },
                            onClick = { onNavigateToTimer(task.id) }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddTaskDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "添加任务")
        }

        if (showAddTaskDialog) {
            AddTaskDialog(
                onDismiss = { showAddTaskDialog = false },
                onAdd = { title, desc ->
                    viewModel.addTask(title, desc)
                    showAddTaskDialog = false
                }
            )
        }
    }
}

@Composable
fun TaskItem(task: TaskEntity, onDelete: () -> Unit, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = task.title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                if (task.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = task.description, fontSize = 14.sp, color = Color.Gray)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "删除任务", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加新任务") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("任务标题") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("任务描述 (可选)") },
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank()) onAdd(title, description) },
                enabled = title.isNotBlank()
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
