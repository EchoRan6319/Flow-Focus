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

// No longer needs static isSessionActive as we use FocusManager
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface StrictModeEntryPoint {
        fun getWhitelistRepository(): WhitelistedAppRepository
        fun getFocusManager(): FocusManager
    }

    private lateinit var whitelistRepository: WhitelistedAppRepository
    private lateinit var focusManager: FocusManager
    private lateinit var overlayManager: StrictModeOverlayManager
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var redirectionJob: kotlinx.coroutines.Job? = null
    private var lastBlockedPackage: String? = null

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
        focusManager = entryPoint.getFocusManager()
        overlayManager = StrictModeOverlayManager(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString()
            if (packageName != null) {
                _currentPackageName.value = packageName
                Log.d(TAG, "Active App: $packageName")
                
                if (focusManager.isSessionActive.value) {
                    checkAndBlockApp(packageName)
                } else {
                    lastBlockedPackage = null
                    redirectionJob?.cancel()
                    overlayManager.removeOverlay()
                }
            }
        }
    }

    private fun checkAndBlockApp(packageName: String) {
        val isOurApp = packageName == this.packageName
        
        // Exclude our own app and system UI
        if (isOurApp || packageName == "com.android.systemui" || packageName == "com.android.launcher" || packageName.contains("launcher")) {
            // If it's our app, we should only cancel the redirection if we are NOT currently in a countdown
            // because showing the overlay might trigger a window event for our own package.
            if (!isOurApp || (redirectionJob?.isActive != true)) {
                Log.d(TAG, "Safe App detected: $packageName. Stopping blocking.")
                lastBlockedPackage = null
                redirectionJob?.cancel()
                overlayManager.removeOverlay()
                return
            } else {
                Log.d(TAG, "Ignoring event for our own app during active countdown: $packageName")
                return
            }
        }

        // Exclude Input Method Editors (Keyboards)
        val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        val enabledImes = imm.enabledInputMethodList
        if (enabledImes.any { it.packageName == packageName }) {
            lastBlockedPackage = null
            redirectionJob?.cancel()
            overlayManager.removeOverlay()
            return
        }

        // If we are already blocking this package, don't restart the countdown
        if (packageName == lastBlockedPackage && redirectionJob?.isActive == true) {
            return
        }

        lastBlockedPackage = packageName
        redirectionJob?.cancel()

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
                    redirectionJob = launch {
                        for (i in 2 downTo 1) {
                            overlayManager.updateMessage("心流番茄：严格模式\n\n已拦截应用: $appName\n\n将在 $i 秒后返回专注...")
                            kotlinx.coroutines.delay(1000)
                        }
                        
                        // 1. First, return to Home to reliably exit the blocked app
                        performGlobalAction(GLOBAL_ACTION_HOME)
                        
                        // 2. Wait a tiny bit for the home transition, then bring our app to front
                        kotlinx.coroutines.delay(200)

                        // 3. Explicitly launch our MainActivity
                        val intent = android.content.Intent(this@StrictModeAccessibilityService, com.echoran.flowfocus.MainActivity::class.java).apply {
                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or 
                                    android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                    android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        startActivity(intent)
                        lastBlockedPackage = null
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
