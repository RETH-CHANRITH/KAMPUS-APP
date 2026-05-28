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
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.painter.ColorPainter
import coil.request.ImageRequest
import com.example.kampus.ui.localization.rememberUiStrings
import com.example.kampus.ui.theme.ThemeController
import com.example.kampus.ui.theme.AppAccent
import com.example.kampus.ui.theme.AppSettingsStore

private val UiIsDark get() = ThemeController.isDark
private val SBg get() = if (UiIsDark) Color(0xFF1A1D2E) else Color(0xFFF3F4F8)
private val SCard get() = if (UiIsDark) Color(0xFF252A41) else Color(0xFFFFFFFF)
private val SBorder get() = if (UiIsDark) Color(0xFF364153) else Color(0xFFD1D5DB)
private val SWhite get() = if (UiIsDark) Color(0xFFFFFFFF) else Color(0xFF111827)
private val SGray2 get() = if (UiIsDark) Color(0xFFD1D5DC) else Color(0xFF374151)
private val SGray4 get() = if (UiIsDark) Color(0xFF9CA3AF) else Color(0xFF6B7280)
private val SDanger get() = Color(0xFFFB2C36)

private data class SettingsMenuItem(
    val title: String,
    val icon: ImageVector,
    val tintBg: Color,
    val onClick: () -> Unit,
)

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenAccountSettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenPrivacySecurity: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenLanguageRegion: () -> Unit,
    onOpenBlockedUsers: () -> Unit,
    onOpenHelpSupport: () -> Unit,
    onOpenAbout: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = viewModel(),
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val strings = rememberUiStrings()
    var showLogoutDialog by remember { mutableStateOf(false) }
    val accent = ThemeController.accent.color
    val accentSoft = accent.copy(alpha = if (ThemeController.isDark) 0.18f else 0.12f)
    val accentSoftBg = accent.copy(alpha = if (ThemeController.isDark) 0.10f else 0.08f)

    val menuItems = listOf(
        SettingsMenuItem(strings.editProfile, Icons.Outlined.Person, accentSoftBg, onClick = onEditProfile),
        SettingsMenuItem(strings.notifications, Icons.Outlined.Notifications, accent.copy(alpha = 0.10f), onClick = onOpenNotificationSettings),
        SettingsMenuItem(strings.privacyAndSecurity, Icons.Outlined.Shield, accent.copy(alpha = 0.08f), onClick = onOpenPrivacySecurity),
        SettingsMenuItem(strings.account, Icons.Outlined.Edit, accent.copy(alpha = 0.12f), onClick = onOpenAccountSettings),
        SettingsMenuItem(strings.appearance, Icons.Outlined.Palette, accentSoftBg, onClick = onOpenAppearance),
        SettingsMenuItem(strings.languageAndRegion, Icons.Outlined.Language, accent.copy(alpha = 0.09f), onClick = onOpenLanguageRegion),
        SettingsMenuItem(strings.blockedUsers, Icons.Outlined.VisibilityOff, accent.copy(alpha = 0.10f), onClick = onOpenBlockedUsers),
        SettingsMenuItem(strings.helpAndSupport, Icons.AutoMirrored.Outlined.HelpOutline, accent.copy(alpha = 0.08f), onClick = onOpenHelpSupport),
        SettingsMenuItem(strings.about, Icons.AutoMirrored.Outlined.HelpOutline, accent.copy(alpha = 0.07f), onClick = onOpenAbout),
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
                    Text(strings.settings, color = SWhite, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(Modifier.size(24.dp))
                }
            }

            item {
                ProfileHeader(
                    displayName = state.displayName,
                    handle = state.handle,
                    profileImageUrl = state.profileImageUrl,
                    avatarEmoji = state.avatarEmoji,
                    accent = accent,
                    accentSoft = accentSoft,
                )
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
                        .clickable(onClick = { showLogoutDialog = true })
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Security, contentDescription = null, tint = SDanger, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(strings.logOut, color = SDanger, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }

            item {
                Text(
                    text = "${strings.version} 1.0.0",
                    color = SGray4,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                )
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = SCard,
            title = {
                Text(
                    text = strings.logoutConfirmTitle,
                    color = SWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = strings.logoutConfirmMessage,
                    color = SGray2,
                    fontSize = 14.sp,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                ) {
                    Text(text = strings.logOut, color = SDanger, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(text = strings.cancel, color = SGray4, fontWeight = FontWeight.Medium)
                }
            },
        )
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
private fun ProfileHeader(
    displayName: String,
    handle: String,
    profileImageUrl: String,
    avatarEmoji: String,
    accent: Color,
    accentSoft: Color,
) {
    val resolvedName = displayName.ifBlank { "User" }
    val resolvedHandle = if (handle.isNotBlank()) handle else "@${resolvedName.lowercase().replace(" ", "")}"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.98f), accent.copy(alpha = 0.78f))))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(accentSoft)
                    .border(2.dp, Color.White.copy(alpha = if (UiIsDark) 0.95f else 0.8f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (profileImageUrl.isNotBlank()) {
                    val ctx = LocalContext.current
                    AsyncImage(
                        model = ImageRequest.Builder(ctx)
                            .data(profileImageUrl)
                            .crossfade(true)
                            .build(),
                        placeholder = ColorPainter(Color.White.copy(alpha = 0.12f)),
                        contentDescription = "Profile Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape),
                    )
                } else {
                    Text(
                        text = avatarEmoji.ifBlank { resolvedName.firstOrNull()?.uppercase() ?: "U" },
                        color = SWhite,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Column {
                Text(resolvedName, color = SWhite, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(resolvedHandle, color = Color(0xFFDBEAFE), fontSize = 14.sp)
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