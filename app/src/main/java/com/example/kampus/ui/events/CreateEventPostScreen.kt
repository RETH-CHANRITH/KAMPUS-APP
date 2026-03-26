package com.example.kampus.ui.events

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kampus.ui.groups.GroupColors
import com.example.kampus.ui.theme.Primary
import com.example.kampus.ui.theme.TextPrimary
import com.example.kampus.ui.theme.TextSecondary
import com.example.kampus.ui.theme.TextTertiary

data class EventPostAttachment(
    val uri: Uri,
    val type: MediaType,
    val displayName: String,
)

@Composable
fun CreateEventPostScreen(
    onBack: () -> Unit,
    onPost: (text: String, attachments: List<EventPostAttachment>, audience: EventVisibility) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    var audience by remember { mutableStateOf(EventVisibility.PUBLIC) }
    var attachments by remember { mutableStateOf(emptyList<EventPostAttachment>()) }

    val context = androidx.compose.ui.platform.LocalContext.current

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            attachments = attachments + EventPostAttachment(uri, MediaType.PHOTO, uri.getFileName(context))
        }
    }
    val pickVideo = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            attachments = attachments + EventPostAttachment(uri, MediaType.VIDEO, uri.getFileName(context))
        }
    }
    val pickDoc = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            attachments = attachments + EventPostAttachment(uri, MediaType.DOCUMENT, uri.getFileName(context))
        }
    }

    val canPost = text.isNotBlank() || attachments.isNotEmpty()

    Surface(color = GroupColors.Bg) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(GroupColors.Surface)
                        .border(1.dp, GroupColors.Border, CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onBack,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text("Create Event Post", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Share updates for your event", color = TextSecondary, fontSize = 12.sp)
                }

                AudiencePill(audience = audience, onToggle = {
                    audience = when (audience) {
                        EventVisibility.PUBLIC -> EventVisibility.FRIENDS
                        EventVisibility.FRIENDS -> EventVisibility.PRIVATE
                        EventVisibility.PRIVATE -> EventVisibility.PUBLIC
                    }
                })
            }

            HorizontalDivider(color = GroupColors.Border.copy(alpha = 0.55f))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp)
                    .padding(top = 16.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = GroupColors.Card,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, GroupColors.Border, RoundedCornerShape(16.dp)),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Write an update…", color = TextTertiary) },
                            minLines = 4,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = GroupColors.Border,
                                focusedContainerColor = GroupColors.Surface,
                                unfocusedContainerColor = GroupColors.Surface,
                                cursorColor = Primary,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedPlaceholderColor = TextTertiary,
                                unfocusedPlaceholderColor = TextTertiary,
                            ),
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Default),
                        )

                        AnimatedVisibility(visible = attachments.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                            AttachmentStrip(
                                attachments = attachments,
                                onRemove = { toRemove -> attachments = attachments - toRemove },
                            )
                        }
                    }
                }

                Text("ADD", color = TextTertiary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(GroupColors.Card)
                        .border(1.dp, GroupColors.Border, RoundedCornerShape(16.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ActionChip(label = "Photo", icon = Icons.Default.Image, onClick = { pickPhoto.launch("image/*") })
                    ActionChip(label = "Video", icon = Icons.Default.Videocam, onClick = { pickVideo.launch("video/*") })
                    ActionChip(label = "Doc", icon = Icons.Default.AttachFile, onClick = { pickDoc.launch("*/*") })
                    Spacer(Modifier.weight(1f))
                    ActionChip(label = "Tag", icon = Icons.Default.Tag, enabled = false, onClick = {})
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GroupColors.Bg)
                    .padding(horizontal = 18.dp, vertical = 14.dp)
                    .navigationBarsPadding(),
            ) {
                Button(
                    onClick = { onPost(text.trim(), attachments, audience) },
                    enabled = canPost,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary,
                        contentColor = Color.White,
                        disabledContainerColor = GroupColors.Surface,
                        disabledContentColor = TextTertiary,
                    ),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                ) {
                    Text("Post", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AudiencePill(audience: EventVisibility, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(GroupColors.Surface)
            .border(1.dp, GroupColors.Border, RoundedCornerShape(999.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle,
            )
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.Default.Public, null, tint = Primary, modifier = Modifier.size(16.dp))
        Text(audience.label, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val bg = if (enabled) GroupColors.Surface else GroupColors.Surface.copy(alpha = 0.5f)
    val border = if (enabled) GroupColors.Border else GroupColors.Border.copy(alpha = 0.5f)
    val tint = if (enabled) TextPrimary else TextTertiary
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, null, tint = Primary, modifier = Modifier.size(16.dp))
        Text(label, color = tint, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun AttachmentStrip(
    attachments: List<EventPostAttachment>,
    onRemove: (EventPostAttachment) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        attachments.forEach { att ->
            AttachmentChip(att = att, onRemove = { onRemove(att) })
        }
    }
}

@Composable
private fun AttachmentChip(att: EventPostAttachment, onRemove: () -> Unit) {
    val icon = when (att.type) {
        MediaType.PHOTO -> Icons.Default.Image
        MediaType.VIDEO -> Icons.Default.Movie
        MediaType.DOCUMENT -> Icons.Default.AttachFile
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Primary.copy(alpha = 0.16f))
            .border(1.dp, Primary.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, null, tint = Primary, modifier = Modifier.size(16.dp))
        Text(
            text = if (att.displayName.length > 22) att.displayName.take(22) + "…" else att.displayName,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(GroupColors.Surface)
                .border(1.dp, GroupColors.Border, CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onRemove,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Close, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
        }
    }
}
