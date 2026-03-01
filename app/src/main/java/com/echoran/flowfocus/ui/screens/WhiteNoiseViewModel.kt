package com.echoran.flowfocus.ui.screens

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.echoran.flowfocus.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NoiseTrack(val name: String, val resId: Int?, val customUri: String? = null)

@HiltViewModel
class WhiteNoiseViewModel @Inject constructor(
    val player: ExoPlayer,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _availableTracks = MutableStateFlow<List<NoiseTrack>>(
        listOf(
            NoiseTrack("下雨", null),
            NoiseTrack("海浪", null),
            NoiseTrack("咖啡馆", null)
        )
    )
    val availableTracks: StateFlow<List<NoiseTrack>> = _availableTracks.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentTrack = MutableStateFlow<NoiseTrack?>(null)
    val currentTrack: StateFlow<NoiseTrack?> = _currentTrack.asStateFlow()

    init {
        player.repeatMode = Player.REPEAT_MODE_ONE
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }
        })

        // Combine built-in and custom tracks
        viewModelScope.launch {
            settingsRepository.customWhiteNoiseTracks.collect { customUris ->
                val builtIn = listOf(
                    NoiseTrack("下雨", null),
                    NoiseTrack("海浪", null),
                    NoiseTrack("咖啡馆", null)
                )
                val custom = customUris.map { uri ->
                    NoiseTrack(uri.split("/").last(), null, uri)
                }
                _availableTracks.value = builtIn + custom
            }
        }
    }

    fun playTrack(track: NoiseTrack) {
        if (_currentTrack.value == track) {
            if (_isPlaying.value) {
                player.pause()
            } else {
                player.play()
            }
            return
        }

        _currentTrack.value = track
        
        val mediaItem = if (track.customUri != null) {
            MediaItem.fromUri(track.customUri)
        } else {
            // Mock built-in resources for now
            // MediaItem.fromUri(Uri.parse("android.resource://com.echoran.flowfocus/${track.resId}"))
            null
        }

        if (mediaItem != null) {
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
        } else {
            // Mock play state for built-in if no file yet
            _isPlaying.value = true
        }
    }

    fun stop() {
        player.stop()
        _isPlaying.value = false
        _currentTrack.value = null
    }

    override fun onCleared() {
        super.onCleared()
        player.release()
    }
}
