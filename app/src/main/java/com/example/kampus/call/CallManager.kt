package com.example.kampus.call

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.media.AudioManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer
import org.webrtc.RTCStats
import org.webrtc.RTCStatsReport
import org.webrtc.VideoTrack
import android.util.Log
import kotlin.math.max
import kotlin.math.min

private enum class CallRole {
    CALLER,
    CALLEE,
}

data class CallSessionState(
    val chatId: String = "",
    val callId: String = "",
    val callType: String = "voice",
    val status: String = "IDLE",
    val message: String = "",
    val isMuted: Boolean = false,
    val speakerOn: Boolean = true,
    val isVideo: Boolean = false,
    val isCameraEnabled: Boolean = true,
    val isReconnecting: Boolean = false,
    val connectionQuality: String = "",
    val connectedAt: Long = 0L,
    val remoteUserId: String = "",
)

object CallManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val firestore = FirebaseFirestore.getInstance()
    private val signalingRepo = FirestoreSignalingRepository(firestore)
    private val auth = FirebaseAuth.getInstance()
    private const val TAG = "CallManager"
    private val state = MutableStateFlow(CallSessionState())
    private var appContext: Context? = null
    private var webRtcClient: WebRtcClient? = null
    private var callDocListener: ListenerRegistration? = null
    private var callerCandidatesListener: ListenerRegistration? = null
    private var calleeCandidatesListener: ListenerRegistration? = null
    private var callDocRef: DocumentReference? = null
    private var currentRole: CallRole? = null
    private var remoteDescriptionApplied = false
    private var localDescriptionApplied = false
    private var callStartTicker: Job? = null
    private var adaptiveMonitorJob: Job? = null
    private var activeCallId: String = ""
    private var activeChatId: String = ""
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 3
    private var lastAdaptiveSample: AdaptiveStatsSample? = null
    private var appliedAdaptiveBitrateKbps: Int = 0
    private var appliedAdaptiveQuality: WebRtcClient.VideoQuality = WebRtcClient.VideoQuality.AUTO

    val sessionState: StateFlow<CallSessionState> = state.asStateFlow()

    fun initialize(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        webRtcClient = WebRtcClient(appContext!!).also { it.initialize() }
    }

    fun attachLocalRenderer(renderer: SurfaceViewRenderer) {
        webRtcClient?.setLocalRenderer(renderer)
    }

    fun attachRemoteRenderer(renderer: SurfaceViewRenderer) {
        webRtcClient?.setRemoteRenderer(renderer)
    }

    fun start(chatId: String, callId: String, callType: String) {
        val userId = auth.currentUser?.uid ?: return
        if (chatId.isBlank() || callId.isBlank()) return

        initialize(appContext ?: return)
        if (activeCallId == callId && activeChatId == chatId) return

        clearSession(keepUiState = false)
        activeCallId = callId
        activeChatId = chatId
        callDocRef = firestore.collection("chats").document(chatId).collection("calls").document(callId)
        state.value = CallSessionState(
            chatId = chatId,
            callId = callId,
            callType = callType,
            status = "CONNECTING",
            message = "Preparing secure connection...",
            isVideo = callType.equals("video", ignoreCase = true),
            isCameraEnabled = callType.equals("video", ignoreCase = true),
            remoteUserId = "",
            connectionQuality = "Connecting",
        )

        scope.launch {
            val callSnapshot = runCatching { callDocRef?.get()?.await() }.getOrNull()
            if (callSnapshot == null || !callSnapshot.exists()) {
                updateState(status = "ENDED", message = "Call not found")
                return@launch
            }

            val callerId = callSnapshot.getString("callerId").orEmpty()
            val calleeId = callSnapshot.getString("calleeId").orEmpty()
            currentRole = if (callerId == userId) CallRole.CALLER else CallRole.CALLEE
            val remoteUserId = if (currentRole == CallRole.CALLER) calleeId else callerId
            val isVideo = callType.equals("video", ignoreCase = true)

            updateState(remoteUserId = remoteUserId, isVideo = isVideo)
            configureAudioRoute(isSpeakerOn = true)

            val observer = createPeerObserver()
            val iceServers = CallConfig.buildIceServers()
            Log.d(TAG, "Using iceServers=${iceServers.size} for callId=$callId chatId=$chatId")

            webRtcClient?.createPeerConnection(iceServers, observer)

            val capturer = if (isVideo) webRtcClient?.createFrontCameraCapturer() else null
            // detect emulator and request low quality capture there
            val preferredQuality = if (isProbablyAnEmulator()) {
                Log.d(TAG, "Emulator detected - requesting low quality video capture")
                WebRtcClient.VideoQuality.LOW
            } else WebRtcClient.VideoQuality.AUTO
            webRtcClient?.createLocalMedia(capturer, preferredQuality)

            listenForCallDocument(callDocRef!!)
            when (currentRole) {
                CallRole.CALLER -> startAsCaller(callSnapshot)
                CallRole.CALLEE -> startAsCallee(callSnapshot)
                null -> Unit
            }
        }
    }

    private fun isProbablyAnEmulator(): Boolean {
        val fingerprint = android.os.Build.FINGERPRINT ?: ""
        val product = android.os.Build.PRODUCT ?: ""
        val model = android.os.Build.MODEL ?: ""
        val manufacturer = android.os.Build.MANUFACTURER ?: ""
        return fingerprint.startsWith("generic") || fingerprint.startsWith("unknown") ||
            product.contains("sdk") || product.contains("emulator") ||
            model.contains("Android SDK built for") || manufacturer.contains("Genymotion")
    }

    fun bindVideoRenderers(localRenderer: SurfaceViewRenderer?, remoteRenderer: SurfaceViewRenderer?) {
        localRenderer?.let { webRtcClient?.setLocalRenderer(it) }
        remoteRenderer?.let { webRtcClient?.setRemoteRenderer(it) }
    }

    fun toggleMute() {
        val nextMuted = !state.value.isMuted
        webRtcClient?.setMicrophoneEnabled(!nextMuted)
        state.value = state.value.copy(isMuted = nextMuted)
    }

    fun toggleSpeaker() {
        val nextSpeaker = !state.value.speakerOn
        configureAudioRoute(nextSpeaker)
        state.value = state.value.copy(speakerOn = nextSpeaker)
    }

    fun switchCamera() {
        webRtcClient?.switchCamera()
    }

    fun toggleCamera() {
        val nextEnabled = !state.value.isCameraEnabled
        webRtcClient?.setCameraEnabled(nextEnabled)
        state.value = state.value.copy(isCameraEnabled = nextEnabled)
    }

    fun endCall() {
        scope.launch {
            runCatching {
                callDocRef?.update(
                    mapOf(
                        "status" to "ENDED",
                        "endedAt" to System.currentTimeMillis(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                )
            }
            appContext?.let { CallForegroundService.stop(it) }
            teardownSession(keepUiState = false)
            // cleanup firestore candidate docs and call doc
            callDocRef?.let { ref ->
                try { signalingRepo.cleanupCall(ref) } catch (_: Exception) {}
            }
            updateState(status = "ENDED", message = "Call ended")
        }
    }

    fun startScreenShare(screenIntentData: Intent) {
        val capturer = webRtcClient?.createScreenCapturer(screenIntentData)
        if (capturer != null) {
            webRtcClient?.startScreenShare(capturer)
        }
    }

    fun stopScreenShare() {
        webRtcClient?.stopScreenShare()
    }

    fun setVideoMaxBitrate(kbps: Int) {
        webRtcClient?.setVideoMaxBitrate(kbps)
    }

    fun setPreferredVideoQuality(quality: WebRtcClient.VideoQuality) {
        webRtcClient?.let { client ->
            // recreate local media with preferred quality if capturer exists
            val capturer = client.createFrontCameraCapturer()
            client.createLocalMedia(capturer, quality)
        }
    }

    fun restartLocalCapture() {
        webRtcClient?.restartCaptureWithLastParams()
    }

    fun setAudioRouteTo(route: String) {
        val context = appContext ?: return
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        when (route.uppercase()) {
            "SPEAKER" -> {
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                audioManager.isSpeakerphoneOn = true
            }
            "EARPIECE" -> {
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                audioManager.isSpeakerphoneOn = false
            }
            "BLUETOOTH" -> {
                // request SCO for bluetooth headset
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                try { audioManager.startBluetoothSco() } catch (_: Exception) {}
            }
        }
        state.value = state.value.copy(speakerOn = audioManager.isSpeakerphoneOn)
    }

    fun disconnectLocalOnly() {
        teardownSession(keepUiState = true)
    }

    private data class AdaptiveStatsSample(
        val timestampUs: Double,
        val outgoingBitrateBps: Double,
        val rttMs: Double,
        val bytesSent: Long,
    )

    private fun startAsCaller(callSnapshot: DocumentSnapshot) {
        val offerSdp = callSnapshot.getString("offerSdp").orEmpty()
        val answerSdp = callSnapshot.getString("answerSdp").orEmpty()

        listenForRemoteCandidates(CallRole.CALLER)

        if (answerSdp.isNotBlank()) {
            applyRemoteDescription(answerSdp, SessionDescription.Type.ANSWER)
        }

        if (offerSdp.isNotBlank()) {
            return
        }

        webRtcClient?.createOffer(
            sdpObserver(
                onCreateSuccess = { desc ->
                    Log.d(TAG, "Offer created for call=${callDocRef?.id}, setting local description")
                    webRtcClient?.setLocalDescription(
                        desc,
                        sdpObserver(
                            onSetSuccess = {
                                localDescriptionApplied = true
                                scope.launch {
                                    runCatching {
                                        callDocRef?.let { ref ->
                                            signalingRepo.writeOffer(ref, desc.description, desc.type.canonicalForm())
                                        }
                                        Log.d(TAG, "Offer written to Firestore for call=${callDocRef?.id}")
                                    }
                                }
                            },
                            onSetFailure = { error -> updateState(status = "FAILED", message = error) },
                        ),
                    )
                },
                onCreateFailure = { error -> updateState(status = "FAILED", message = error) },
            ),
            MediaConstraints(),
        )
    }

    private fun startAsCallee(callSnapshot: DocumentSnapshot) {
        listenForRemoteCandidates(CallRole.CALLEE)

        val offerSdp = callSnapshot.getString("offerSdp").orEmpty()
        if (offerSdp.isBlank()) {
            updateState(status = "RINGING", message = "Waiting for offer...")
            return
        }

        applyRemoteDescription(offerSdp, SessionDescription.Type.OFFER)
    }

    private fun listenForCallDocument(callRef: DocumentReference) {
        callDocListener?.remove()
        callDocListener = callRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                updateState(status = "FAILED", message = error.message ?: "Call sync failed")
                return@addSnapshotListener
            }

            if (snapshot == null || !snapshot.exists()) {
                updateState(status = "ENDED", message = "Call ended")
                return@addSnapshotListener
            }

            val status = snapshot.getString("status").orEmpty().ifBlank { "RINGING" }
            val message = when (status.uppercase()) {
                "ACCEPTED" -> "Connecting..."
                "CONNECTED" -> "Connected"
                "DECLINED" -> "Call declined"
                "MISSED" -> "Call missed"
                "ENDED" -> "Call ended"
                else -> "Ringing..."
            }
            updateState(status = status.uppercase(), message = message)

            if (status.equals("ENDED", ignoreCase = true)) {
                teardownSession(keepUiState = true)
                updateState(status = "ENDED", message = "Call ended")
                return@addSnapshotListener
            }

            val answerSdp = snapshot.getString("answerSdp").orEmpty()
            if (currentRole == CallRole.CALLER && answerSdp.isNotBlank() && !remoteDescriptionApplied) {
                applyRemoteDescription(answerSdp, SessionDescription.Type.ANSWER)
            }

            val offerSdp = snapshot.getString("offerSdp").orEmpty()
            if (currentRole == CallRole.CALLEE && offerSdp.isNotBlank() && !remoteDescriptionApplied) {
                applyRemoteDescription(offerSdp, SessionDescription.Type.OFFER)
            }
        }
    }

    private fun listenForRemoteCandidates(role: CallRole) {
        val candidateCollection = if (role == CallRole.CALLER) "calleeCandidates" else "callerCandidates"
        val collectionRef = callDocRef?.collection(candidateCollection) ?: return
        val listener = collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            snapshot.documentChanges.forEach { change ->
                if (change.type.name != "ADDED") return@forEach
                val candidate = candidateFromSnapshot(change.document)
                if (candidate != null) {
                    Log.d(TAG, "Remote ICE candidate received for call=${callDocRef?.id}: mid=${candidate.sdpMid}")
                    webRtcClient?.addIceCandidate(candidate)
                }
            }
        }
        if (role == CallRole.CALLER) {
            calleeCandidatesListener?.remove()
            calleeCandidatesListener = listener
        } else {
            callerCandidatesListener?.remove()
            callerCandidatesListener = listener
        }
    }

    private fun createPeerObserver(): PeerConnection.Observer {
        return object : PeerConnection.Observer {
            override fun onSignalingChange(newState: PeerConnection.SignalingState) = Unit
            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState) {
                Log.d(TAG, "onIceConnectionChange: $newState for call=${callDocRef?.id}")
                when (newState) {
                    PeerConnection.IceConnectionState.CONNECTED -> {
                        updateState(
                            status = "CONNECTED",
                            message = "Connected",
                            connectedAt = System.currentTimeMillis(),
                            isReconnecting = false,
                            connectionQuality = "Excellent",
                        )
                        scope.launch {
                            runCatching {
                                callDocRef?.update(
                                    mapOf(
                                        "status" to "CONNECTED",
                                        "connectedAt" to System.currentTimeMillis(),
                                        "updatedAt" to FieldValue.serverTimestamp(),
                                    ),
                                )
                            }
                        }
                        startConnectedTicker()
                        startAdaptiveMonitoring()
                        appContext?.let { CallForegroundService.start(it, activeChatId, activeCallId, state.value.callType, "Connected") }
                    }

                    PeerConnection.IceConnectionState.DISCONNECTED,
                    PeerConnection.IceConnectionState.FAILED,
                    PeerConnection.IceConnectionState.CLOSED -> {
                        // try a limited number of reconnect attempts for transient failures
                        if (newState != PeerConnection.IceConnectionState.CLOSED && reconnectAttempts < maxReconnectAttempts) {
                            reconnectAttempts++
                            Log.d(TAG, "Scheduling reconnect attempt #$reconnectAttempts for call=${callDocRef?.id}")
                            scope.launch {
                                delay(1500L * reconnectAttempts)
                                tryReconnect()
                            }
                        }
                        val isClosed = newState == PeerConnection.IceConnectionState.CLOSED
                        updateState(
                            status = if (isClosed) "ENDED" else "RECONNECTING",
                            message = if (isClosed) "Call ended" else "Reconnecting...",
                            isReconnecting = !isClosed,
                            connectionQuality = if (isClosed) "Offline" else if (newState == PeerConnection.IceConnectionState.FAILED) "Poor" else "Fair",
                        )
                        if (isClosed) {
                            stopConnectedTicker()
                            stopAdaptiveMonitoring()
                            appContext?.let { CallForegroundService.stop(it) }
                        }
                    }

                    else -> Unit
                }
            }

            override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
            override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState) = Unit
            override fun onIceCandidate(candidate: IceCandidate) {
                val role = currentRole ?: return
                val candidateCollection = if (role == CallRole.CALLER) "callerCandidates" else "calleeCandidates"
                scope.launch {
                    runCatching {
                        Log.d(TAG, "Local ICE candidate gathered, adding to $candidateCollection for call=${callDocRef?.id}")
                        callDocRef?.let { ref -> signalingRepo.addCandidate(ref, candidateCollection, candidate.toMap()) }
                    }
                }
            }

            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) = Unit
            override fun onAddStream(stream: org.webrtc.MediaStream) = Unit
            override fun onRemoveStream(stream: org.webrtc.MediaStream) = Unit
            override fun onDataChannel(dataChannel: org.webrtc.DataChannel) = Unit
            override fun onRenegotiationNeeded() = Unit
            override fun onAddTrack(receiver: org.webrtc.RtpReceiver, mediaStreams: Array<out org.webrtc.MediaStream>) {
                val track = receiver.track()
                if (track is VideoTrack) {
                    Log.d(TAG, "Remote video track added for call=${callDocRef?.id}")
                    webRtcClient?.notifyRemoteVideoTrack(track)
                }
            }
            override fun onTrack(transceiver: org.webrtc.RtpTransceiver?) {
                val track = transceiver?.receiver?.track()
                if (track is VideoTrack) {
                    webRtcClient?.notifyRemoteVideoTrack(track)
                }
            }
            override fun onStandardizedIceConnectionChange(newState: PeerConnection.IceConnectionState) = Unit
            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) {
                when (newState) {
                    PeerConnection.PeerConnectionState.CONNECTED -> updateState(
                        status = "CONNECTED",
                        message = "Connected",
                        isReconnecting = false,
                        connectionQuality = "Excellent",
                    )
                    PeerConnection.PeerConnectionState.DISCONNECTED,
                    PeerConnection.PeerConnectionState.FAILED,
                    PeerConnection.PeerConnectionState.CLOSED -> {
                        val isClosed = newState == PeerConnection.PeerConnectionState.CLOSED
                        updateState(
                            status = if (isClosed) "ENDED" else "RECONNECTING",
                            message = if (isClosed) "Call ended" else "Reconnecting...",
                            isReconnecting = !isClosed,
                            connectionQuality = if (isClosed) "Offline" else if (newState == PeerConnection.PeerConnectionState.FAILED) "Poor" else "Fair",
                        )
                        if (isClosed) {
                            stopConnectedTicker()
                            stopAdaptiveMonitoring()
                            appContext?.let { CallForegroundService.stop(it) }
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun tryReconnect() {
        if (callDocRef == null) return
        Log.d(TAG, "Attempting reconnect for call=${callDocRef?.id}")
        // Recreate peer connection and local media
        webRtcClient?.close()
        webRtcClient = appContext?.let { WebRtcClient(it).also { client -> client.initialize() } }
        val observer = createPeerObserver()
        val iceServers = CallConfig.buildIceServers()
        webRtcClient?.createPeerConnection(iceServers, observer)
        val capturer = if (state.value.isVideo) webRtcClient?.createFrontCameraCapturer() else null
        webRtcClient?.createLocalMedia(capturer)
        // If caller, trigger new offer to restart ICE
        if (currentRole == CallRole.CALLER) {
            webRtcClient?.createOffer(
                sdpObserver(
                    onCreateSuccess = { desc ->
                        webRtcClient?.setLocalDescription(desc, sdpObserver(onSetSuccess = {
                            scope.launch {
                                runCatching {
                                    callDocRef?.let { ref -> signalingRepo.writeOffer(ref, desc.description, desc.type.canonicalForm()) }
                                }
                            }
                        }))
                    }
                )
            )
        }
    }

    private fun applyRemoteDescription(sdpText: String, type: SessionDescription.Type) {
        remoteDescriptionApplied = true
        webRtcClient?.setRemoteDescription(
            SessionDescription(type, sdpText),
            sdpObserver(
                onSetSuccess = {
                    if (currentRole == CallRole.CALLEE && type == SessionDescription.Type.OFFER) {
                        webRtcClient?.createAnswer(
                            sdpObserver(
                                onCreateSuccess = { desc ->
                                    webRtcClient?.setLocalDescription(
                                        desc,
                                        sdpObserver(
                                            onSetSuccess = {
                                                localDescriptionApplied = true
                                                scope.launch {
                                                    runCatching {
                                                                        callDocRef?.let { ref ->
                                                                            signalingRepo.writeAnswer(ref, desc.description)
                                                                        }
                                                    }
                                                }
                                            },
                                            onSetFailure = { error -> updateState(status = "FAILED", message = error) },
                                        ),
                                    )
                                },
                                onCreateFailure = { error -> updateState(status = "FAILED", message = error) },
                            ),
                            MediaConstraints(),
                        )
                    }
                },
                onSetFailure = { error -> updateState(status = "FAILED", message = error) },
            ),
        )
    }

    private fun candidateFromSnapshot(document: com.google.firebase.firestore.DocumentSnapshot): IceCandidate? {
        val candidate = document.getString("candidate").orEmpty()
        val sdpMid = document.getString("sdpMid")
        val sdpMLineIndex = (document.getLong("sdpMLineIndex") ?: 0L).toInt()
        if (candidate.isBlank() || sdpMid.isNullOrBlank()) return null
        return IceCandidate(sdpMid, sdpMLineIndex, candidate)
    }

    private fun IceCandidate.toMap(): Map<String, Any> = mapOf(
        "candidate" to sdp,
        "sdpMid" to (sdpMid ?: "0"),
        "sdpMLineIndex" to sdpMLineIndex,
        "createdAt" to System.currentTimeMillis(),
    )

    private fun startConnectedTicker() {
        if (callStartTicker?.isActive == true) return
        callStartTicker = scope.launch {
            while (true) {
                delay(1000)
                if (!state.value.status.equals("CONNECTED", ignoreCase = true)) break
            }
        }
    }

    private fun startAdaptiveMonitoring() {
        if (adaptiveMonitorJob?.isActive == true) return
        adaptiveMonitorJob = scope.launch {
            while (isActive) {
                delay(2500)
                if (!state.value.status.equals("CONNECTED", ignoreCase = true)) continue
                webRtcClient?.collectStats { report ->
                    scope.launch { evaluateAdaptiveStats(report) }
                }
            }
        }
    }

    private fun stopAdaptiveMonitoring() {
        adaptiveMonitorJob?.cancel()
        adaptiveMonitorJob = null
        lastAdaptiveSample = null
        appliedAdaptiveBitrateKbps = 0
        appliedAdaptiveQuality = WebRtcClient.VideoQuality.AUTO
    }

    private suspend fun evaluateAdaptiveStats(report: RTCStatsReport) {
        val sample = extractAdaptiveSample(report) ?: return
        val previous = lastAdaptiveSample
        lastAdaptiveSample = sample

        val measuredOutgoingKbps = previous?.let {
            val deltaBytes = max(0L, sample.bytesSent - it.bytesSent)
            val deltaSeconds = ((sample.timestampUs - it.timestampUs) / 1_000_000.0).coerceAtLeast(0.001)
            (deltaBytes * 8.0 / 1000.0 / deltaSeconds)
        } ?: sample.outgoingBitrateBps / 1000.0

        val networkEstimateKbps = max(measuredOutgoingKbps, sample.outgoingBitrateBps / 1000.0)
        val target = when {
            networkEstimateKbps <= 400.0 || sample.rttMs > 450.0 -> AdaptiveTarget(WebRtcClient.VideoQuality.LOW, 320)
            networkEstimateKbps <= 1100.0 || sample.rttMs > 250.0 -> AdaptiveTarget(WebRtcClient.VideoQuality.MEDIUM, 700)
            else -> AdaptiveTarget(WebRtcClient.VideoQuality.HIGH, 1600)
        }

        val qualityChanged = target.quality != appliedAdaptiveQuality
        val bitrateChanged = appliedAdaptiveBitrateKbps == 0 || kotlin.math.abs(target.bitrateKbps - appliedAdaptiveBitrateKbps) >= 120
        if (!qualityChanged && !bitrateChanged) return

        appliedAdaptiveQuality = target.quality
        appliedAdaptiveBitrateKbps = target.bitrateKbps

        Log.d(TAG, "Adaptive video stats: network=${"%.0f".format(networkEstimateKbps)}kbps rtt=${"%.0f".format(sample.rttMs)}ms -> ${target.quality} ${target.bitrateKbps}kbps")
        webRtcClient?.setVideoMaxBitrate(target.bitrateKbps)
        updateState(
            connectionQuality = when (target.quality) {
                WebRtcClient.VideoQuality.LOW -> "Poor · ${target.bitrateKbps}kbps"
                WebRtcClient.VideoQuality.MEDIUM -> "Fair · ${target.bitrateKbps}kbps"
                WebRtcClient.VideoQuality.HIGH -> "Good · ${target.bitrateKbps}kbps"
                WebRtcClient.VideoQuality.AUTO -> "Adaptive · ${target.bitrateKbps}kbps"
            },
        )
    }

    private data class AdaptiveTarget(
        val quality: WebRtcClient.VideoQuality,
        val bitrateKbps: Int,
    )

    private fun extractAdaptiveSample(report: RTCStatsReport): AdaptiveStatsSample? {
        var bytesSent = 0L
        var outgoingBitrateBps = 0.0
        var rttMs = 0.0

        report.statsMap.values.forEach { stats ->
            when (stats.type) {
                "outbound-rtp" -> {
                    if (isVideoStats(stats)) {
                        bytesSent += stats.longMember("bytesSent") ?: 0L
                    }
                }
                "candidate-pair" -> {
                    if (stats.stringMember("state") == "succeeded" || stats.boolMember("nominated") == true) {
                        outgoingBitrateBps = max(outgoingBitrateBps, stats.doubleMember("availableOutgoingBitrate") ?: 0.0)
                        val currentRttSeconds = stats.doubleMember("currentRoundTripTime")
                            ?: stats.doubleMember("roundTripTime")
                        if (currentRttSeconds != null) {
                            rttMs = max(rttMs, currentRttSeconds * 1000.0)
                        }
                    }
                }
                "remote-inbound-rtp" -> {
                    if (isVideoStats(stats)) {
                        val fractionLost = stats.doubleMember("fractionLost")
                        if (fractionLost != null && fractionLost > 0.0) {
                            // Use packet loss as an extra pressure signal when available.
                            if (fractionLost >= 0.05) {
                                rttMs = max(rttMs, 350.0)
                            }
                        }
                    }
                }
            }
        }

        if (bytesSent == 0L && outgoingBitrateBps == 0.0) return null

        return AdaptiveStatsSample(
            timestampUs = report.timestampUs,
            outgoingBitrateBps = outgoingBitrateBps,
            rttMs = rttMs,
            bytesSent = bytesSent,
        )
    }

    private fun isVideoStats(stats: RTCStats): Boolean {
        return stats.stringMember("kind") == "video" || stats.stringMember("mediaType") == "video"
    }

    private fun RTCStats.doubleMember(name: String): Double? {
        val value = members[name] ?: return null
        return when (value) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull()
            else -> null
        }
    }

    private fun RTCStats.longMember(name: String): Long? {
        val value = members[name] ?: return null
        return when (value) {
            is Number -> value.toLong()
            is String -> value.toLongOrNull()
            else -> null
        }
    }

    private fun RTCStats.boolMember(name: String): Boolean? {
        val value = members[name] ?: return null
        return when (value) {
            is Boolean -> value
            is String -> value.toBooleanStrictOrNull()
            else -> null
        }
    }

    private fun RTCStats.stringMember(name: String): String? {
        val value = members[name] ?: return null
        return value as? String
    }

    private fun stopConnectedTicker() {
        callStartTicker?.cancel()
        callStartTicker = null
    }

    private fun configureAudioRoute(isSpeakerOn: Boolean) {
        val context = appContext ?: return
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = isSpeakerOn
    }

    private fun updateState(
        chatId: String = state.value.chatId,
        callId: String = state.value.callId,
        callType: String = state.value.callType,
        status: String = state.value.status,
        message: String = state.value.message,
        isMuted: Boolean = state.value.isMuted,
        speakerOn: Boolean = state.value.speakerOn,
        isVideo: Boolean = state.value.isVideo,
        isCameraEnabled: Boolean = state.value.isCameraEnabled,
        isReconnecting: Boolean = state.value.isReconnecting,
        connectionQuality: String = state.value.connectionQuality,
        connectedAt: Long = state.value.connectedAt,
        remoteUserId: String = state.value.remoteUserId,
    ) {
        state.value = CallSessionState(
            chatId = chatId,
            callId = callId,
            callType = callType,
            status = status,
            message = message,
            isMuted = isMuted,
            speakerOn = speakerOn,
            isVideo = isVideo,
            isCameraEnabled = isCameraEnabled,
            isReconnecting = isReconnecting,
            connectionQuality = connectionQuality,
            connectedAt = connectedAt,
            remoteUserId = remoteUserId,
        )
    }

    private fun clearSession(keepUiState: Boolean) {
        callDocListener?.remove()
        callDocListener = null
        callerCandidatesListener?.remove()
        callerCandidatesListener = null
        calleeCandidatesListener?.remove()
        calleeCandidatesListener = null
        stopConnectedTicker()
        remoteDescriptionApplied = false
        localDescriptionApplied = false
        currentRole = null
        callDocRef = null
        activeCallId = ""
        activeChatId = ""
        webRtcClient?.close()
        webRtcClient = appContext?.let { WebRtcClient(it).also { client -> client.initialize() } }
        if (!keepUiState) {
            appContext?.let { CallForegroundService.stop(it) }
        }
        if (!keepUiState) {
            state.value = CallSessionState()
        }
    }

    private fun teardownSession(keepUiState: Boolean) {
        clearSession(keepUiState = keepUiState)
    }
}

private fun SessionDescription.Type.canonicalForm(): String = when (this) {
    SessionDescription.Type.OFFER -> "offer"
    SessionDescription.Type.PRANSWER -> "pranswer"
    SessionDescription.Type.ANSWER -> "answer"
    SessionDescription.Type.ROLLBACK -> "rollback"
    else -> toString().lowercase()
}

private fun sdpObserver(
    onCreateSuccess: (SessionDescription) -> Unit = {},
    onCreateFailure: (String) -> Unit = {},
    onSetSuccess: () -> Unit = {},
    onSetFailure: (String) -> Unit = {},
): SdpObserver = object : SdpObserver {
    override fun onCreateSuccess(desc: SessionDescription) = onCreateSuccess(desc)
    override fun onCreateFailure(error: String) = onCreateFailure(error)
    override fun onSetSuccess() = onSetSuccess()
    override fun onSetFailure(error: String) = onSetFailure(error)
}
