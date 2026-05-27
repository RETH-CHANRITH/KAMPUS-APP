package com.example.kampus.call

import org.webrtc.PeerConnection
import android.util.Log

/**
 * Centralized ICE server configuration. Replace TURN entries with your
 * production TURN provider credentials. TURN servers are required for
 * reliable cross-network connectivity (NAT/CGNAT).
 */
object CallConfig {
    private const val TAG = "CallConfig"

    private val STUN_URIS = listOf(
        "stun:stun.l.google.com:19302",
        "stun:stun1.l.google.com:19302",
    )

    // Example placeholder for TURN servers. Populate with real creds in production.
    data class TurnServer(val url: String, val username: String, val credential: String)

    // Mutable list that can be populated at runtime with production TURN credentials.
    // Call `CallConfig.setTurnServers(...)` early in app startup (e.g., in KampusApp) with
    // real TURN credentials provided by your TURN provider.
    private var TURN_SERVERS_INTERNAL: List<TurnServer> = emptyList()

    fun setTurnServers(servers: List<TurnServer>) {
        TURN_SERVERS_INTERNAL = servers
        Log.i(TAG, "TURN servers set: ${servers.map { it.url }}")
    }

    fun clearTurnServers() {
        TURN_SERVERS_INTERNAL = emptyList()
    }

    fun buildIceServers(): List<PeerConnection.IceServer> {
        val list = mutableListOf<PeerConnection.IceServer>()
        STUN_URIS.forEach { uri ->
            try {
                list.add(PeerConnection.IceServer.builder(uri).createIceServer())
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to add STUN $uri: ${t.message}")
            }
        }

        TURN_SERVERS_INTERNAL.forEach { turn ->
            try {
                list.add(
                    PeerConnection.IceServer.builder(turn.url)
                        .setUsername(turn.username)
                        .setPassword(turn.credential)
                        .createIceServer(),
                )
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to add TURN ${turn.url}: ${t.message}")
            }
        }

        return list
    }
}
