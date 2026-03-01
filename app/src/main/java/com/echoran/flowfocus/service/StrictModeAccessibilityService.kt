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
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var lastBlockedPackage: String? = null
    private var lastInterceptTimestamp: Long = 0

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
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString()
            if (packageName != null) {
                _currentPackageName.value = packageName
                Log.d(TAG, "Active App: $packageName")
                
                if (focusManager.isStrictMode.value) {
                    checkAndBlockApp(packageName)
                } else {
                    lastBlockedPackage = null
                }
            }
        }
    }

    private fun checkAndBlockApp(packageName: String) {
        val isOurApp = packageName == this.packageName
        
        Log.d(TAG, "DEBUG: checkAndBlockApp called for $packageName. Our package: ${this.packageName}. isOurApp: $isOurApp")

        // Exclude only our own app and system UI/System Server
        if (isOurApp || packageName == "android" || packageName == "com.android.systemui") {
            Log.d(TAG, "DEBUG: Package $packageName is SAFE. Ignoring.")
            return
        }
        
        // Block launchers and recent apps
        if (packageName.contains("launcher", ignoreCase = true) || 
            packageName.contains("trebuchet", ignoreCase = true) ||
            packageName == "com.android.launcher3" ||
            packageName == "com.google.android.apps.nexuslauncher" ||
            packageName == "com.android.systemui.recents") {
            Log.d(TAG, "DEBUG: Blocking launcher or recent apps: $packageName")
            launchInterceptActivity("系统桌面")
            return
        }

        // Whitelist ALL System Apps (Safe Guard for most ROMs)
        try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            if ((appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0 || 
                (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                Log.d(TAG, "DEBUG: Package $packageName is a SYSTEM APP. Ignoring.")
                return
            }
        } catch (e: Exception) {
            // Package might be transient or gone
        }

        // Exclude Input Method Editors (Keyboards)
        val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        val enabledImes = imm.enabledInputMethodList
        if (enabledImes.any { it.packageName == packageName }) {
            Log.d(TAG, "DEBUG: Package $packageName is an IME. Ignoring.")
            return
        }

        // Avoid infinite launch loops
        val now = System.currentTimeMillis()
        if (packageName == lastBlockedPackage && (now - lastInterceptTimestamp < 2000)) {
            Log.d(TAG, "DEBUG: Package $packageName recently blocked. Throttling.")
            return
        }

        lastBlockedPackage = packageName
        lastInterceptTimestamp = now

        scope.launch {
            val isWhitelisted = whitelistRepository.isAppWhitelisted(packageName)
            Log.d(TAG, "DEBUG: Package $packageName whitelist status: $isWhitelisted")
            if (!isWhitelisted) {
                Log.d(TAG, "DEBUG: BLOCKING non-whitelisted app: $packageName")
                
                launch(Dispatchers.Main) {
                    val pm = packageManager
                    val appName = try {
                        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
                    } catch (e: Exception) {
                        packageName
                    }
                    
                    // Add a Toast for immediate visual confirmation since Logcat is failing the user
                    android.widget.Toast.makeText(applicationContext, "严格模式拦截: $appName ($packageName)", android.widget.Toast.LENGTH_SHORT).show()
                    
                    launchInterceptActivity(appName)
                }
            }
        }
    }

    private fun launchInterceptActivity(appName: String) {
        Log.d(TAG, "Launching InterceptActivity for $appName")
        val intent = android.content.Intent(this, com.echoran.flowfocus.ui.InterceptActivity::class.java).apply {
            putExtra("EXTRA_APP_NAME", appName)
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or 
                    android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility Service Interrupted")
    }
}
