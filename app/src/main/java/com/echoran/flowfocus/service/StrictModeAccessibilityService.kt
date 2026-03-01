package com.echoran.flowfocus.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.echoran.flowfocus.data.repository.WhitelistedAppRepository
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class StrictModeAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "StrictModeService"
        
        // Expose a flow so other parts of the app can react to window changes if needed
        private val _currentPackageName = MutableStateFlow<String?>(null)
        val currentPackageName: StateFlow<String?> = _currentPackageName.asStateFlow()

        // Global state for timer to control blocking, updated by TimerService
        var isSessionActive = false
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface StrictModeEntryPoint {
        fun getWhitelistRepository(): WhitelistedAppRepository
    }

    private lateinit var whitelistRepository: WhitelistedAppRepository
    private lateinit var overlayManager: StrictModeOverlayManager
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            // We only care about window state changes to detect app launches
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            // Set flag to get window content if needed later
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        }
        serviceInfo = info
        Log.d(TAG, "Accessibility Service Connected")

        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            StrictModeEntryPoint::class.java
        )
        whitelistRepository = entryPoint.getWhitelistRepository()
        overlayManager = StrictModeOverlayManager(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString()
            if (packageName != null) {
                _currentPackageName.value = packageName
                Log.d(TAG, "Active App: $packageName")
                
                if (isSessionActive) {
                    checkAndBlockApp(packageName)
                } else {
                    overlayManager.removeOverlay()
                }
            }
        }
    }

    private fun checkAndBlockApp(packageName: String) {
        // Exclude our own app and system UI
        if (packageName == this.packageName || packageName == "com.android.systemui" || packageName == "com.android.launcher" || packageName.contains("launcher")) {
            overlayManager.removeOverlay()
            return
        }

        // Exclude Input Method Editors (Keyboards)
        val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        val enabledImes = imm.enabledInputMethodList
        if (enabledImes.any { it.packageName == packageName }) {
            overlayManager.removeOverlay()
            return
        }

        scope.launch {
            val isWhitelisted = whitelistRepository.isAppWhitelisted(packageName)
            launch(Dispatchers.Main) {
                if (!isWhitelisted) {
                    Log.d(TAG, "Blocking App: $packageName")
                    val pm = packageManager
                    val appName = try {
                        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
                    } catch (e: Exception) {
                        packageName
                    }
                    
                    overlayManager.showOverlay(appName)
                    
                    // Start countdown
                    launch {
                        for (i in 3 downTo 1) {
                            overlayManager.updateMessage("心流番茄：严格模式\n\n已拦截应用: $appName\n\n将在 $i 秒后返回专注...")
                            kotlinx.coroutines.delay(1000)
                        }
                        
                        // Automatic redirection to our app after countdown
                        val intent = android.content.Intent(this@StrictModeAccessibilityService, com.echoran.flowfocus.MainActivity::class.java).apply {
                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                        }
                        startActivity(intent)
                    }
                } else {
                    overlayManager.removeOverlay()
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility Service Interrupted")
    }
}
