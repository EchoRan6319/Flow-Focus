package com.echoran.flowfocus.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
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
    onNavigateToTimer: (Long) -> Unit
) {
    val tasks by viewModel.allTasks.collectAsState(initial = emptyList())
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showEditMenu by remember { mutableStateOf(false) }
    var selectedTask by remember { mutableStateOf<TaskEntity?>(null) }
    var showEditTaskDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            Text("待办清单", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            
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
                            onClick = { onNavigateToTimer(task.id) },
                            onLongClick = {
                                selectedTask = task
                                showEditMenu = true
                            }
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
                onAdd = { title, desc, mode, duration ->
                    viewModel.addTask(title, desc, mode, duration)
                    showAddTaskDialog = false
                }
            )
        }

        if (showEditTaskDialog && selectedTask != null) {
            EditTaskDialog(
                task = selectedTask!!,
                onDismiss = { showEditTaskDialog = false },
                onUpdate = { updatedTask ->
                    viewModel.updateTask(updatedTask)
                    showEditTaskDialog = false
                    showEditMenu = false
                }
            )
        }

        if (showEditMenu && selectedTask != null) {
            EditTaskMenu(
                task = selectedTask!!,
                tasks = tasks,
                onDismiss = { showEditMenu = false },
                onEdit = { showEditTaskDialog = true },
                onDelete = {
                    viewModel.deleteTask(selectedTask!!)
                    showEditMenu = false
                },
                onMoveUp = {
                    viewModel.moveTaskUp(selectedTask!!, tasks)
                    showEditMenu = false
                },
                onMoveDown = {
                    viewModel.moveTaskDown(selectedTask!!, tasks)
                    showEditMenu = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskItem(
    task: TaskEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onClick() },
                        onLongPress = { onLongClick() }
                    )
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = task.title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                if (task.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = task.description, fontSize = 14.sp, color = Color.Gray)
                }
                Spacer(modifier = Modifier.height(4.dp))
                val modeText = if (task.timerMode == "POMODORO") "番茄钟 (${task.pomodoroDuration}min)" else "正计时"
                Text(text = modeText, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }
            
            IconButton(onClick = onClick) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "开始专注", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(onDismiss: () -> Unit, onAdd: (String, String, String, Int) -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var timerMode by remember { mutableStateOf("POMODORO") }
    var duration by remember { mutableStateOf("25") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加新任务") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("任务标题") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("任务描述 (可选)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text("计时模式", style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = timerMode == "POMODORO",
                        onClick = { timerMode = "POMODORO" }
                    )
                    Text("番茄钟")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(
                        selected = timerMode == "STOPWATCH",
                        onClick = { timerMode = "STOPWATCH" }
                    )
                    Text("正计时")
                }

                if (timerMode == "POMODORO") {
                    OutlinedTextField(
                        value = duration,
                        onValueChange = { if (it.all { char -> char.isDigit() }) duration = it },
                        label = { Text("番茄时长 (分钟)") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (title.isNotBlank()) {
                        val durationInt = duration.toIntOrNull() ?: 25
                        onAdd(title, description, timerMode, durationInt)
                    }
                },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskMenu(
    task: TaskEntity,
    tasks: List<TaskEntity>,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑任务") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("选择操作:")
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onEdit,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("编辑待办事项")
                }
                
                Button(
                    onClick = onMoveUp,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = tasks.indexOf(task) > 0
                ) {
                    Text("上移")
                }
                
                Button(
                    onClick = onMoveDown,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = tasks.indexOf(task) < tasks.size - 1
                ) {
                    Text("下移")
                }
                
                Button(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text("删除待办事项")
                }
                
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        },
        dismissButton = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskDialog(
    task: TaskEntity,
    onDismiss: () -> Unit,
    onUpdate: (TaskEntity) -> Unit
) {
    var title by remember { mutableStateOf(task.title) }
    var description by remember { mutableStateOf(task.description) }
    var timerMode by remember { mutableStateOf(task.timerMode) }
    var duration by remember { mutableStateOf(task.pomodoroDuration.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑任务") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 任务基本信息
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("任务标题") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("任务描述 (可选)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // 时间设置
                Text("计时模式", style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = timerMode == "POMODORO",
                        onClick = { timerMode = "POMODORO" }
                    )
                    Text("番茄钟")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(
                        selected = timerMode == "STOPWATCH",
                        onClick = { timerMode = "STOPWATCH" }
                    )
                    Text("正计时")
                }

                if (timerMode == "POMODORO") {
                    OutlinedTextField(
                        value = duration,
                        onValueChange = { if (it.all { char -> char.isDigit() }) duration = it },
                        label = { Text("番茄时长 (分钟)") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (title.isNotBlank()) {
                        val durationInt = duration.toIntOrNull() ?: 25
                        val updatedTask = task.copy(
                            title = title,
                            description = description,
                            timerMode = timerMode,
                            pomodoroDuration = durationInt
                        )
                        onUpdate(updatedTask)
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
