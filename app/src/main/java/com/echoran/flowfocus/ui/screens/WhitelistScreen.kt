package com.echoran.flowfocus.ui.screens

import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhitelistScreen(
    viewModel: WhitelistViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val installedApps by viewModel.installedApps.collectAsState()
    val whitelistedApps by viewModel.whitelistedApps.collectAsState(initial = emptyList())

    LaunchedEffect(Unit) {
        viewModel.loadInstalledApps(context.packageManager)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("应用白名单 (严格模式)", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Text(
                "在专注期间，仅允许打开以下选中的应用。未选中的应用将被打断。",
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(installedApps) { app ->
                    val isWhitelisted = whitelistedApps.any { it.packageName == app.packageName }
                    AppListItem(
                        appInfo = app,
                        isWhitelisted = isWhitelisted,
                        onCheckedChange = { isChecked ->
                            viewModel.toggleAppWhitelist(app, isChecked)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AppListItem(
    appInfo: InstalledAppInfo,
    isWhitelisted: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = appInfo.appName, fontWeight = FontWeight.SemiBold)
            Text(text = appInfo.packageName, style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = isWhitelisted,
            onCheckedChange = onCheckedChange
        )
    }
}
