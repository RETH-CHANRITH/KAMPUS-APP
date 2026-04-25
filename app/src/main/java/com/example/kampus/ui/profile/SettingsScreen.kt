package com.example.kampus.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

private val SBg = Color(0xFF1A1D2E)
private val SCard = Color(0xFF252A41)
private val SBorder = Color(0xFF364153)
private val SWhite = Color(0xFFFFFFFF)
private val SGray2 = Color(0xFFD1D5DC)
private val SGray4 = Color(0xFF9CA3AF)
private val SDanger = Color(0xFFFB2C36)

private data class SettingsMenuItem(
    val title: String,
    val icon: ImageVector,
    val tintBg: Color,
    val onClick: () -> Unit,
)

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenAccountSettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenPrivacySecurity: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenBlockedUsers: () -> Unit,
    onOpenHelpSupport: () -> Unit,
    onOpenAbout: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = viewModel(),
) {
    val menuItems = listOf(
        SettingsMenuItem("Account", Icons.Outlined.Edit, Color(0xFF0D7FFF).copy(alpha = 0.13f), onClick = onOpenAccountSettings),
        SettingsMenuItem("Notifications", Icons.Outlined.Notifications, Color(0xFFFF4D6D).copy(alpha = 0.13f), onClick = onOpenNotificationSettings),
        SettingsMenuItem("Privacy & Security", Icons.Outlined.Shield, Color(0xFF00C853).copy(alpha = 0.13f), onClick = onOpenPrivacySecurity),
        SettingsMenuItem("Appearance", Icons.Outlined.Palette, Color(0xFF9C27B0).copy(alpha = 0.13f), onClick = onOpenAppearance),
        SettingsMenuItem("Language & Region", Icons.Outlined.Language, Color(0xFF00BCD4).copy(alpha = 0.13f), onClick = {}),
        SettingsMenuItem("Blocked Users", Icons.Outlined.VisibilityOff, Color(0xFFF44336).copy(alpha = 0.13f), onClick = onOpenBlockedUsers),
        SettingsMenuItem("Help & Support", Icons.AutoMirrored.Outlined.HelpOutline, Color(0xFF4CAF50).copy(alpha = 0.13f), onClick = onOpenHelpSupport),
        SettingsMenuItem("About", Icons.AutoMirrored.Outlined.HelpOutline, Color(0xFF607D8B).copy(alpha = 0.13f), onClick = onOpenAbout),
    )

    Surface(color = SBg) {
        LazyColumn(
            modifier = Modifier
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HeaderBackButton(onBack = onBack)
                    Text("Settings", color = SWhite, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(Modifier.size(24.dp))
                }
            }

            item {
                ProfileHeader()
            }

            items(menuItems) { menu ->
                SettingsRow(menu = menu)
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SCard)
                        .border(1.dp, SBorder, RoundedCornerShape(14.dp))
                        .clickable(onClick = onLogout)
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Security, contentDescription = null, tint = SDanger, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Log Out", color = SDanger, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }

            item {
                Text(
                    text = "Version 1.0.0",
                    color = Color(0xFF6A7282),
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                )
            }
        }
    }
}

@Composable
private fun HeaderBackButton(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(SCard)
            .border(1.dp, SBorder, CircleShape)
            .clickable(onClick = onBack),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = SGray2)
    }
}

@Composable
private fun ProfileHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF0D7FFF), Color(0xFF0A5FD4))))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("S", color = SWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Column {
                Text("Sarah Johnson", color = SWhite, fontSize = 18.sp)
                Text("@sarahjohnson", color = Color(0xFFDBEAFE), fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun SettingsRow(menu: SettingsMenuItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SCard)
            .border(1.dp, SBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = menu.onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(menu.tintBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(menu.icon, contentDescription = null, tint = SWhite, modifier = Modifier.size(20.dp))
        }
        Text(menu.title, color = SWhite, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = SGray4)
    }
}