package com.example.kampus.ui.chat

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kampus.ui.chat.Message

// ─── Colour tokens ───────────────────────────────────────────────────────────
private val BubbleSent      = Color(0xFF3E6BCA)   // blue-ish sent
private val BubbleReceived  = Color(0xFF1E2A3B)   // dark navy received
private val TimestampColor  = Color(0xFF6B7A99)

// ─── Voice waveform bar heights (static decoration) ──────────────────────────
private val waveHeights = listOf(
    0.4f, 0.7f, 0.5f, 1.0f, 0.6f, 0.8f, 0.5f, 0.9f,
    0.4f, 0.7f, 0.6f, 0.8f, 0.5f, 0.3f, 0.7f, 0.9f,
    0.6f, 0.4f, 0.8f, 0.5f,
)

@Composable
fun MessageBubble(
    message: Message,
    modifier: Modifier = Modifier,
) {
    val horizontalArrangement =
        if (message.isSentByMe) Arrangement.End else Arrangement.Start

    Row(
        modifier           = modifier.fillMaxWidth(),
        horizontalArrangement = horizontalArrangement,
    ) {
        if (message.isVoice) {
            VoiceBubble(message)
        } else {
            TextBubble(message)
        }
    }
}

// ─── Text bubble ─────────────────────────────────────────────────────────────
@Composable
private fun TextBubble(message: Message) {
    val bubbleColor = if (message.isSentByMe) BubbleSent else BubbleReceived
    val shape = if (message.isSentByMe)
        RoundedCornerShape(18.dp, 4.dp, 18.dp, 18.dp)
    else
        RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp)

    Column(horizontalAlignment = if (message.isSentByMe) Alignment.End else Alignment.Start) {
        Box(
            modifier = Modifier
                .widthIn(max = 270.dp)
                .clip(shape)
                .background(bubbleColor)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text       = message.text,
                color      = Color.White,
                fontSize   = 15.sp,
                lineHeight = 22.sp,
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text     = message.timestamp,
            color    = TimestampColor,
            fontSize = 11.sp,
        )
    }
}

// ─── Voice bubble ─────────────────────────────────────────────────────────────
@Composable
private fun VoiceBubble(message: Message) {
    var playing by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (playing) 1f else 0f,
        animationSpec = tween(durationMillis = 3000),
        label = "voice_progress",
    )

    Column(horizontalAlignment = Alignment.Start) {
        Box(
            modifier = Modifier
                .widthIn(max = 260.dp)
                .clip(RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp))
                .background(BubbleReceived)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Play button
                IconButton(
                    onClick  = { playing = !playing },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF3E6BCA), shape = RoundedCornerShape(50)),
                ) {
                    Icon(
                        imageVector        = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint               = Color.White,
                        modifier           = Modifier.size(20.dp),
                    )
                }

                // Waveform bars
                Row(
                    modifier            = Modifier.height(36.dp),
                    verticalAlignment   = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    waveHeights.forEachIndexed { index, height ->
                        val fraction = index.toFloat() / waveHeights.size
                        val isPlayed = progress > fraction
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .fillMaxHeight(height)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (isPlayed) Color(0xFF60A5FA) else Color(0xFF3A4A65)
                                )
                        )
                    }
                }

                if (message.voiceDuration.isNotBlank()) {
                    Text(
                        text     = message.voiceDuration,
                        color    = TimestampColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text     = message.timestamp,
            color    = TimestampColor,
            fontSize = 11.sp,
        )
    }
}