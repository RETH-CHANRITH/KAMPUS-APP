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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kampus.ui.theme.ThemeController
import com.example.kampus.utils.LanguageManager

private val UiIsDark get() = ThemeController.isDark
private val LBg get() = if (UiIsDark) Color(0xFF1A1D2E) else Color(0xFFF3F4F8)
private val LCard get() = if (UiIsDark) Color(0xFF252A41) else Color(0xFFFFFFFF)
private val LBorder get() = if (UiIsDark) Color(0xFF364153) else Color(0xFFD1D5DB)
private val LTextPrimary get() = if (UiIsDark) Color(0xFFFFFFFF) else Color(0xFF111827)
private val LTextSecondary get() = if (UiIsDark) Color(0xFFD1D5DC) else Color(0xFF6B7280)
private val LBlue get() = Color(0xFF0D7FFF)

@Composable
fun LanguageRegionScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val selectedLanguage by LanguageManager.languageCode.collectAsState()
    val strings = com.example.kampus.ui.localization.rememberUiStrings()

    Surface(color = LBg, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(LCard)
                        .border(1.dp, LBorder, CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = LTextPrimary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(strings.languageAndRegion, color = LTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(strings.languageSubtitle, color = LTextSecondary, fontSize = 14.sp)
                }
            }

            LanguageOptionCard(
                title = strings.englishLanguage,
                subtitle = strings.englishDesc,
                selected = selectedLanguage == LanguageManager.ENGLISH,
                onClick = { LanguageManager.setLanguage(context, LanguageManager.ENGLISH) },
            )

            LanguageOptionCard(
                title = strings.khmerLanguage,
                subtitle = strings.khmerDesc,
                selected = selectedLanguage == LanguageManager.KHMER,
                onClick = { LanguageManager.setLanguage(context, LanguageManager.KHMER) },
            )
        }
    }
}



@Composable
private fun LanguageOptionCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) LBlue.copy(alpha = 0.12f) else LCard)
            .border(1.dp, if (selected) LBlue else LBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (selected) LBlue else LBorder),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Language, contentDescription = null, tint = Color.White)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = LTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = LTextSecondary, fontSize = 13.sp)
        }
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (selected) LBlue else Color.Transparent)
                .border(1.dp, if (selected) LBlue else LBorder, CircleShape),
        )
    }
}
