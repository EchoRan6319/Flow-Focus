package com.echoran.flowfocus.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FocusManager @Inject constructor() {
    
    private val _isSessionActive = MutableStateFlow(false)
    val isSessionActive: StateFlow<Boolean> = _isSessionActive.asStateFlow()

    private val _isStrictMode = MutableStateFlow(false)
    val isStrictMode: StateFlow<Boolean> = _isStrictMode.asStateFlow()

    fun setSessionState(active: Boolean, strict: Boolean = false) {
        _isSessionActive.value = active
        _isStrictMode.value = strict
    }
}
