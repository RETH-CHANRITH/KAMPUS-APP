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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.kampus.ui.theme.ThemeController
import com.example.kampus.ui.theme.AppAccent
import com.example.kampus.ui.theme.AppSettingsStore
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.kampus.ui.localization.rememberUiStrings

private data class AccentOption(val accent: AppAccent, val name: String, val color: Color)

@Composable
fun AppearanceSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    val strings = rememberUiStrings()
    val accentOptions = listOf(
        AccentOption(AppAccent.Blue, strings.blue, AppAccent.Blue.color),
        AccentOption(AppAccent.Purple, strings.purple, AppAccent.Purple.color),
        AccentOption(AppAccent.Pink, strings.pink, AppAccent.Pink.color),
        AccentOption(AppAccent.Red, strings.red, AppAccent.Red.color),
        AccentOption(AppAccent.Orange, strings.orange, AppAccent.Orange.color),
        AccentOption(AppAccent.Green, strings.green, AppAccent.Green.color),
        AccentOption(AppAccent.Teal, strings.teal, AppAccent.Teal.color),
    )
    var selectedAccent by remember {
        mutableStateOf(accentOptions.firstOrNull { it.accent == ThemeController.accent } ?: accentOptions.first())
    }
    var sliderValue by remember { mutableStateOf(ThemeController.fontScale) }

    LaunchedEffect(ThemeController.accent) {
        selectedAccent = accentOptions.firstOrNull { it.accent == ThemeController.accent } ?: accentOptions.first()
    }
    LaunchedEffect(ThemeController.fontScale) {
        sliderValue = ThemeController.fontScale
    }

    val fontPercent = (sliderValue * 100).toInt()

    Surface(color = colors.background, modifier = Modifier.fillMaxSize()) {
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
                        .background(colors.surfaceVariant)
                        .border(1.dp, colors.outlineVariant, CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = strings.back, tint = colors.onSurface)
                }
                Text(strings.appearance, color = colors.onBackground, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Box(Modifier.size(40.dp))
            }

            Section(strings.theme) {
                ThemeRow(strings.light, !ThemeController.isDark) {
                    ThemeController.isDark = false
                    scope.launch { AppSettingsStore.saveTheme(context, isDark = false) }
                }
                ThemeRow(strings.dark, ThemeController.isDark, showDivider = false) {
                    ThemeController.isDark = true
                    scope.launch { AppSettingsStore.saveTheme(context, isDark = true) }
                }
            }

            Section(strings.accentColor) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    accentOptions.chunked(4).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            row.forEach { option ->
                                AccentCard(
                                    option = option,
                                    selected = option == selectedAccent,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        selectedAccent = option
                                        ThemeController.accent = option.accent
                                        scope.launch {
                                            AppSettingsStore.saveAccent(context, option.accent.key)
                                        }
                                    },
                                )
                            }
                            repeat(4 - row.size) {
                                Box(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Section(strings.fontSize) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Aa", color = colors.onSurface, fontSize = 14.sp)
                        Text("$fontPercent%", color = selectedAccent.color, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Aa", color = colors.onSurface, fontSize = 20.sp)
                    }
                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        onValueChangeFinished = {
                            ThemeController.fontScale = sliderValue
                            scope.launch { AppSettingsStore.saveFontScale(context, sliderValue) }
                        },
                        valueRange = 0.3f..1.5f,
                        colors = SliderDefaults.colors(
                            thumbColor = selectedAccent.color,
                            activeTrackColor = selectedAccent.color,
                            inactiveTrackColor = colors.outlineVariant,
                        ),
                    )
                }
            }

            Section(strings.preview) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4A5565)),
                        )
                        Column {
                            Text(strings.username, color = colors.onSurface, fontWeight = FontWeight.Medium, fontSize = (16 * sliderValue).sp)
                            Text("2 ${strings.hoursAgo}", color = colors.onSurfaceVariant, fontSize = 13.sp)
                        }
                    }
                    Text(
                        strings.thisIsPreview,
                        color = colors.onSurfaceVariant,
                        fontSize = (15 * sliderValue).sp,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ActionChip(strings.like, selectedAccent.color)
                        ActionChip(strings.comment, selectedAccent.color)
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, color = colors.onBackground, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.surface)
                .border(1.dp, colors.outlineVariant, RoundedCornerShape(14.dp)),
        ) {
            content()
        }
    }
}

@Composable
private fun ThemeRow(
    label: String,
    selected: Boolean,
    showDivider: Boolean = true,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
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
            Text(label, color = colors.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(colors.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Check, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(16.dp))
                }
            }
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .background(colors.outlineVariant)
                    .size(width = 1.dp, height = 1.dp),
            )
        }
    }
}

@Composable
private fun AccentCard(
    option: AccentOption,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(option.color.copy(alpha = 0.15f))
            .border(
                width = if (selected) 1.5.dp else 0.dp,
                color = if (selected) option.color else Color.Transparent,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(option.color),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(Icons.Outlined.Check, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(16.dp))
            }
        }
        Text(option.name, color = colors.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ActionChip(label: String, accent: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(accent.copy(alpha = 0.15f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label, color = accent, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}