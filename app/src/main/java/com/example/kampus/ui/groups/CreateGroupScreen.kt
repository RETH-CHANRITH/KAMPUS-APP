@file:Suppress("SpellCheckingInspection")
package com.example.kampus.ui.groups

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kampus.ui.groups.GroupColors as C

private val emojiOptions = listOf("🎨","🌀","💻","📷","✏️","🎵","🚀","🌸","⚽","🎮","📚","🍕","🌍","🎬","🏋️")
private val gradientOptions = listOf(
    Color(0xFF1E1040) to Color(0xFF2D1B6E),
    Color(0xFF0D1F3C) to Color(0xFF0F3460),
    Color(0xFF0D2B18) to Color(0xFF0F4C2A),
    Color(0xFF1A0D2B) to Color(0xFF3D1A5C),
    Color(0xFF2B1400) to Color(0xFF5C2E00),
    Color(0xFF1A2B0D) to Color(0xFF2E4A14),
    Color(0xFF2B0D1A) to Color(0xFF5C1A3D),
    Color(0xFF0D1A2B) to Color(0xFF0F2D50),
)
private val categoryOptions = listOf(
    "Art & Design","Technology","Photography","Music","Business",
    "Sports","Gaming","Education","Entertainment","3D & Graphics",
)

// ─────────────────────────────────────────────────────────────────────────────
//  Screen
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun CreateGroupScreen(
    onBack   : () -> Unit = {},
    onCreate : (GroupData) -> Unit = {},
    viewModel: GroupViewModel = viewModel(),
) {
    var name        by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category    by remember { mutableStateOf(categoryOptions.first()) }
    var emoji       by remember { mutableStateOf(emojiOptions.first()) }
    var gradIndex   by remember { mutableIntStateOf(0) }

    val canCreate = name.isNotBlank() && description.isNotBlank()

    Scaffold(
        containerColor = C.Bg,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                CoverIconButton(onClick = onBack) {
                    Icon(
                        imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint               = C.White,
                        modifier           = Modifier.size(20.dp),
                    )
                }
                Text(
                    text       = "Create Group",
                    color      = C.White,
                    fontSize   = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.size(38.dp))
            }
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(C.NavBg)
                    .border(1.dp, C.NavBorder, RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp))
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .navigationBarsPadding(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (canCreate)
                                Brush.linearGradient(listOf(C.Blue, C.BlueGlow))
                            else
                                Brush.linearGradient(listOf(C.Gray5, C.Gray5))
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = null,
                            enabled           = canCreate,
                        ) {
                            val grad = gradientOptions[gradIndex]
                            val groupData = GroupData(
                                id          = (100..999).random(),
                                name        = name,
                                category    = category,
                                coverColor1 = grad.first,
                                coverColor2 = grad.second,
                                coverEmoji  = emoji,
                                members     = "1",
                                posts       = "0",
                                isJoined    = true,
                                description = description,
                            )
                            // Create the group and log activity
                            viewModel.createGroup(groupData)
                            // Also call the original onCreate callback for navigation
                            onCreate(groupData)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text       = "Create Group",
                        color      = if (canCreate) C.White else C.Gray3,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            // ── Cover preview ─────────────────────────────────────────────────
            val grad = gradientOptions[gradIndex]
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Brush.linearGradient(listOf(grad.first, grad.second))),
                contentAlignment = Alignment.Center,
            ) {
                Text(emoji, fontSize = 72.sp)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, C.Bg.copy(alpha = 0.7f))
                            )
                        )
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Form fields ───────────────────────────────────────────────────
            Column(
                modifier            = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Group name
                FormField(label = "Group Name") {
                    StyledTextField(
                        value       = name,
                        onValueChange = { name = it },
                        placeholder = "e.g. Design Inspiration",
                        singleLine  = true,
                    )
                }

                // Description
                FormField(label = "Description") {
                    StyledTextField(
                        value         = description,
                        onValueChange = { description = it },
                        placeholder   = "What is this group about?",
                        singleLine    = false,
                        minHeight     = 80.dp,
                    )
                }

                // Category
                FormField(label = "Category") {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding        = PaddingValues(vertical = 4.dp),
                    ) {
                        items(categoryOptions) { cat ->
                            val selected = cat == category
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (selected)
                                            Brush.linearGradient(listOf(C.Blue, C.BlueGlow))
                                        else
                                            Brush.linearGradient(listOf(C.Surface, C.Surface))
                                    )
                                    .border(
                                        1.dp,
                                        if (selected) Color.Transparent else C.Border,
                                        RoundedCornerShape(10.dp),
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication        = null,
                                    ) { category = cat }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text       = cat,
                                    color      = if (selected) C.White else C.Gray3,
                                    fontSize   = 13.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            }
                        }
                    }
                }

                // Emoji picker
                FormField(label = "Cover Emoji") {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding        = PaddingValues(vertical = 4.dp),
                    ) {
                        items(emojiOptions) { em ->
                            val selected = em == emoji
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) C.Blue.copy(alpha = 0.2f) else C.Surface)
                                    .border(
                                        1.dp,
                                        if (selected) C.Blue else C.Border,
                                        RoundedCornerShape(10.dp),
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication        = null,
                                    ) { emoji = em },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(em, fontSize = 20.sp)
                            }
                        }
                    }
                }

                // Gradient picker
                FormField(label = "Cover Color") {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding        = PaddingValues(vertical = 4.dp),
                    ) {
                        items(gradientOptions.size) { i ->
                            val g = gradientOptions[i]
                            val selected = gradIndex == i
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(g.first, g.second)))
                                    .border(
                                        width = if (selected) 2.5.dp else 0.dp,
                                        color = if (selected) C.White else Color.Transparent,
                                        shape = CircleShape,
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication        = null,
                                    ) { gradIndex = i },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Helpers
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun FormField(
    label   : String,
    content : @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text       = label,
            color      = C.Gray1,
            fontSize   = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
        content()
    }
}

@Composable
private fun StyledTextField(
    value         : String,
    onValueChange : (String) -> Unit,
    placeholder   : String,
    singleLine    : Boolean,
    minHeight     : androidx.compose.ui.unit.Dp = 46.dp,
) {
    val source  = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val border  by animateColorAsState(
        targetValue   = if (focused) C.Blue else C.Border,
        animationSpec = tween(200),
        label         = "field_border",
    )

    BasicTextField(
        value             = value,
        onValueChange     = onValueChange,
        modifier          = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(C.Surface)
            .border(1.dp, border, RoundedCornerShape(13.dp))
            .padding(14.dp)
            .defaultMinSize(minHeight = minHeight),
        singleLine        = singleLine,
        textStyle         = TextStyle(color = C.White, fontSize = 14.sp, lineHeight = 20.sp),
        cursorBrush       = SolidColor(C.Blue),
        interactionSource = source,
        decorationBox     = { inner ->
            if (value.isEmpty()) {
                Text(placeholder, color = C.Gray5, fontSize = 14.sp)
            }
            inner()
        },
    )
}

@Composable
private fun CoverIconButton(
    onClick : () -> Unit,
    content : @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(C.Surface)
            .border(1.dp, C.Border, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick,
            ),
        contentAlignment = Alignment.Center,
        content          = { content() },
    )
}