package com.example.kampus.ui.chat

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import kotlin.collections.isNotEmpty

// ─── Colour tokens ────────────────────────────────────────────────────────────
private val BgDeep          = Color(0xFF070D18)
private val HeaderBg        = Color(0xFF0C1627)
private val InputBg         = Color(0xFF0C1627)
private val InputFieldBg    = Color(0xFF111C2C)
private val AccentBlue      = Color(0xFF3B82F6)
private val TextPrimary     = Color(0xFFEFF3FF)
private val TextSecondary   = Color(0xFF6B7A99)
private val OnlineGreen     = Color(0xFF22C55E)
private val IconButton      = Color(0xFF1A2540)

@Composable
fun ChatScreen(
    chatId    : Int,
    onBack    : () -> Unit,
    viewModel : ChatViewModel = viewModel(),
) {
    LaunchedEffect(chatId) { viewModel.openChat(chatId) }
    val state        by viewModel.chatState.collectAsStateWithLifecycle()
    val listState    = rememberLazyListState()
    val scope        = rememberCoroutineScope()

    // Scroll to bottom when new message added
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(state.messages.size - 1)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // ── Top bar ────────────────────────────────────────────────────────
        ChatTopBar(
            contactName = state.contactName,
            isOnline    = state.isOnline,
            onBack      = onBack,
        )

        // ── Message list ───────────────────────────────────────────────────
        LazyColumn(
            state           = listState,
            modifier        = Modifier.weight(1f),
            contentPadding  = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(state.messages, key = { it.id }) { msg ->
                AnimatedVisibility(
                    visible = true,
                    enter   = slideInHorizontally(
                        initialOffsetX = { if (msg.isSentByMe) it else -it }
                    ) + fadeIn(),
                ) {
                    MessageBubble(message = msg)
                }
            }
        }

        // ── Input bar ──────────────────────────────────────────────────────
        ChatInputBar(
            text          = state.inputText,
            onTextChange  = viewModel::onInputChange,
            onSend        = {
                viewModel.sendMessage()
                scope.launch {
                    listState.animateScrollToItem(
                        (state.messages.size).coerceAtLeast(0)
                    )
                }
            },
        )
    }
}

// ─── Top bar ─────────────────────────────────────────────────────────────────
@Composable
private fun ChatTopBar(
    contactName : String,
    isOnline    : Boolean,
    onBack      : () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(HeaderBg, HeaderBg.copy(alpha = 0.95f))
                )
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        // Back button
        IconButton(onClick = onBack) {
            Icon(
                imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint               = TextPrimary,
            )
        }

        Spacer(Modifier.width(4.dp))

        // Avatar
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(AccentBlue.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text       = contactName.take(2).uppercase(),
                color      = AccentBlue,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.width(10.dp))

        // Name & status
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = contactName,
                color      = TextPrimary,
                fontSize   = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                if (isOnline) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(OnlineGreen),
                    )
                }
                Text(
                    text     = if (isOnline) "Online" else "Offline",
                    color    = if (isOnline) OnlineGreen else TextSecondary,
                    fontSize = 12.sp,
                )
            }
        }

        // Action icons
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ActionIconBtn(icon = Icons.Default.Call,     description = "Call")
            ActionIconBtn(icon = Icons.Default.Videocam, description = "Video")
        }
    }

    // Thin divider
    HorizontalDivider(color = Color(0xFF1A2540), thickness = 0.5.dp)
}

@Composable
private fun ActionIconBtn(
    icon        : androidx.compose.ui.graphics.vector.ImageVector,
    description : String,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(IconButton),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = description,
            tint               = TextPrimary,
            modifier           = Modifier.size(18.dp),
        )
    }
}

// ─── Input bar ────────────────────────────────────────────────────────────────
@Composable
private fun ChatInputBar(
    text         : String,
    onTextChange : (String) -> Unit,
    onSend       : () -> Unit,
) {
    HorizontalDivider(color = Color(0xFF1A2540), thickness = 0.5.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(InputBg)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // + button
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(IconButton),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Attach",
                tint               = TextSecondary,
                modifier           = Modifier.size(20.dp),
            )
        }

        // Mic button
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(IconButton),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Mic,
                contentDescription = "Voice",
                tint               = TextSecondary,
                modifier           = Modifier.size(20.dp),
            )
        }

        // Text field
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(InputFieldBg)
                .padding(horizontal = 16.dp, vertical = 2.dp),
        ) {
            if (text.isEmpty()) {
                Text(
                    "Write now…",
                    color    = TextSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.align(Alignment.CenterStart).padding(vertical = 10.dp),
                )
            }
            TextField(
                value         = text,
                onValueChange = onTextChange,
                singleLine    = false,
                maxLines      = 4,
                colors        = TextFieldDefaults.colors(
                    unfocusedContainerColor  = Color.Transparent,
                    focusedContainerColor    = Color.Transparent,
                    unfocusedIndicatorColor  = Color.Transparent,
                    focusedIndicatorColor    = Color.Transparent,
                    unfocusedTextColor       = TextPrimary,
                    focusedTextColor         = TextPrimary,
                    cursorColor              = AccentBlue,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Send button
        AnimatedVisibility(
            visible = text.isNotBlank(),
            enter   = scaleIn() + fadeIn(),
            exit    = scaleOut() + fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(AccentBlue, Color(0xFF6D28D9))
                        )
                    )
                    .clickableNoRipple { onSend() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint               = Color.White,
                    modifier           = Modifier.size(20.dp),
                )
            }
        }
    }
}

// ─── Helper: clickable without ripple ─────────────────────────────────────────
private fun Modifier.clickableNoRipple(onClick: () -> Unit) =
    this.then(
        Modifier.clickable(
            interactionSource = null,
            indication        = null,
            onClick           = onClick,
        )
    )