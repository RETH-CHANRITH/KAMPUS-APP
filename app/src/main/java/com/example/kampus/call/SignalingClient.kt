package com.example.kampus.call

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.Response
import okio.ByteString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

/**
 * Simple WebSocket signaling client using OkHttp.
 * Replace `signalingUrl` with your server and handle the JSON payloads accordingly.
 */
class SignalingClient(private val signalingUrl: String) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events

    private var ws: WebSocket? = null

    fun connect() {
        val client = OkHttpClient.Builder().build()
        val request = Request.Builder().url(signalingUrl).build()
        ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                // Notify connected
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                // Forward signaling messages to listeners (JSON SDP / ICE)
                scope.launch {
                    _events.emit(text)
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                onMessage(webSocket, bytes.utf8())
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            }
        })
    }

    fun send(message: String) {
        ws?.send(message)
    }

    fun close() {
        ws?.close(1000, "bye")
        ws = null
    }
}
