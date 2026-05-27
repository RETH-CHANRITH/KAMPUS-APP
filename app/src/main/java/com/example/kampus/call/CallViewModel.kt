package com.example.kampus.call

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * ViewModel wrapper around CallManager for UI binding.
 * Exposes call session state and control methods (screen share, bitrate, mute, etc.).
 */
class CallViewModel : ViewModel() {

    val sessionState: StateFlow<CallSessionState> = CallManager.sessionState

    fun startCall(chatId: String, callId: String, callType: String) {
        CallManager.start(chatId, callId, callType)
    }

    fun endCall() {
        CallManager.endCall()
    }

    fun toggleMute() {
        CallManager.toggleMute()
    }

    fun toggleSpeaker() {
        CallManager.toggleSpeaker()
    }

    fun toggleCamera() {
        CallManager.toggleCamera()
    }

    fun switchCamera() {
        CallManager.switchCamera()
    }

    fun startScreenShareWithIntent(data: Intent?) {
        data?.let { CallManager.startScreenShare(it) }
    }

    fun stopScreenShare() {
        CallManager.stopScreenShare()
    }

    fun setVideoBitrate(kbps: Int) {
        CallManager.setVideoMaxBitrate(kbps)
    }

}
