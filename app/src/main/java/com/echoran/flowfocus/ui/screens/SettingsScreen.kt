package com.echoran.flowfocus.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import com.echoran.flowfocus.data.repository.SettingsRepository
import com.echoran.flowfocus.service.NotificationBlockerService
import com.echoran.flowfocus.service.StrictModeAccessibilityService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateToWhitelist: () -> Unit
) {
    val settingsRepository = viewModel.repository
    val scope = rememberCoroutineScope()
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri: android.net.Uri? ->
        uri?.let {
            // Persist permission for cross-reboot access
            try {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {
                // Ignore if not supported or already taken
            }
            scope.launch { settingsRepository.addCustomWhiteNoise(it.toString()) }
        }
    }

    val webDavUrl by settingsRepository.webDavServerUrl.collectAsState(initial = "")
    val webDavUser by settingsRepository.webDavUsername.collectAsState(initial = "")
    val webDavPass by settingsRepository.webDavPassword.collectAsState(initial = "")
    val webDavEnabled by settingsRepository.isWebDavSyncEnabled.collectAsState(initial = false)

    val isStrictModeEnabled by settingsRepository.isStrictModeEnabled.collectAsState(initial = false)
    val isNotificationBlockingEnabled by settingsRepository.isNotificationBlockingEnabled.collectAsState(initial = false)

    var showAccessibilityDialog by remember { mutableStateOf(false) }
    var showOverlayDialog by remember { mutableStateOf(false) }
    var showNotificationServiceDialog by remember { mutableStateOf(false) }

    if (showAccessibilityDialog) {
        AlertDialog(
            onDismissRequest = { showAccessibilityDialog = false },
            title = { Text("需要无障碍权限") },
            text = { Text("严格模式需要无障碍权限来检测当前运行的应用。当您尝试打开非白名单应用时，我们会通过该权限通过系统动作将您带回专注界面。") },
            confirmButton = {
                Button(onClick = { 
                    showAccessibilityDialog = false
                    viewModel.openAccessibilitySettings() 
                }) { Text("前往开启") }
            },
            dismissButton = { TextButton(onClick = { showAccessibilityDialog = false }) { Text("取消") } }
        )
    }

    if (showOverlayDialog) {
        AlertDialog(
            onDismissRequest = { showOverlayDialog = false },
            title = { Text("需要悬浮窗权限") },
            text = { Text("严格模式需要悬浮窗权限来显示拦截提示和倒计时。如果不开启，您在被拦截时将无法看到反馈信息。") },
            confirmButton = {
                Button(onClick = { 
                    showOverlayDialog = false
                    viewModel.openOverlaySettings() 
                }) { Text("前往开启") }
            },
            dismissButton = { TextButton(onClick = { showOverlayDialog = false }) { Text("取消") } }
        )
    }

    if (showNotificationServiceDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationServiceDialog = false },
            title = { Text("需要通知监听权限") },
            text = { Text("通知拦截功能需要通知监听权限来读取并自动屏蔽来自非白名单应用的提醒，确保您的专注过程不受干扰。") },
            confirmButton = {
                Button(onClick = { 
                    showNotificationServiceDialog = false
                    viewModel.openNotificationSettings() 
                }) { Text("前往开启") }
            },
            dismissButton = { TextButton(onClick = { showNotificationServiceDialog = false }) { Text("取消") } }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
    ) {
        Text("设置", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        
        Spacer(modifier = Modifier.height(16.dp))

        // Focus Enhancement Section
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("专注增强", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("严格模式")
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(checked = isStrictModeEnabled, onCheckedChange = { 
                        if (it) {
                            if (!viewModel.isAccessibilityServiceEnabled()) {
                                showAccessibilityDialog = true
                            } else if (!viewModel.canDrawOverlays()) {
                                showOverlayDialog = true
                            } else {
                                viewModel.setStrictModeEnabled(true)
                            }
                        } else {
                            viewModel.setStrictModeEnabled(false)
                        }
                    })
                }
                Text("开启后，在专注期间打开非白名单应用将自动返回本应用。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("通知拦截")
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(checked = isNotificationBlockingEnabled, onCheckedChange = { 
                        if (it) {
                            if (!viewModel.isNotificationServiceEnabled()) {
                                showNotificationServiceDialog = true
                            } else {
                                viewModel.setNotificationBlockingEnabled(true)
                            }
                        } else {
                            viewModel.setNotificationBlockingEnabled(false)
                        }
                    })
                }
                Text("开启后，在专注期间将自动拦截非白名单应用的通知。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onNavigateToWhitelist,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                ) {
                    Text("管理应用白名单")
                }
            }
        }

        // Custom White Noise Section
        val customTracks by settingsRepository.customWhiteNoiseTracks.collectAsState(initial = emptySet())
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("自定义白噪音", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                
                customTracks.forEach { trackUri ->
                    val fileName = trackUri.split("/").last()
                    Text(fileName, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 4.dp))
                }

                Button(
                    onClick = { launcher.launch(arrayOf("audio/*")) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer)
                ) {
                    Text("导入本地音频")
                }
            }
        }

        // WebDAV Section
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("WebDAV 配置 (坚果云)", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                OutlinedTextField(value = webDavUrl, onValueChange = { scope.launch { settingsRepository.updateWebDavSettings(it, webDavUser, webDavPass, webDavEnabled) } }, label = { Text("服务器地址") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = webDavUser, onValueChange = { scope.launch { settingsRepository.updateWebDavSettings(webDavUrl, it, webDavPass, webDavEnabled) } }, label = { Text("账号") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = webDavPass, onValueChange = { scope.launch { settingsRepository.updateWebDavSettings(webDavUrl, webDavUser, it, webDavEnabled) } }, label = { Text("应用密码") }, modifier = Modifier.fillMaxWidth())
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Text("启用自动备份 (坚果云)")
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(checked = webDavEnabled, onCheckedChange = { enabled ->
                        scope.launch { settingsRepository.updateWebDavSettings(webDavUrl, webDavUser, webDavPass, enabled) }
                    })
                }
            }
        }
    }
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    val repository: com.echoran.flowfocus.data.repository.SettingsRepository
) : androidx.lifecycle.ViewModel() {

    fun setStrictModeEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setStrictModeEnabled(enabled) }
    }

    fun setNotificationBlockingEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setNotificationBlockingEnabled(enabled) }
    }

    fun isAccessibilityServiceEnabled(): Boolean {
        val expectedName = context.packageName + "/" + StrictModeAccessibilityService::class.java.name
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return enabled?.contains(expectedName) == true
    }

    fun canDrawOverlays(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun isNotificationServiceEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        val componentName = context.packageName + "/" + NotificationBlockerService::class.java.name
        return enabledListeners?.contains(componentName) == true
    }

    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun openOverlaySettings() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            data = android.net.Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun openNotificationSettings() {
        val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
