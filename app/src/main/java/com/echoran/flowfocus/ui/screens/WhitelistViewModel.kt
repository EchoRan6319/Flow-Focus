package com.echoran.flowfocus.ui.screens

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echoran.flowfocus.data.model.WhitelistedAppEntity
import com.echoran.flowfocus.data.repository.WhitelistedAppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val isWhitelisted: Boolean
)

@HiltViewModel
class WhitelistViewModel @Inject constructor(
    private val repository: WhitelistedAppRepository
) : ViewModel() {

    val whitelistedApps: Flow<List<WhitelistedAppEntity>> = repository.getAllWhitelistedApps()

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()

    fun loadInstalledApps(packageManager: PackageManager) {
        viewModelScope.launch {
            val installedPackages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            val currentWhitelist = repository.getAllWhitelistedApps() // Needs flow collection to sync properly
            
            val appsList = installedPackages.mapNotNull { appInfo ->
                // Filter out system apps for simplicity unless desired
                if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0) {
                    null
                } else {
                    val appName = packageManager.getApplicationLabel(appInfo).toString()
                    val packageName = appInfo.packageName
                    // Optimization required here: Need real-time sync with Flow
                    InstalledAppInfo(packageName, appName, false)
                }
            }.sortedBy { it.appName }

            _installedApps.value = appsList
        }
    }

    fun toggleAppWhitelist(app: InstalledAppInfo, isWhitelisted: Boolean) {
        viewModelScope.launch {
            val entity = WhitelistedAppEntity(
                packageName = app.packageName,
                appName = app.appName,
                addedAt = System.currentTimeMillis()
            )
            if (isWhitelisted) {
                repository.addAppToWhitelist(entity)
            } else {
                repository.removeAppFromWhitelist(entity)
            }
            // Update local state temporarily, full implementation should observe DB Flow
            _installedApps.value = _installedApps.value.map {
                if (it.packageName == app.packageName) it.copy(isWhitelisted = isWhitelisted) else it
            }
        }
    }
}
