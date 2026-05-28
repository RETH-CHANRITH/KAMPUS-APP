package com.example.kampus.ui.screens.groups

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kampus.data.model.GroupPrivacy
import com.example.kampus.ui.theme.KampusColors as C
import com.example.kampus.ui.theme.KampusType as T
import com.example.kampus.viewmodel.GroupsViewModel

private val categoryOptions = listOf(
    "Art & Design",
    "Technology",
    "Photography",
    "Music",
    "Business",
    "Sports",
    "Gaming",
    "Education",
    "Entertainment",
    "3D & Graphics",
    "Marketing",
    "Productivity",
    "AI & Tools",
    "Writing",
    "Lifestyle",
)

private val coverEmojiOptions = listOf("🎨", "🌀", "💻", "📸", "✏️", "🎵", "🚀", "🌸")

private val coverGradientOptions = listOf(
    listOf(Color(0xFF4B2C7A), Color(0xFF1A1D2E)),
    listOf(Color(0xFF0D7FFF), Color(0xFF2E3450)),
    listOf(Color(0xFF0F766E), Color(0xFF252A41)),
    listOf(Color(0xFF8B5CF6), Color(0xFF1A1D2E)),
    listOf(Color(0xFFB45309), Color(0xFF2E3450)),
    listOf(Color(0xFF15803D), Color(0xFF252A41)),
    listOf(Color(0xFFBE185D), Color(0xFF1A1D2E)),
    listOf(Color(0xFF1D4ED8), Color(0xFF1A1D2E)),
)

@Composable
fun CreateGroupScreen(
    viewModel: GroupsViewModel,
    onBack: () -> Unit,
    onCreated: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categoryOptions.first()) }
    var privacy by remember { mutableStateOf(GroupPrivacy.PUBLIC) }
    var categoryOpen by remember { mutableStateOf(false) }
    var selectedCoverEmoji by remember { mutableStateOf(coverEmojiOptions.first()) }
    var selectedGradientIndex by remember { mutableIntStateOf(0) }
    val scrollState = rememberScrollState()

    Scaffold(containerColor = C.Background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, null, tint = C.TextPrimary)
                }
                Text("Create Group", color = C.TextPrimary, style = T.HeadingLarge)
            }

            CoverPreviewCard(
                emoji = selectedCoverEmoji,
                gradient = coverGradientOptions[selectedGradientIndex],
            )

            FormField(label = "Group name") {
                AppTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "Enter group name",
                )
            }

            FormField(label = "Description") {
                AppTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = "Tell people what this group is about",
                    singleLine = false,
                    minHeight = 96.dp,
                )
            }

            FormField(label = "Category") {
                Box {
                    AppTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        placeholder = "Choose a category",
                        enabled = false,
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { categoryOpen = true })
                }
                DropdownMenu(expanded = categoryOpen, onDismissRequest = { categoryOpen = false }) {
                    categoryOptions.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item) },
                            onClick = {
                                selectedCategory = item
                                categoryOpen = false
                            },
                        )
                    }
                }
            }

            FormField(label = "Cover emoji") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 2.dp)) {
                    items(coverEmojiOptions) { emoji ->
                        CoverEmojiChip(
                            emoji = emoji,
                            selected = selectedCoverEmoji == emoji,
                            onClick = { selectedCoverEmoji = emoji },
                        )
                    }
                }
            }

            FormField(label = "Cover color") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 2.dp)) {
                    items(coverGradientOptions.indices.toList()) { index ->
                        CoverColorChip(
                            colors = coverGradientOptions[index],
                            selected = selectedGradientIndex == index,
                            onClick = { selectedGradientIndex = index },
                        )
                    }
                }
            }

            FormField(label = "Group privacy") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PrivacySelectCard(
                        title = "Public",
                        subtitle = "Anyone can join instantly",
                        selected = privacy == GroupPrivacy.PUBLIC,
                        icon = Icons.Filled.Public,
                        modifier = Modifier.weight(1f),
                        onClick = { privacy = GroupPrivacy.PUBLIC },
                    )
                    PrivacySelectCard(
                        title = "Private",
                        subtitle = "Admin approval required",
                        selected = privacy == GroupPrivacy.PRIVATE,
                        icon = Icons.Outlined.Lock,
                        modifier = Modifier.weight(1f),
                        onClick = { privacy = GroupPrivacy.PRIVATE },
                    )
                }
            }

            PrivacyInfoCard(privacy = privacy)

            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        viewModel.createGroup(
                            name = name.trim(),
                            category = selectedCategory,
                            description = description.trim(),
                            privacy = privacy,
                        )
                        onCreated()
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = C.Primary,
                    contentColor = Color.White,
                    disabledContainerColor = C.Primary.copy(alpha = 0.4f),
                    disabledContentColor = Color.White.copy(alpha = 0.6f),
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .height(52.dp),
            ) {
                Text("Create Group", style = T.LabelMedium, fontWeight = FontWeight.SemiBold)
            }

            Spacer(
                modifier = Modifier
                    .height(16.dp)
                    .navigationBarsPadding()
                    .imePadding()
            )
        }
    }
}

@Composable
private fun CoverPreviewCard(
    emoji: String,
    gradient: List<Color>,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(gradient)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, style = T.HeadingLarge)
        }
    }
}

@Composable
private fun CoverEmojiChip(
    emoji: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor by animateColorAsState(if (selected) C.Primary else C.Border, tween(180), label = "emoji_border")
    val backgroundColor by animateColorAsState(if (selected) C.PrimaryContainer else C.Surface, tween(180), label = "emoji_bg")

    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji, style = T.HeadingSmall)
    }
}

@Composable
private fun CoverColorChip(
    colors: List<Color>,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor by animateColorAsState(if (selected) Color.White else Color.Transparent, tween(180), label = "color_border")

    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(colors))
            .border(2.dp, borderColor, CircleShape)
            .clickable(onClick = onClick),
    )
}

@Composable
private fun PrivacySelectCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val borderColor by animateColorAsState(if (selected) C.Primary else C.Border, tween(220), label = "privacy_border")
    val backgroundColor by animateColorAsState(if (selected) C.PrimaryContainer else C.Surface, tween(220), label = "privacy_bg")

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, null, tint = if (selected) C.Primary else C.TextSecondary)
        Text(title, color = C.TextPrimary, style = T.HeadingSmall)
        Text(subtitle, color = C.TextMuted, style = T.BodySmall)
    }
}

@Composable
private fun PrivacyInfoCard(privacy: GroupPrivacy) {
    Surface(color = C.Surface, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, C.Border)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (privacy == GroupPrivacy.PUBLIC) "Public group" else "Private group", color = C.TextPrimary, style = T.HeadingSmall)
            Bullet(if (privacy == GroupPrivacy.PUBLIC) "Members can join and post right away." else "Users must wait for admin approval to join.")
            Bullet(if (privacy == GroupPrivacy.PUBLIC) "Best for open communities and class-wide sharing." else "Best for controlled communities and moderated posting.")
            Bullet("You can change the group feel later with cover image and category.")
        }
    }
}

@Composable
private fun Bullet(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(C.Primary).padding(top = 6.dp))
        Text(text, color = C.TextSecondary, style = T.BodySmall)
    }
}

@Composable
private fun FormField(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = C.TextSecondary, style = T.LabelMedium)
        content()
    }
}

@Composable
private fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    minHeight: androidx.compose.ui.unit.Dp = 56.dp,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        placeholder = { Text(placeholder, color = C.TextHint) },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight),
        singleLine = singleLine,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = C.Surface,
            unfocusedContainerColor = C.Surface,
            disabledContainerColor = C.Surface,
            focusedTextColor = C.TextPrimary,
            unfocusedTextColor = C.TextPrimary,
            disabledTextColor = C.TextPrimary,
            focusedIndicatorColor = C.Primary,
            unfocusedIndicatorColor = C.Border,
            disabledIndicatorColor = C.Border,
            cursorColor = C.Primary,
        ),
    )
}