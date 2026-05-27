package com.example.kampus.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
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

// Note: Uses ProfileColors for dynamic theming

private sealed interface PrivacySecurityItem {
    data class Toggle(
        val title: String,
        val subtitle: String,
        val checked: Boolean,
        val onCheckedChange: (Boolean) -> Unit,
    ) : PrivacySecurityItem

    data class Action(
        val title: String,
        val subtitle: String,
        val onClick: () -> Unit,
    ) : PrivacySecurityItem
}

@Composable
fun PrivacySecurityScreen(
    onBack: () -> Unit,
    onChangePassword: () -> Unit = {},
    onLoginActivity: () -> Unit = {},
    onDownloadData: () -> Unit = {},
    onSearchHistory: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel(),
) {
    val strings = rememberUiStrings()
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val privacy = state.privacySettings
    val privacyItems = listOf(
        PrivacySecurityItem.Toggle(
            title = strings.privateAccount,
            subtitle = "Only followers can see your posts",
            checked = privacy.privateAccount,
            onCheckedChange = viewModel::setPrivateAccount,
        ),
        PrivacySecurityItem.Toggle(
            title = strings.activityStatus,
            subtitle = "Show when you're active",
            checked = privacy.activityStatus,
            onCheckedChange = viewModel::setActivityStatus,
        ),
        PrivacySecurityItem.Toggle(
            title = strings.allowTagging,
            subtitle = "Let others tag you in photos",
            checked = privacy.allowTagging,
            onCheckedChange = viewModel::setAllowTagging,
        ),
        PrivacySecurityItem.Toggle(
            title = strings.allowMentions,
            subtitle = "Let others mention you",
            checked = privacy.allowMentions,
            onCheckedChange = viewModel::setAllowMentions,
        ),
    )
    val securityItems = listOf(
        PrivacySecurityItem.Toggle(
            title = strings.twoFactorAuthentication,
            subtitle = "Add extra security to your account",
            checked = privacy.twoFactorAuthentication,
            onCheckedChange = viewModel::setTwoFactorAuthentication,
        ),
        PrivacySecurityItem.Action(
            title = strings.changePassword,
            subtitle = "Update your password",
            onClick = onChangePassword,
        ),
        PrivacySecurityItem.Action(
            title = strings.loginActivity,
            subtitle = "Review recent logins",
            onClick = onLoginActivity,
        ),
    )
    val dataItems = listOf(
        PrivacySecurityItem.Action(
            title = strings.downloadYourData,
            subtitle = "Get a copy of your data",
            onClick = onDownloadData,
        ),
        PrivacySecurityItem.Action(
            title = strings.searchHistory,
            subtitle = "View and clear searches",
            onClick = onSearchHistory,
        ),
    )

    Surface(color = ProfileColors.Bg, modifier = Modifier.fillMaxSize()) {
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
                        .background(ProfileColors.Card)
                        .border(1.dp, ProfileColors.Border, CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = strings.back, tint = ProfileColors.White)
                }
                Text(strings.privacySecurityTitle, color = ProfileColors.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Box(Modifier.size(40.dp))
            }

            state.error?.let { message ->
                ErrorMessage(
                    message = message,
                    onDismiss = viewModel::clearError,
                )
            }

            if (state.isLoading) {
                InfoMessage(message = strings.syncingPrivacySettings)
            }

            SectionCard(title = strings.privacy, items = privacyItems)
            SectionCard(title = strings.security, items = securityItems)
            SectionCard(title = strings.dataAndHistory, items = dataItems)
        }
    }
}

@Composable
private fun SectionCard(title: String, items: List<PrivacySecurityItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = ProfileColors.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(ProfileColors.Card)
                .border(1.dp, ProfileColors.Border, RoundedCornerShape(14.dp)),
        ) {
            items.forEachIndexed { index, item ->
                when (item) {
                    is PrivacySecurityItem.Toggle -> ToggleRow(
                        title = item.title,
                        subtitle = item.subtitle,
                        checked = item.checked,
                        onCheckedChange = item.onCheckedChange,
                        showDivider = index != items.lastIndex,
                    )

                    is PrivacySecurityItem.Action -> ActionRow(
                        title = item.title,
                        subtitle = item.subtitle,
                        onClick = item.onClick,
                        showDivider = index != items.lastIndex,
                    )
                }
            }
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
                Text(title, color = ProfileColors.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text(subtitle, color = ProfileColors.Subtle, fontSize = 13.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ProfileColors.White,
                    checkedTrackColor = ProfileColors.Blue,
                    uncheckedThumbColor = ProfileColors.White,
                    uncheckedTrackColor = Color(0xFF4A5565),
                ),
            )
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .height(1.dp)
                    .background(ProfileColors.Border),
            )
        }
    }
}

@Composable
private fun ActionRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showDivider: Boolean = true,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = ProfileColors.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text(subtitle, color = ProfileColors.Subtle, fontSize = 13.sp)
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = ProfileColors.Subtle,
            )
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .height(1.dp)
                    .background(ProfileColors.Border),
            )
        }
    }
}

@Composable
private fun ErrorMessage(
    message: String,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ProfileColors.ErrorBg)
            .clickable(onClick = onDismiss)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = message,
            color = ProfileColors.White,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
    }
}

@Composable
private fun InfoMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ProfileColors.SuccessBg)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = message,
            color = ProfileColors.White,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
    }
}