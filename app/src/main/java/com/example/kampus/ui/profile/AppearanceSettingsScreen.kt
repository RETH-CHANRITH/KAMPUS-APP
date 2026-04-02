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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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

private val UBg = Color(0xFF1A1D2E)
private val UCard = Color(0xFF252A41)
private val UBorder = Color(0xFF364153)
private val UWhite = Color(0xFFFFFFFF)
private val USubtle = Color(0xFF99A1AF)
private val UTextMuted = Color(0xFFD1D5DC)

private data class AccentOption(val name: String, val color: Color)

@Composable
fun AppearanceSettingsScreen(onBack: () -> Unit) {
    var selectedTheme by remember { mutableStateOf("Dark") }
    val accentOptions = listOf(
        AccentOption("Blue", Color(0xFF0D7FFF)),
        AccentOption("Purple", Color(0xFF9C27B0)),
        AccentOption("Pink", Color(0xFFE91E63)),
        AccentOption("Red", Color(0xFFF44336)),
        AccentOption("Orange", Color(0xFFFF9800)),
        AccentOption("Green", Color(0xFF4CAF50)),
        AccentOption("Teal", Color(0xFF009688)),
    )
    var selectedAccent by remember { mutableStateOf(accentOptions.first()) }
    var fontScale by remember { mutableFloatStateOf(1f) }

    Surface(color = UBg, modifier = Modifier.fillMaxSize()) {
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
                        .background(UCard)
                        .border(1.dp, UBorder, CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = UWhite)
                }
                Text("Appearance", color = UWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Box(Modifier.size(40.dp))
            }

            Section("Theme") {
                ThemeRow("Light", selectedTheme == "Light") { selectedTheme = "Light" }
                ThemeRow("Dark", selectedTheme == "Dark") { selectedTheme = "Dark" }
                ThemeRow("Black", selectedTheme == "Black", showDivider = false) { selectedTheme = "Black" }
            }

            Section("Accent Color") {
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
                                    onClick = { selectedAccent = option },
                                )
                            }
                            repeat(4 - row.size) {
                                Box(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Section("Font Size") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Aa", color = UWhite, fontSize = 14.sp)
                        Text("Aa", color = UWhite, fontSize = 20.sp)
                    }
                    Slider(
                        value = fontScale,
                        onValueChange = { fontScale = it },
                        valueRange = 0.8f..1.3f,
                        colors = SliderDefaults.colors(
                            thumbColor = selectedAccent.color,
                            activeTrackColor = selectedAccent.color,
                            inactiveTrackColor = UBorder,
                        ),
                    )
                }
            }

            Section("Preview") {
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
                            Text("Username", color = UWhite, fontWeight = FontWeight.Medium, fontSize = (16 * fontScale).sp)
                            Text("2 hours ago", color = USubtle, fontSize = 13.sp)
                        }
                    }
                    Text(
                        "This is a preview of how your posts will look with the selected theme and colors.",
                        color = UTextMuted,
                        fontSize = (15 * fontScale).sp,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ActionChip("Like", selectedAccent.color)
                        ActionChip("Comment", selectedAccent.color)
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, color = UWhite, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(UCard)
                .border(1.dp, UBorder, RoundedCornerShape(14.dp)),
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
            Text(label, color = UWhite, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0D7FFF)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Check, contentDescription = null, tint = UWhite, modifier = Modifier.size(16.dp))
                }
            }
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .background(UBorder)
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
                Icon(Icons.Outlined.Check, contentDescription = null, tint = UWhite, modifier = Modifier.size(16.dp))
            }
        }
        Text(option.name, color = UWhite, fontSize = 12.sp, fontWeight = FontWeight.Medium)
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