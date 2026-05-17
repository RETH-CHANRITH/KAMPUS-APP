package com.example.kampus.call

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer

/**
 * Small utility to play/stop a ringtone from raw resources.
 * Add a ringtone file under `res/raw/ringtone_incoming.mp3` to use.
 */
object RingtonePlayer {
    private var player: MediaPlayer? = null

    fun play(context: Context, resId: Int) {
        stop()
        try {
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build()
                )
                setDataSource(context.resources.openRawResourceFd(resId))
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            player = null
        }
    }

    fun stop() {
        try {
            player?.stop()
            player?.release()
        } catch (ignored: Exception) {
        }
        player = null
    }
}
