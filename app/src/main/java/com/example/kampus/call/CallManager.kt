package com.example.kampus.call

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Lightweight placeholder for realtime call signalling and state.
 * Replace with real WebSocket / WebRTC logic when integrating.
 */
object CallManager {
    private val scope = CoroutineScope(Dispatchers.Main)

    enum class State { IDLE, RINGING, CONNECTING, CONNECTED, ENDED }

    private val _state = MutableStateFlow(State.IDLE)
    val state: StateFlow<State> = _state

    private val _remoteUser = MutableStateFlow("")
    val remoteUser: StateFlow<String> = _remoteUser

    fun startIncoming(from: String) {
        _remoteUser.value = from
        _state.value = State.RINGING
    }

    fun accept() {
        _state.value = State.CONNECTING
        // Simulate connect
        scope.launch {
            kotlinx.coroutines.delay(700)
            _state.value = State.CONNECTED
        }
    }

    fun decline() {
        _state.value = State.ENDED
        scope.launch { kotlinx.coroutines.delay(300); _state.value = State.IDLE }
    }

    fun endCall() {
        _state.value = State.ENDED
        scope.launch { kotlinx.coroutines.delay(300); _state.value = State.IDLE }
    }

    // Simple API for outgoing call simulation
    fun startOutgoing(to: String) {
        _remoteUser.value = to
        _state.value = State.CONNECTING
        scope.launch {
            kotlinx.coroutines.delay(900)
            _state.value = State.CONNECTED
        }
    }
}
