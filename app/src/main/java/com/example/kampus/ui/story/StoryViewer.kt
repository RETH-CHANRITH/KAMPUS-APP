@file:Suppress("UNUSED_PARAMETER", "UNUSED_VARIABLE", "NAME_SHADOWING")

package com.example.kampus.ui.story

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.layout.ContentScale
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

/**
 * Minimal Story viewer UI skeleton. Integrate ExoPlayer for video support and
 * add progress bars, gestures, and reply UI in real implementation.
 */
@Composable
fun StoryViewerScreen(
    storyId: String,
    imageUrl: String,
    ownerName: String,
    timestampLabel: String,
    onClose: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onReply: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val isVideo = imageUrl.endsWith(".mp4", ignoreCase = true) || imageUrl.endsWith(".webm", ignoreCase = true) || imageUrl.endsWith(".m3u8", ignoreCase = true)
    val closeAction = onClose
    val previousAction = onPrevious
    val replyAction = onReply

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        val context = LocalContext.current
        if (isVideo) {
            val player = ExoPlayer.Builder(context).build().apply {
                val item = MediaItem.fromUri(imageUrl)
                setMediaItem(item)
                prepare()
                playWhenReady = true
            }

            DisposableEffect(key1 = player) {
                onDispose { player.release() }
            }

            AndroidView(factory = {
                PlayerView(context).apply {
                    useController = false
                    this.player = player
                }
            }, modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { _ -> scope.launch { onNext() } },
                        onLongPress = { player.playWhenReady = !player.playWhenReady },
                    )
                }
            )
        } else {
            AsyncImage(
                model = imageUrl,
                contentDescription = ownerName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { _ -> scope.launch { onNext() } },
                            onLongPress = { scope.launch { onPrevious() } },
                        )
                    },
            )
        }

        // Top bar: profile and timestamp
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
            Text(text = ownerName, color = Color.White)
            Text(text = timestampLabel, color = Color.LightGray)
        }

        Row(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)) {
            TextButton(onClick = previousAction) {
                Text(text = "Previous", color = Color.White)
            }
            TextButton(onClick = { replyAction(imageUrl) }) {
                Text(text = "Reply", color = Color.White)
            }
        }

        IconButton(
            onClick = closeAction,
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White,
            )
        }
    }

    // Record view once when opened
    LaunchedEffect(key1 = storyId) {
        try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                val now = System.currentTimeMillis()
                val ref = FirebaseFirestore.getInstance().collection("stories").document(storyId).collection("views").document(uid)
                ref.set(mapOf("viewerId" to uid, "seenAt" to now))
                // Increment viewersCount via transaction (best-effort)
                val storyRef = FirebaseFirestore.getInstance().collection("stories").document(storyId)
                FirebaseFirestore.getInstance().runTransaction { tx ->
                    val snap = tx.get(storyRef)
                    val current = (snap.getLong("viewersCount") ?: 0L)
                    tx.update(storyRef, "viewersCount", current + 1)
                }
            }
        } catch (_: Exception) {
        }
    }
}
