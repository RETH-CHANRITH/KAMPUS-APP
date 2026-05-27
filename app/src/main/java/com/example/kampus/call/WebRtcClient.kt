package com.example.kampus.call

import android.content.Context
import android.util.Log
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RTCStatsReport
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.SoftwareVideoDecoderFactory
import org.webrtc.VideoCapturer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import android.media.projection.MediaProjection
import android.app.Activity
import android.content.Intent
import org.webrtc.ScreenCapturerAndroid
import org.webrtc.RtpSender
import org.webrtc.RtpParameters

/**
 * Minimal WebRTC client skeleton. This file provides an opinionated scaffold:
 * - initialize PeerConnectionFactory
 * - create PeerConnection
 * - expose start/stop and methods to apply remote SDP / ICE
 *
 * You need to wire this to `SignalingClient` and host permissions (CAMERA/AUDIO).
 */
class WebRtcClient(private val context: Context) {
    private val eglBase = EglBase.create()
    private val TAG = "WebRtcClient"
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localAudioTrack: AudioTrack? = null
    private var localVideoTrack: VideoTrack? = null
    private var remoteVideoTrack: VideoTrack? = null
    private var videoCapturer: VideoCapturer? = null
    private var localRenderer: SurfaceViewRenderer? = null
    private var remoteRenderer: SurfaceViewRenderer? = null
    private var initialized = false
    private var videoSender: RtpSender? = null
    private var lastCaptureParams: Triple<Int, Int, Int>? = null // width, height, fps
    private var lowQualityMode: Boolean = false
    private var forceSoftwareDecoder: Boolean = false

    fun initialize() {
        if (initialized) return
        val options = PeerConnectionFactory.InitializationOptions.builder(context).createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        Log.d(TAG, "PeerConnectionFactory initialized")

        val encoderFactory = DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
        val decoderFactory = when {
            forceSoftwareDecoder || isProbablyAnEmulator() -> {
                Log.d(TAG, "Using software video decoder fallback")
                SoftwareVideoDecoderFactory()
            }
            else -> DefaultVideoDecoderFactory(eglBase.eglBaseContext)
        }

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()

        initialized = true
    }

    fun setSoftwareDecoderFallbackEnabled(enabled: Boolean) {
        forceSoftwareDecoder = enabled
        Log.d(TAG, "Software decoder fallback enabled=$enabled")
    }

    fun createPeerConnection(iceServers: List<PeerConnection.IceServer>, observer: PeerConnection.Observer) {
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, observer)
        Log.d(TAG, "PeerConnection created, iceServers=${iceServers.size}")
    }

    enum class VideoQuality { AUTO, LOW, MEDIUM, HIGH }

    private fun isProbablyAnEmulator(): Boolean {
        val fingerprint = android.os.Build.FINGERPRINT ?: ""
        val product = android.os.Build.PRODUCT ?: ""
        val model = android.os.Build.MODEL ?: ""
        val manufacturer = android.os.Build.MANUFACTURER ?: ""
        return fingerprint.startsWith("generic") || fingerprint.startsWith("unknown") ||
            product.contains("sdk") || product.contains("emulator") ||
            model.contains("Android SDK built for") || manufacturer.contains("Genymotion")
    }

    fun createLocalMedia(videoCapturer: VideoCapturer?, preferredQuality: VideoQuality = VideoQuality.AUTO) {
        // Create audio source with common audio processing constraints (AEC/NS/AGC)
        val audioConstraints = MediaConstraints().apply {
            // these are hints used by some WebRTC builds
            optional.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
            optional.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
            optional.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
            optional.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
        }
        val audioSource: AudioSource? = peerConnectionFactory?.createAudioSource(audioConstraints)
        localAudioTrack = peerConnectionFactory?.createAudioTrack("ARDAMSa0", audioSource)

        Log.d(TAG, "Local audio track created: ${localAudioTrack != null}")

        if (videoCapturer != null) {
            this.videoCapturer = videoCapturer
            val videoSource: VideoSource? = peerConnectionFactory?.createVideoSource(videoCapturer.isScreencast)
            val surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
            try {
                videoCapturer.initialize(surfaceTextureHelper, context, videoSource?.capturerObserver)

                // Decide capture parameters based on emulator and preferredQuality
                val (w, h, fps) = when {
                    preferredQuality == VideoQuality.LOW -> Triple(320, 240, 15)
                    preferredQuality == VideoQuality.MEDIUM -> Triple(640, 480, 20)
                    preferredQuality == VideoQuality.HIGH -> Triple(1280, 720, 25)
                    isProbablyAnEmulator() -> {
                        lowQualityMode = true
                        Triple(320, 240, 15)
                    }
                    else -> Triple(640, 480, 20)
                }

                // store last params for possible retries
                lastCaptureParams = Triple(w, h, fps)

                videoCapturer.startCapture(w, h, fps)
                localVideoTrack = peerConnectionFactory?.createVideoTrack("ARDAMSv0", videoSource)
                localRenderer?.let { renderer ->
                    initRenderer(renderer)
                    localVideoTrack?.addSink(renderer)
                }
                Log.d(TAG, "Local video track created: ${localVideoTrack != null} capture=${w}x${h}@${fps} (emulator=$lowQualityMode)")
            } catch (t: Throwable) {
                Log.w(TAG, "createLocalMedia: capture init failed: ${t.message}, attempting fallback")
                // fallback attempt: try lower resolution
                try {
                    val fallback = Triple(320, 240, 15)
                    lastCaptureParams = fallback
                    videoCapturer.startCapture(fallback.first, fallback.second, fallback.third)
                    localVideoTrack = peerConnectionFactory?.createVideoTrack("ARDAMSv0", videoSource)
                    localRenderer?.let { renderer ->
                        initRenderer(renderer)
                        localVideoTrack?.addSink(renderer)
                    }
                    Log.d(TAG, "Fallback capture started: ${fallback.first}x${fallback.second}@${fallback.third}")
                } catch (t2: Throwable) {
                    Log.e(TAG, "Fallback capture failed: ${t2.message}")
                }
            }
        }

        peerConnection?.addTrack(localAudioTrack)
        localVideoTrack?.let {
            val sender = peerConnection?.addTrack(it)
            videoSender = sender
        }
        Log.d(TAG, "Local tracks added to PeerConnection. audio=${localAudioTrack!=null} video=${localVideoTrack!=null}")
    }

    fun setVideoMaxBitrate(kbps: Int) {
        try {
            val sender = videoSender ?: return
            val params = sender.parameters
            params.encodings.forEach { encoding ->
                encoding.maxBitrateBps = if (kbps > 0) kbps * 1000 else null
            }
            sender.parameters = params
            Log.d(TAG, "Set video max bitrate to ${kbps}kbps")
        } catch (t: Throwable) {
            Log.w(TAG, "setVideoMaxBitrate failed: ${t.message}")
        }
    }

    fun collectStats(callback: (RTCStatsReport) -> Unit) {
        try {
            peerConnection?.getStats { report ->
                callback(report)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "collectStats failed: ${t.message}")
        }
    }

    fun restartCaptureWithLastParams() {
        val capturer = videoCapturer ?: return
        val params = lastCaptureParams ?: return
        try {
            capturer.stopCapture()
        } catch (_: Exception) {}
        try {
            capturer.startCapture(params.first, params.second, params.third)
            Log.d(TAG, "restartCaptureWithLastParams: started ${params.first}x${params.second}@${params.third}")
        } catch (t: Throwable) {
            Log.w(TAG, "restartCaptureWithLastParams failed: ${t.message}")
        }
    }

    fun createScreenCapturer(data: Intent): VideoCapturer? {
        return try {
            ScreenCapturerAndroid(data, object : MediaProjection.Callback() {})
        } catch (t: Throwable) {
            Log.w(TAG, "createScreenCapturer failed: ${t.message}")
            null
        }
    }

    fun startScreenShare(screenCapturer: VideoCapturer?) {
        if (screenCapturer == null) return
        try {
            // stop camera capturer if present
            videoCapturer?.let {
                try { it.stopCapture() } catch (_: Exception) {}
                try { it.dispose() } catch (_: Exception) {}
            }
            videoCapturer = screenCapturer
            val videoSource: VideoSource? = peerConnectionFactory?.createVideoSource(screenCapturer.isScreencast)
            val surfaceTextureHelper = SurfaceTextureHelper.create("ScreenCaptureThread", eglBase.eglBaseContext)
            screenCapturer.initialize(surfaceTextureHelper, context, videoSource?.capturerObserver)
            screenCapturer.startCapture(720, 1280, 15)
            localVideoTrack = peerConnectionFactory?.createVideoTrack("ARDAMSv0", videoSource)
            localRenderer?.let { renderer ->
                initRenderer(renderer)
                localVideoTrack?.addSink(renderer)
            }
            // replace sender track
            val sender = videoSender
            sender?.setTrack(localVideoTrack, true)
            Log.d(TAG, "Started screen share")
        } catch (t: Throwable) {
            Log.w(TAG, "startScreenShare failed: ${t.message}")
        }
    }

    fun stopScreenShare() {
        try {
            videoCapturer?.let {
                try { it.stopCapture() } catch (_: Exception) {}
                try { it.dispose() } catch (_: Exception) {}
            }
            videoCapturer = null
            localVideoTrack?.let { track ->
                // keep track object; application can create camera capturer later
                Log.d(TAG, "Stopped screen share, local track remains")
            }
        } catch (t: Throwable) {
            Log.w(TAG, "stopScreenShare failed: ${t.message}")
        }
    }

    fun createOffer(observer: SdpObserver, constraints: MediaConstraints = MediaConstraints()) {
        Log.d(TAG, "createOffer() called")
        peerConnection?.createOffer(object : SdpObserver by observer {}, constraints)
    }

    fun createAnswer(observer: SdpObserver, constraints: MediaConstraints = MediaConstraints()) {
        Log.d(TAG, "createAnswer() called")
        peerConnection?.createAnswer(object : SdpObserver by observer {}, constraints)
    }

    fun setLocalDescription(sdp: SessionDescription, observer: SdpObserver) {
        Log.d(TAG, "setLocalDescription(type=${sdp.type})")
        peerConnection?.setLocalDescription(object : SdpObserver by observer {}, sdp)
    }

    fun setRemoteDescription(sdp: SessionDescription, observer: SdpObserver) {
        Log.d(TAG, "setRemoteDescription(type=${sdp.type})")
        peerConnection?.setRemoteDescription(object : SdpObserver by observer {}, sdp)
    }

    fun addIceCandidate(candidate: IceCandidate) {
        Log.d(TAG, "addIceCandidate: sdpMid=${candidate.sdpMid} index=${candidate.sdpMLineIndex}")
        peerConnection?.addIceCandidate(candidate)
    }

    fun setLocalRenderer(renderer: SurfaceViewRenderer) {
        localRenderer = renderer
        initRenderer(renderer)
        localVideoTrack?.addSink(renderer)
    }

    fun setRemoteRenderer(renderer: SurfaceViewRenderer) {
        remoteRenderer = renderer
        initRenderer(renderer)
        remoteVideoTrack?.addSink(renderer)
    }

    fun switchCamera() {
        // This SDK fork does not expose a stable camera-switch API on the capturer type.
    }

    fun setMicrophoneEnabled(enabled: Boolean) {
        localAudioTrack?.setEnabled(enabled)
    }

    fun setCameraEnabled(enabled: Boolean) {
        localVideoTrack?.setEnabled(enabled)
    }

    fun notifyRemoteVideoTrack(track: VideoTrack?) {
        remoteVideoTrack = track
        remoteRenderer?.let { renderer ->
            initRenderer(renderer)
            remoteVideoTrack?.addSink(renderer)
        }
    }

    fun createFrontCameraCapturer(): VideoCapturer? {
        val enumerator = Camera2Enumerator(context)
        val deviceName = enumerator.deviceNames.firstOrNull { enumerator.isFrontFacing(it) }
            ?: enumerator.deviceNames.firstOrNull()
            ?: return null
        return enumerator.createCapturer(deviceName, null)
    }

    private fun initRenderer(renderer: SurfaceViewRenderer) {
        try {
            renderer.init(eglBase.eglBaseContext, null)
            renderer.setEnableHardwareScaler(true)
            renderer.setMirror(true)
        } catch (e: Exception) {
            Log.w("WebRtcClient", "Renderer init failed: ${e.message}")
        }
    }

    fun close() {
        try {
            videoCapturer?.stopCapture()
        } catch (_: Exception) {
        }
        try {
            videoCapturer?.dispose()
        } catch (_: Exception) {
        }
        videoCapturer = null
        try {
            localRenderer?.clearImage()
            remoteRenderer?.clearImage()
        } catch (_: Exception) {
        }
        localRenderer = null
        remoteRenderer = null
        localVideoTrack = null
        localAudioTrack = null
        remoteVideoTrack = null
        peerConnection?.close()
        peerConnection = null
        peerConnectionFactory?.dispose()
        peerConnectionFactory = null
        initialized = false
    }
}
