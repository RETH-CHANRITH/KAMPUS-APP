package com.example.kampus.call

import android.content.Context
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoCapturer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack

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
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localAudioTrack: AudioTrack? = null
    private var localVideoTrack: VideoTrack? = null

    fun initialize() {
        val options = PeerConnectionFactory.InitializationOptions.builder(context).createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        val encoderFactory = DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
        val decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()
    }

    fun createPeerConnection(iceServers: List<PeerConnection.IceServer>, observer: PeerConnection.Observer) {
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, observer)
    }

    fun createLocalMedia(videoCapturer: VideoCapturer?) {
        val audioSource: AudioSource? = peerConnectionFactory?.createAudioSource(MediaConstraints())
        localAudioTrack = peerConnectionFactory?.createAudioTrack("ARDAMSa0", audioSource)

        if (videoCapturer != null) {
            val videoSource: VideoSource? = peerConnectionFactory?.createVideoSource(videoCapturer.isScreencast)
            videoCapturer.initialize(org.webrtc.SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext), context, videoSource?.capturerObserver)
            videoCapturer.startCapture(640, 480, 30)
            localVideoTrack = peerConnectionFactory?.createVideoTrack("ARDAMSv0", videoSource)
        }
    }

    fun createOffer(observer: SdpObserver, constraints: MediaConstraints = MediaConstraints()) {
        peerConnection?.createOffer(object : SdpObserver by observer {} , constraints)
    }

    fun setRemoteDescription(sdp: SessionDescription, observer: SdpObserver) {
        peerConnection?.setRemoteDescription(object : SdpObserver by observer {} , sdp)
    }

    fun addIceCandidate(candidate: IceCandidate) {
        peerConnection?.addIceCandidate(candidate)
    }

    fun close() {
        localVideoTrack = null
        localAudioTrack = null
        peerConnection?.close()
        peerConnection = null
        peerConnectionFactory = null
    }
}
