package com.echoran.flowfocus.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.echoran.flowfocus.data.repository.SettingsRepository
import com.echoran.flowfocus.data.repository.WhitelistedAppRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationBlockerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var whitelistRepository: WhitelistedAppRepository

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)

        val packageName = sbn.packageName
        
        // We use the same flag as StrictMode accessibility service
        if (!StrictModeAccessibilityService.isSessionActive) return

        serviceScope.launch {
            val isEnabled = settingsRepository.isNotificationBlockingEnabled.first()
            if (!isEnabled) return@launch

            // Check if whitelisted
            val isWhitelisted = whitelistRepository.isAppWhitelisted(packageName)
            if (isWhitelisted) return@launch

            // Exceptions for Phone and SMS (basic safety)
            // Note: Different ROMs might have different package names for these, 
            // but usually they contain "telecom", "telephony", or "mms".
            if (packageName.contains("telecom") || 
                packageName.contains("telephony") || 
                packageName.contains("mms") ||
                packageName.contains("incallui")) {
                return@launch
            }

            // Dismiss the notification
            cancelNotification(sbn.key)
            Log.d("NotificationBlocker", "Blocked notification from: $packageName")
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
    }
}
