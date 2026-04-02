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
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val NSBg = Color(0xFF1A1D2E)
private val NSCard = Color(0xFF252A41)
private val NSBorder = Color(0xFF364153)
private val NSWhite = Color(0xFFFFFFFF)
private val NSSubtle = Color(0xFF99A1AF)
private val NSBlue = Color(0xFF0D7FFF)

@Composable
fun NotificationSettingsScreen(onBack: () -> Unit) {
    var pushEnabled by remember { mutableStateOf(true) }
    var likesEnabled by remember { mutableStateOf(true) }
    var commentsEnabled by remember { mutableStateOf(true) }
    var followersEnabled by remember { mutableStateOf(true) }
    var mentionsEnabled by remember { mutableStateOf(true) }
    var directMessagesEnabled by remember { mutableStateOf(true) }
    var groupActivityEnabled by remember { mutableStateOf(false) }
    var emailEnabled by remember { mutableStateOf(true) }
    var weeklyDigestEnabled by remember { mutableStateOf(false) }
    var smsEnabled by remember { mutableStateOf(false) }

    Surface(color = NSBg, modifier = Modifier.fillMaxSize()) {
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
                        .background(NSCard)
                        .border(1.dp, NSBorder, CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = NSWhite)
                }
                Text("Notifications", color = NSWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Box(Modifier.size(40.dp))
            }

            NotificationSection(title = "Push Notifications") {
                NotificationToggleRow(
                    title = "Push Notifications",
                    subtitle = "Enable all push notifications",
                    checked = pushEnabled,
                    onCheckedChange = { pushEnabled = it },
                )
                NotificationToggleRow(
                    title = "Likes",
                    subtitle = "When someone likes your post",
                    checked = likesEnabled,
                    onCheckedChange = { likesEnabled = it },
                )
                NotificationToggleRow(
                    title = "Comments",
                    subtitle = "When someone comments on your post",
                    checked = commentsEnabled,
                    onCheckedChange = { commentsEnabled = it },
                )
                NotificationToggleRow(
                    title = "New Followers",
                    subtitle = "When someone follows you",
                    checked = followersEnabled,
                    onCheckedChange = { followersEnabled = it },
                )
                NotificationToggleRow(
                    title = "Mentions",
                    subtitle = "When someone mentions you",
                    checked = mentionsEnabled,
                    onCheckedChange = { mentionsEnabled = it },
                )
                NotificationToggleRow(
                    title = "Direct Messages",
                    subtitle = "When you receive a new message",
                    checked = directMessagesEnabled,
                    onCheckedChange = { directMessagesEnabled = it },
                )
                NotificationToggleRow(
                    title = "Group Activity",
                    subtitle = "Updates from groups you're in",
                    checked = groupActivityEnabled,
                    onCheckedChange = { groupActivityEnabled = it },
                    showDivider = false,
                )
            }

            NotificationSection(title = "Email Notifications") {
                NotificationToggleRow(
                    title = "Email Notifications",
                    subtitle = "Receive email notifications",
                    checked = emailEnabled,
                    onCheckedChange = { emailEnabled = it },
                )
                NotificationToggleRow(
                    title = "Weekly Digest",
                    subtitle = "Get a weekly summary email",
                    checked = weeklyDigestEnabled,
                    onCheckedChange = { weeklyDigestEnabled = it },
                    showDivider = false,
                )
            }

            NotificationSection(title = "SMS Notifications") {
                NotificationToggleRow(
                    title = "SMS Notifications",
                    subtitle = "Receive important updates via SMS",
                    checked = smsEnabled,
                    onCheckedChange = { smsEnabled = it },
                    showDivider = false,
                )
            }
        }
    }
}

@Composable
private fun NotificationSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = NSWhite, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(NSCard)
                .border(1.dp, NSBorder, RoundedCornerShape(14.dp)),
        ) {
            content()
        }
    }
}

@Composable
private fun NotificationToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showDivider: Boolean = true,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = NSWhite, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text(subtitle, color = NSSubtle, fontSize = 13.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = NSWhite,
                    checkedTrackColor = NSBlue,
                    uncheckedThumbColor = NSWhite,
                    uncheckedTrackColor = Color(0xFF4A5565),
                ),
            )
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .background(NSBorder)
                    .size(width = 1.dp, height = 1.dp),
            )
        }
    }
}