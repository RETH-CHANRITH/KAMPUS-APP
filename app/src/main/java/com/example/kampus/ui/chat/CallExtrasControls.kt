package com.example.kampus.ui.chat

import android.content.Context
import android.app.Activity.RESULT_OK
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.automirrored.filled.ScreenShare
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kampus.call.CallViewModel

@Composable
fun CallExtrasControls(
    modifier: Modifier = Modifier,
    viewModel: CallViewModel = viewModel(),
    isMuted: Boolean,
    speakerOn: Boolean,
    cameraEnabled: Boolean,
    isVideo: Boolean,
    onEndCall: () -> Unit,
) {
    val context = LocalContext.current
    val mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
    var bitrate by remember { mutableFloatStateOf(1200f) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            viewModel.startScreenShareWithIntent(result.data)
        }
    }

    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.10f),
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 10.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.06f)),
                    ),
                )
                .border(1.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(28.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Live controls",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.HighQuality, contentDescription = null, tint = Color.White.copy(alpha = 0.72f), modifier = Modifier.size(16.dp))
                    Text(text = "${bitrate.toInt()} kbps", color = Color.White.copy(alpha = 0.78f), fontSize = 11.sp)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CompactAction(
                    icon = if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                    tint = if (isMuted) Color(0xFFF97316) else Color.White,
                    onClick = viewModel::toggleMute,
                )
                CompactAction(
                    icon = Icons.Filled.Speaker,
                    tint = if (speakerOn) Color(0xFF22C55E) else Color.White,
                    onClick = viewModel::toggleSpeaker,
                )
                if (isVideo) {
                    CompactAction(
                        icon = if (cameraEnabled) Icons.Filled.Videocam else Icons.Filled.VideocamOff,
                        tint = if (cameraEnabled) Color(0xFF60A5FA) else Color.White,
                        onClick = viewModel::toggleCamera,
                    )
                    CompactAction(
                        icon = Icons.AutoMirrored.Filled.ScreenShare,
                        tint = Color(0xFF38BDF8),
                        onClick = {
                            mediaProjectionManager?.let { manager ->
                                launcher.launch(manager.createScreenCaptureIntent())
                            }
                        },
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "Adaptive bitrate", color = Color.White.copy(alpha = 0.82f), fontSize = 11.sp)
                Slider(
                    value = bitrate,
                    onValueChange = {
                        bitrate = it
                        viewModel.setVideoBitrate(it.toInt())
                    },
                    valueRange = 250f..2500f,
                )

                Surface(
                    onClick = onEndCall,
                    color = Color(0xFFEF4444),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.FiberManualRecord, contentDescription = null, tint = Color.White)
                        Text(text = "Hang up", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = Color.White.copy(alpha = 0.08f),
        shape = CircleShape,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier.size(52.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(52.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = tint)
        }
    }
}
