package com.example.kampus.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ABg = Color(0xFF1A1D2E)
private val ACard = Color(0xFF252A41)
private val ABorder = Color(0xFF364153)
private val AWhite = Color(0xFFFFFFFF)
private val ASubtle = Color(0xFF99A1AF)
private val ABlue = Color(0xFF0D7FFF)
private val ADanger = Color(0xFFFB2C36)

@Composable
fun AccountSettingsScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Get real email from Firebase Auth
    val realEmail = FirebaseAuth.getInstance().currentUser?.email ?: state.email
    
    // Format account creation date
    val createdDate = FirebaseAuth.getInstance().currentUser?.metadata?.creationTimestamp?.let { timestamp ->
        val sdf = SimpleDateFormat("MMMM dd, yyyy", Locale.ENGLISH)
        sdf.format(Date(timestamp))
    } ?: "N/A"
    
    // Get phone from profile state
    val phone = state.phone.ifEmpty { "Not set" }
    
    Surface(color = ABg, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ACard)
                        .border(1.dp, ABorder, CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = AWhite)
                }
                Text("Account", color = AWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Box(Modifier.size(40.dp))
            }

            Section(title = "Account Information") {
                InfoRow(label = "Email", value = realEmail)
                InfoRow(label = "Phone", value = phone)
                InfoRow(label = "Account Created", value = createdDate, showDivider = false)
            }

            Section(title = "Linked Accounts") {
                LinkedRow(
                    tag = "f",
                    tagBackground = Color(0xFF2B7FFF),
                    name = "Facebook",
                    status = "Connected",
                    action = "Disconnect",
                    actionColor = ADanger,
                )
                LinkedRow(
                    tag = "@",
                    tagBackground = Brush.horizontalGradient(listOf(Color(0xFFAD46FF), Color(0xFFF6339A))),
                    name = "Instagram",
                    status = "Not connected",
                    action = "Connect",
                    actionColor = ABlue,
                )
                LinkedRow(
                    tag = "X",
                    tagBackground = Color.Black,
                    name = "X (Twitter)",
                    status = "Not connected",
                    action = "Connect",
                    actionColor = ABlue,
                    showDivider = false,
                )
            }

            Section(title = "Account Actions") {
                ActionRow(
                    title = "Deactivate Account",
                    subtitle = "Temporarily disable your account",
                    showDivider = false,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Danger Zone", color = ADanger, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ADanger.copy(alpha = 0.1f))
                        .border(1.dp, ADanger.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = ADanger,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "Permanently delete your account and data",
                        color = Color(0xFFFF6467),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = AWhite, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(ACard)
                .border(1.dp, ABorder, RoundedCornerShape(14.dp)),
        ) {
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, showDivider: Boolean = true) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
        Text(label, color = ASubtle, fontSize = 14.sp)
        Text(value, color = AWhite, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .background(ABorder)
                    .size(width = 1.dp, height = 1.dp),
            )
        }
    }
}

@Composable
private fun LinkedRow(
    tag: String,
    tagBackground: Color,
    name: String,
    status: String,
    action: String,
    actionColor: Color,
    showDivider: Boolean = true,
) {
    LinkedRowContent(
        tag = tag,
        tagBrush = null,
        tagColor = tagBackground,
        name = name,
        status = status,
        action = action,
        actionColor = actionColor,
        showDivider = showDivider,
    )
}

@Composable
private fun LinkedRow(
    tag: String,
    tagBackground: Brush,
    name: String,
    status: String,
    action: String,
    actionColor: Color,
    showDivider: Boolean = true,
) {
    LinkedRowContent(
        tag = tag,
        tagBrush = tagBackground,
        tagColor = Color.Transparent,
        name = name,
        status = status,
        action = action,
        actionColor = actionColor,
        showDivider = showDivider,
    )
}

@Composable
private fun LinkedRowContent(
    tag: String,
    tagBrush: Brush?,
    tagColor: Color,
    name: String,
    status: String,
    action: String,
    actionColor: Color,
    showDivider: Boolean,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(tagColor)
                        .let { base ->
                            if (tagBrush != null) base.background(tagBrush) else base
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(tag, color = AWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Column {
                    Text(name, color = AWhite, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text(status, color = ASubtle, fontSize = 14.sp)
                }
            }
            Text(
                text = action,
                color = actionColor,
                textAlign = TextAlign.End,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { },
            )
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .background(ABorder)
                    .size(width = 1.dp, height = 1.dp),
            )
        }
    }
}

@Composable
private fun ActionRow(title: String, subtitle: String, showDivider: Boolean = true) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = AWhite, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text(subtitle, color = ASubtle, fontSize = 13.sp)
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = ASubtle,
            )
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .background(ABorder)
                    .size(width = 1.dp, height = 1.dp),
            )
        }
    }
}