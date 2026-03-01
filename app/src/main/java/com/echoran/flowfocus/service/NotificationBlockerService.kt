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
    lateinit var focusManager: FocusManager

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var whitelistRepository: WhitelistedAppRepository

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)

        val packageName = sbn.packageName
        
        // 1. Check if focus session is active via FocusManager
        if (!focusManager.isSessionActive.value) return

        // 2. Exclude self and system-critical packages
        if (packageName == this.packageName || 
            packageName == "com.android.systemui" || 
            packageName.contains("launcher")) {
            return
        }

        serviceScope.launch {
            // 3. Check if notification blocking is enabled in settings
            val isEnabled = settingsRepository.isNotificationBlockingEnabled.first()
            if (!isEnabled) return@launch

            // 4. Check if whitelisted
            val isWhitelisted = whitelistRepository.isAppWhitelisted(packageName)
            if (isWhitelisted) {
                Log.d("NotificationBlocker", "Whitelisted app: $packageName, allowing notification.")
                return@launch
            }

            // 5. Basic safety exceptions (Phone/SMS)
            if (packageName.contains("telecom") || 
                packageName.contains("telephony") || 
                packageName.contains("mms") ||
                packageName.contains("incallui")) {
                return@launch
            }

            // 6. Dismiss the notification
            cancelNotification(sbn.key)
            Log.d("NotificationBlocker", "Blocked notification from: $packageName")
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
    }
}
