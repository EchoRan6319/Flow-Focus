package com.echoran.flowfocus

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.echoran.flowfocus.data.repository.SettingsRepository
import com.echoran.flowfocus.ui.navigation.FlowFocusNavGraph
import com.echoran.flowfocus.ui.theme.FlowFocusTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Check if hide from recents is enabled
        CoroutineScope(Dispatchers.Main).launch {
            settingsRepository.isHideFromRecentsEnabled.collect {
                if (it) {
                    // Set flag to exclude from recents
                    intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                }
            }
        }
        
        setContent {
            FlowFocusTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FlowFocusNavGraph()
                }
            }
        }
    }
}