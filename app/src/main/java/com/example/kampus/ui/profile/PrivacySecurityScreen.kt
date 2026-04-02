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

private val PSBg = Color(0xFF1A1D2E)
private val PSCard = Color(0xFF252A41)
private val PSBorder = Color(0xFF364153)
private val PSWhite = Color(0xFFFFFFFF)
private val PSSubtle = Color(0xFF99A1AF)
private val PSBlue = Color(0xFF0D7FFF)

@Composable
fun PrivacySecurityScreen(onBack: () -> Unit) {
    var privateAccount by remember { mutableStateOf(false) }
    var activityStatus by remember { mutableStateOf(true) }
    var allowTagging by remember { mutableStateOf(true) }
    var allowMentions by remember { mutableStateOf(true) }
    var twoFactor by remember { mutableStateOf(false) }

    Surface(color = PSBg, modifier = Modifier.fillMaxSize()) {
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
                        .background(PSCard)
                        .border(1.dp, PSBorder, CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = PSWhite)
                }
                Text("Privacy & Security", color = PSWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Box(Modifier.size(40.dp))
            }

            SectionCard(title = "Privacy") {
                ToggleRow(
                    title = "Private Account",
                    subtitle = "Only followers can see your posts",
                    checked = privateAccount,
                    onCheckedChange = { privateAccount = it },
                )
                ToggleRow(
                    title = "Activity Status",
                    subtitle = "Show when you're active",
                    checked = activityStatus,
                    onCheckedChange = { activityStatus = it },
                )
                ToggleRow(
                    title = "Allow Tagging",
                    subtitle = "Let others tag you in photos",
                    checked = allowTagging,
                    onCheckedChange = { allowTagging = it },
                )
                ToggleRow(
                    title = "Allow Mentions",
                    subtitle = "Let others mention you",
                    checked = allowMentions,
                    onCheckedChange = { allowMentions = it },
                    showDivider = false,
                )
            }

            SectionCard(title = "Security") {
                ToggleRow(
                    title = "Two-Factor Authentication",
                    subtitle = "Add extra security to your account",
                    checked = twoFactor,
                    onCheckedChange = { twoFactor = it },
                )
                ActionRow(
                    title = "Change Password",
                    subtitle = "Update your password",
                )
                ActionRow(
                    title = "Login Activity",
                    subtitle = "Review recent logins",
                    showDivider = false,
                )
            }

            SectionCard(title = "Data & History") {
                ActionRow(
                    title = "Download Your Data",
                    subtitle = "Get a copy of your data",
                )
                ActionRow(
                    title = "Search History",
                    subtitle = "View and clear searches",
                    showDivider = false,
                )
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = PSWhite, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(PSCard)
                .border(1.dp, PSBorder, RoundedCornerShape(14.dp)),
        ) {
            content()
        }
    }
}

@Composable
private fun ToggleRow(
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
                Text(title, color = PSWhite, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text(subtitle, color = PSSubtle, fontSize = 13.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PSWhite,
                    checkedTrackColor = PSBlue,
                    uncheckedThumbColor = PSWhite,
                    uncheckedTrackColor = Color(0xFF4A5565),
                ),
            )
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .background(PSBorder)
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
                Text(title, color = PSWhite, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text(subtitle, color = PSSubtle, fontSize = 13.sp)
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = PSSubtle,
            )
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .background(PSBorder)
                    .size(width = 1.dp, height = 1.dp),
            )
        }
    }
}