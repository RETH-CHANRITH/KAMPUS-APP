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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kampus.ui.localization.rememberUiStrings

private val NSBg get() = ProfileColors.Bg
private val NSCard get() = ProfileColors.Card
private val NSBorder get() = ProfileColors.Border
private val NSWhite get() = ProfileColors.White
private val NSSubtle get() = ProfileColors.Subtle
private val NSBlue get() = ProfileColors.Blue
private val NSSwitchOffTrack get() = if (ProfileColors.Bg == Color(0xFFF3F4F8)) Color(0xFFD1D5DB) else Color(0xFF4A5565)

@Composable
fun NotificationSettingsScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel(),
) {
    val strings = rememberUiStrings()
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val settings = state.notificationSettings

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
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = strings.back, tint = NSWhite)
                }
                Text(strings.notificationsTitle, color = NSWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Box(Modifier.size(40.dp))
            }

            NotificationSection(title = strings.pushNotifications) {
                NotificationToggleRow(
                    title = strings.pushNotifications,
                    subtitle = "Enable all push notifications",
                    checked = settings.pushNotifications,
                    onCheckedChange = { viewModel.setPushNotificationsEnabled(it) },
                )
                NotificationToggleRow(
                    title = "Likes",
                    subtitle = "When someone likes your post",
                    checked = settings.likes,
                    onCheckedChange = { viewModel.setNotificationToggle("likes", it) },
                )
                NotificationToggleRow(
                    title = "Comments",
                    subtitle = "When someone comments on your post",
                    checked = settings.comments,
                    onCheckedChange = { viewModel.setNotificationToggle("comments", it) },
                )
                NotificationToggleRow(
                    title = "New Followers",
                    subtitle = "When someone follows you",
                    checked = settings.newFollowers,
                    onCheckedChange = { viewModel.setNotificationToggle("newFollowers", it) },
                )
                NotificationToggleRow(
                    title = "Mentions",
                    subtitle = "When someone mentions you",
                    checked = settings.mentions,
                    onCheckedChange = { viewModel.setNotificationToggle("mentions", it) },
                )
                NotificationToggleRow(
                    title = "Direct Messages",
                    subtitle = "When you receive a new message",
                    checked = settings.directMessages,
                    onCheckedChange = { viewModel.setNotificationToggle("directMessages", it) },
                )
                NotificationToggleRow(
                    title = "Group Activity",
                    subtitle = "Updates from groups you're in",
                    checked = settings.groupActivity,
                    onCheckedChange = { viewModel.setNotificationToggle("groupActivity", it) },
                    showDivider = false,
                )
            }

            NotificationSection(title = strings.emailNotifications) {
                NotificationToggleRow(
                    title = strings.emailNotifications,
                    subtitle = "Receive email notifications",
                    checked = settings.emailNotifications,
                    onCheckedChange = { viewModel.setEmailNotificationsEnabled(it) },
                )
                NotificationToggleRow(
                    title = "Weekly Digest",
                    subtitle = "Get a weekly summary email",
                    checked = settings.weeklyDigest,
                    onCheckedChange = { viewModel.setNotificationToggle("weeklyDigest", it) },
                    showDivider = false,
                )
            }

            NotificationSection(title = strings.smsNotifications) {
                NotificationToggleRow(
                    title = strings.smsNotifications,
                    subtitle = "Receive important updates via SMS",
                    checked = settings.smsNotifications,
                    onCheckedChange = { viewModel.setNotificationToggle("smsNotifications", it) },
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
                    uncheckedTrackColor = NSSwitchOffTrack,
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