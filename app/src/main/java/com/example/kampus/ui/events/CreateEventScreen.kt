@file:Suppress("SpellCheckingInspection")
package com.example.kampus.ui.events

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.kampus.ui.events.getFileName
import com.example.kampus.ui.theme.BgDark
import com.example.kampus.ui.theme.BorderSubtle
import com.example.kampus.ui.theme.CardSurface
import com.example.kampus.ui.theme.Primary
import com.example.kampus.ui.theme.SurfaceDark
import com.example.kampus.ui.theme.TextPrimary
import com.example.kampus.ui.theme.TextSecondary
import com.example.kampus.ui.theme.TextTertiary
import com.example.kampus.ui.groups.GroupColors
import com.example.kampus.ui.theme.AccentYellow

// ─────────────────────────────────────────────────────────────────────────────
//  Models
// ─────────────────────────────────────────────────────────────────────────────

data class NewEventData(
    val title: String = "",
    val description: String = "",
    val date: String = "",
    val time: String = "",
    val location: String = "",
    val organiser: String = "",
    val website: String = "",
    val coverEmoji: String = "🎉",
    val category: EventCategory = EventCategory.CAMPUS,
    val tags: List<String> = emptyList(),
    val attachments: List<MediaAttachment> = emptyList(),
    val textInput: String = "",
    val visibility: EventVisibility = EventVisibility.PUBLIC,
    val limitCapacity: Boolean = false,
    val capacity: Int = 50,
    val rsvpRequired: Boolean = true,
    val allowComments: Boolean = true,
    val shareToFeed: Boolean = true,
)

enum class MediaType(val label: String, val emoji: String) {
    PHOTO("Photo", "🖼️"),
    VIDEO("Video", "🎬"),
    DOCUMENT("Document", "📄")
}

data class MediaAttachment(
    val uri: Uri,
    val type: MediaType,
    val displayName: String,
)

sealed class CategoryChoice {
    abstract val label: String
    abstract val emoji: String
    abstract val color: Color

    data class Predefined(
        override val label: String,
        override val emoji: String,
        override val color: Color,
    ) : CategoryChoice()

    data class Custom(
        override val label: String,
        override val emoji: String,
        override val color: Color = AccentYellow,
    ) : CategoryChoice()
}

private fun CategoryChoice.toEventCategory(): EventCategory = when (this) {
    is CategoryChoice.Predefined -> when (label.lowercase()) {
        "academic" -> EventCategory.CAMPUS
        "social" -> EventCategory.ENTERTAINMENT
        "career" -> EventCategory.BUSINESS
        "workshop" -> EventCategory.TECH
        else -> EventCategory.CAMPUS
    }

    is CategoryChoice.Custom -> EventCategory.CAMPUS
}

enum class EventVisibility(val label: String, val desc: String, val icon: String) {
    PUBLIC("Public", "Visible to everyone", "🌍"),
    FRIENDS("Friends", "Visible to friends only", "👥"),
    PRIVATE("Private", "Visible to you only", "🔒")
}

@Composable
private fun CreateEventPalette(): ScreenPalette {
    val cs = MaterialTheme.colorScheme
    return remember(cs) {
        ScreenPalette(
            bg = GroupColors.Bg,
            card = GroupColors.Card,
            surface = GroupColors.Surface,
            border = GroupColors.Border,
            text = TextPrimary,
            text2 = TextSecondary,
            text3 = TextTertiary,
            accent = Primary,
            accentSoft = Primary.copy(alpha = 0.18f),
            primary = Primary,
            primarySoft = Primary.copy(alpha = 0.16f),
        )
    }
}

private data class ScreenPalette(
    val bg: Color,
    val card: Color,
    val surface: Color,
    val border: Color,
    val text: Color,
    val text2: Color,
    val text3: Color,
    val accent: Color,
    val accentSoft: Color,
    val primary: Color,
    val primarySoft: Color,
)

// ─────────────────────────────────────────────────────────────────────────────
//  Screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CreateEventScreen(
    onBack: () -> Unit,
    onPublish: (NewEventData) -> Unit,
) {
    val p = CreateEventPalette()
    var currentStep by remember { mutableIntStateOf(1) }

    // State for all steps
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("🎉") }
    var selectedChoice by remember { mutableStateOf<CategoryChoice?>(null) }
    var showCustomCatDialog by remember { mutableStateOf(false) }
    var customCategories by remember { mutableStateOf<List<CategoryChoice.Custom>>(emptyList()) }

    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var organiser by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }

    var attachments by remember { mutableStateOf<List<MediaAttachment>>(emptyList()) }
    var showTextInput by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }

    var visibility by remember { mutableStateOf(EventVisibility.PUBLIC) }
    var limitCapacity by remember { mutableStateOf(false) }
    var capacity by remember { mutableStateOf(50) }
    var rsvpRequired by remember { mutableStateOf(true) }
    var allowComments by remember { mutableStateOf(true) }
    var shareToFeed by remember { mutableStateOf(true) }
    var tags by remember { mutableStateOf<List<String>>(emptyList()) }
    var tagInput by remember { mutableStateOf("") }

    val context = LocalContext.current
    val mediaPickers = rememberMediaPickers(
        onResult = { uri, type ->
            uri?.let {
                val name = it.getFileName(context)
                attachments = attachments + MediaAttachment(it, type, name)
            }
        }
    )

    val step1Valid = title.isNotBlank() && description.length >= 10 && selectedChoice != null
    val step2Valid = date.isNotBlank() && time.isNotBlank() && location.isNotBlank()
    val step3Valid = true // Optional
    val allValid = step1Valid && step2Valid && step3Valid

    if (showCustomCatDialog) {
        CustomCategoryDialog(
            onDismiss = { showCustomCatDialog = false },
            onAdd = { label, emoji ->
                val newCat = CategoryChoice.Custom(label, emoji)
                customCategories = customCategories + newCat
                selectedChoice = newCat
                showCustomCatDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            StepperTopBar(
                currentStep = currentStep,
                onBack = {
                    if (currentStep > 1) currentStep--
                    else onBack()
                },
                p = p,
            )
        },
        bottomBar = {
            StepperBottom(
                currentStep = currentStep,
                onNext = { if (currentStep < 4) currentStep++ },
                onPrev = { if (currentStep > 1) currentStep-- },
                nextEnabled = when (currentStep) {
                    1 -> step1Valid
                    2 -> step2Valid
                    3 -> step3Valid
                    else -> false
                },
                isLastStep = currentStep == 4,
                onPublish = {
                    val categoryChoice = selectedChoice ?: return@StepperBottom
                    onPublish(
                        NewEventData(
                            title = title,
                            description = description,
                            date = date,
                            time = time,
                            location = location,
                            organiser = organiser,
                            website = website,
                            coverEmoji = selectedEmoji,
                            category = categoryChoice.toEventCategory(),
                            tags = tags,
                            attachments = attachments,
                            textInput = textInput,
                            visibility = visibility,
                            limitCapacity = limitCapacity,
                            capacity = if (limitCapacity) capacity else 0,
                            rsvpRequired = rsvpRequired,
                            allowComments = allowComments,
                            shareToFeed = shareToFeed
                        )
                    )
                },
                allStepsValid = allValid,
                p = p,
            )
        },
        containerColor = p.bg,
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            AnimatedVisibility(visible = currentStep == 1, enter = fadeIn(), exit = fadeOut()) {
                Step1Basics(
                    title = title, onTitleChange = { title = it },
                    description = description, onDescriptionChange = { description = it },
                    selectedEmoji = selectedEmoji, onEmojiSelect = { selectedEmoji = it },
                    selectedChoice = selectedChoice, onCategorySelect = { selectedChoice = it },
                    customCategories = customCategories, onShowCustomCatDialog = { showCustomCatDialog = true }
                )
            }
            AnimatedVisibility(visible = currentStep == 2, enter = fadeIn(), exit = fadeOut()) {
                Step2Details(
                    date = date, onDateChange = { date = it },
                    time = time, onTimeChange = { time = it },
                    location = location, onLocationChange = { location = it },
                    organiser = organiser, onOrganiserChange = { organiser = it },
                    website = website, onWebsiteChange = { website = it }
                )
            }
            AnimatedVisibility(visible = currentStep == 3, enter = fadeIn(), exit = fadeOut()) {
                Step3Media(
                    attachments = attachments,
                    onAddMedia = { type ->
                        when (type) {
                            MediaType.PHOTO -> mediaPickers.photo.launch("image/*")
                            MediaType.VIDEO -> mediaPickers.video.launch("video/*")
                            MediaType.DOCUMENT -> mediaPickers.doc.launch("*/*")
                        }
                    },
                    onRemoveAttachment = { attachments = attachments - it },
                    showTextInput = showTextInput,
                    onToggleText = { showTextInput = !showTextInput },
                    textInput = textInput,
                    onTextInput = {
                        textInput = it
                        if (title.isBlank()) title = it
                    },
                    tags = tags,
                    tagInput = tagInput,
                    onTagInputChange = { tagInput = it },
                    onAddTag = {
                        if (tagInput.isNotBlank() && tags.size < 8) {
                            tags = tags + tagInput.trim()
                            tagInput = ""
                        }
                    },
                    onRemoveTag = { tags = tags - it }
                )
            }
            AnimatedVisibility(visible = currentStep == 4, enter = fadeIn(), exit = fadeOut()) {
                Step4Settings(
                    visibility = visibility, onVisibilityChange = { visibility = it },
                    limitCapacity = limitCapacity, onLimitCapacityChange = { limitCapacity = it },
                    capacity = capacity, onCapacityChange = { capacity = it },
                    rsvpRequired = rsvpRequired, onRsvpRequiredChange = { rsvpRequired = it },
                    allowComments = allowComments, onAllowCommentsChange = { allowComments = it },
                    shareToFeed = shareToFeed, onShareToFeedChange = { shareToFeed = it }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Step 1: Basics
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun Step1Basics(
    title: String, onTitleChange: (String) -> Unit,
    description: String, onDescriptionChange: (String) -> Unit,
    selectedEmoji: String, onEmojiSelect: (String) -> Unit,
    selectedChoice: CategoryChoice?, onCategorySelect: (CategoryChoice) -> Unit,
    customCategories: List<CategoryChoice.Custom>, onShowCustomCatDialog: () -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text("EVENT BASICS", color = TextTertiary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            EventTextField(
                value = title,
                onValue = onTitleChange,
                label = "Event Title *",
                placeholder = "e.g. Campus Music Festival",
                icon = Icons.Outlined.Title,
                maxLength = 80
            )
        }
        item {
            EventTextField(
                value = description,
                onValue = onDescriptionChange,
                label = "Event Description *",
                placeholder = "Tell everyone what your event is about...",
                icon = Icons.Outlined.Edit,
                singleLine = false,
                maxLines = 5,
                maxLength = 500
            )
        }
        item {
            Text("COVER EMOJI", color = TextTertiary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            EmojiPickerRow(selected = selectedEmoji, onSelect = onEmojiSelect)
        }
        item {
            Text("CATEGORY *", color = TextTertiary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            CategoryGrid(
                selected = selectedChoice,
                customCategories = customCategories,
                onSelect = onCategorySelect,
                onAddCustom = onShowCustomCatDialog,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Step 2: Details
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun Step2Details(
    date: String, onDateChange: (String) -> Unit,
    time: String, onTimeChange: (String) -> Unit,
    location: String, onLocationChange: (String) -> Unit,
    organiser: String, onOrganiserChange: (String) -> Unit,
    website: String, onWebsiteChange: (String) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text("EVENT DETAILS", color = TextTertiary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            EventTextField(
                value = date,
                onValue = onDateChange,
                label = "Date *",
                placeholder = "e.g. July 26, 2024",
                icon = Icons.Outlined.DateRange
            )
        }
        item {
            EventTextField(
                value = time,
                onValue = onTimeChange,
                label = "Time *",
                placeholder = "e.g. 7:00 PM - 10:00 PM",
                icon = Icons.Outlined.Schedule
            )
        }
        item {
            EventTextField(
                value = location,
                onValue = onLocationChange,
                label = "Location / Room *",
                placeholder = "e.g. Grand Hall or Online",
                icon = Icons.Outlined.LocationOn
            )
        }
        item {
            Text("ADDITIONAL INFO (OPTIONAL)", color = TextTertiary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            EventTextField(
                value = organiser,
                onValue = onOrganiserChange,
                label = "Organiser",
                placeholder = "e.g. Student Union",
                icon = Icons.Outlined.Person
            )
        }
        item {
            EventTextField(
                value = website,
                onValue = onWebsiteChange,
                label = "Website / Link",
                placeholder = "e.g. campus.events/music",
                icon = Icons.Outlined.Public
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Step 3: Media & Tags
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun Step3Media(
    attachments: List<MediaAttachment>,
    onAddMedia: (MediaType) -> Unit,
    onRemoveAttachment: (MediaAttachment) -> Unit,
    showTextInput: Boolean,
    onToggleText: () -> Unit,
    textInput: String,
    onTextInput: (String) -> Unit,
    tags: List<String>,
    tagInput: String,
    onTagInputChange: (String) -> Unit,
    onAddTag: () -> Unit,
    onRemoveTag: (String) -> Unit,
) {
    val p = CreateEventPalette()
    var selectedMediaTab by remember { mutableStateOf(MediaType.PHOTO) }
    val (zoneTitle, zoneSubtitle, buttonLabel) = remember(selectedMediaTab) {
        val title = when (selectedMediaTab) {
            MediaType.PHOTO -> "Drop photos here"
            MediaType.VIDEO -> "Drop a video here"
            MediaType.DOCUMENT -> "Write something about your event"
        }
        val subtitle = when (selectedMediaTab) {
            MediaType.PHOTO -> "Tap to browse — multiple photos supported"
            MediaType.VIDEO -> "MP4, MOV or WebM — one clip"
            MediaType.DOCUMENT -> "Highlights, what to bring, directions…"
        }
        val label = when (selectedMediaTab) {
            MediaType.PHOTO -> "Choose Photos"
            MediaType.VIDEO -> "Choose Video"
            MediaType.DOCUMENT -> "Add Text"
        }
        Triple(title, subtitle, label)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Media Section
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("ATTACHMENTS", color = TextTertiary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)

            // Segmented tabs (Photo / Video / Text)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(p.card)
                    .border(1.dp, p.border, RoundedCornerShape(16.dp))
                    .height(62.dp),
            ) {
                fun tabBg(active: Boolean) = if (active) p.primarySoft.copy(alpha = 0.75f) else Color.Transparent
                fun tabText(active: Boolean) = if (active) p.primary else p.text3

                val tabShapeLeft = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                val tabShapeMid = RoundedCornerShape(0.dp)
                val tabShapeRight = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)

                @Composable
                fun TabButton(
                    label: String,
                    emoji: String,
                    active: Boolean,
                    shape: RoundedCornerShape,
                    onClick: () -> Unit,
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clip(shape)
                            .background(tabBg(active))
                            .clickable(onClick = onClick)
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(emoji, fontSize = 18.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(label, color = tabText(active), fontSize = 12.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
                    }
                }

                TabButton(
                    label = "Photo",
                    emoji = MediaType.PHOTO.emoji,
                    active = selectedMediaTab == MediaType.PHOTO,
                    shape = tabShapeLeft,
                    onClick = { selectedMediaTab = MediaType.PHOTO },
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxSize()
                        .background(p.border.copy(alpha = 0.7f))
                )
                TabButton(
                    label = "Video",
                    emoji = MediaType.VIDEO.emoji,
                    active = selectedMediaTab == MediaType.VIDEO,
                    shape = tabShapeMid,
                    onClick = { selectedMediaTab = MediaType.VIDEO },
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxSize()
                        .background(p.border.copy(alpha = 0.7f))
                )
                TabButton(
                    label = "Text",
                    emoji = "📝",
                    active = selectedMediaTab == MediaType.DOCUMENT,
                    shape = tabShapeRight,
                    onClick = {
                        selectedMediaTab = MediaType.DOCUMENT
                        if (!showTextInput) onToggleText()
                    },
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(p.surface)
                    .border(1.dp, p.border.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                    .padding(16.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            width = 2.dp,
                            color = p.border.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(16.dp),
                        )
                        .padding(vertical = 22.dp, horizontal = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    val iconEmoji = when (selectedMediaTab) {
                        MediaType.PHOTO -> "🖼️"
                        MediaType.VIDEO -> "🎬"
                        MediaType.DOCUMENT -> "✍️"
                    }
                    Text(iconEmoji, fontSize = 30.sp)
                    Text(zoneTitle, color = p.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(zoneSubtitle, color = p.text3, fontSize = 12.sp)

                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            when (selectedMediaTab) {
                                MediaType.PHOTO -> onAddMedia(MediaType.PHOTO)
                                MediaType.VIDEO -> onAddMedia(MediaType.VIDEO)
                                MediaType.DOCUMENT -> onToggleText()
                            }
                        },
                        shape = RoundedCornerShape(999.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, p.primary.copy(alpha = 0.5f)),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            containerColor = p.primarySoft.copy(alpha = 0.25f),
                            contentColor = p.primary,
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        Text(buttonLabel, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            AnimatedVisibility(visible = showTextInput) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = onTextInput,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    label = { Text("Add Text Content", fontSize = 12.sp) },
                    placeholder = { Text("Type text that will sync to Event Title…", color = p.text3, fontSize = 12.sp) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = p.primary,
                        unfocusedBorderColor = p.border,
                        cursorColor = p.primary,
                        focusedLabelColor = p.primary,
                        unfocusedLabelColor = p.text3,
                        focusedContainerColor = p.card,
                        unfocusedContainerColor = p.card,
                    ),
                )
            }

            if (attachments.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    attachments.forEach { att ->
                        ChipWithRemove(
                            label = att.displayName,
                            emoji = att.type.emoji,
                            onRemove = { onRemoveAttachment(att) }
                        )
                    }
                }
            }
        }

        // Tags Section
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("ADD TAGS (UP TO 8)", color = TextTertiary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val focusManager = LocalFocusManager.current
                OutlinedTextField(
                    value = tagInput,
                    onValueChange = { if (it.length <= 25) onTagInputChange(it) },
                    modifier = Modifier.weight(1f),
                    label = { Text("Add Tag", fontSize = 12.sp) },
                    placeholder = { Text("#music, #free, #outdoor…", color = p.text3, fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = p.primary,
                        unfocusedBorderColor = p.border,
                        cursorColor = p.primary,
                        focusedLabelColor = p.primary,
                        unfocusedLabelColor = p.text3,
                        focusedContainerColor = p.card,
                        unfocusedContainerColor = p.card,
                    ),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        onAddTag()
                        focusManager.clearFocus()
                    })
                )
                val canAdd = tagInput.isNotBlank() && tags.size < 8
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (canAdd) p.accent else p.card)
                        .border(1.dp, if (canAdd) p.accent else p.border, RoundedCornerShape(12.dp))
                        .clickable(enabled = canAdd, onClick = onAddTag),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.size(20.dp))
                }
            }

            if (tags.isNotEmpty()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    tags.forEach { tag ->
                        ChipWithRemove(
                            label = "#$tag",
                            onRemove = { onRemoveTag(tag) }
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Step 4: Settings
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun Step4Settings(
    visibility: EventVisibility, onVisibilityChange: (EventVisibility) -> Unit,
    limitCapacity: Boolean, onLimitCapacityChange: (Boolean) -> Unit,
    capacity: Int, onCapacityChange: (Int) -> Unit,
    rsvpRequired: Boolean, onRsvpRequiredChange: (Boolean) -> Unit,
    allowComments: Boolean, onAllowCommentsChange: (Boolean) -> Unit,
    shareToFeed: Boolean, onShareToFeedChange: (Boolean) -> Unit,
) {
    val p = CreateEventPalette()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Visibility
        Text("VISIBILITY", color = TextTertiary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EventVisibility.entries.forEach { v ->
                val isSelected = v == visibility
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) p.primarySoft else p.card)
                        .border(1.dp, if (isSelected) p.primary else p.border, RoundedCornerShape(14.dp))
                        .clickable { onVisibilityChange(v) }
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(v.icon, fontSize = 20.sp)
                        Text(
                            text = v.label,
                            color = if (isSelected) p.primary else p.text3,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        // Capacity
        Text("CAPACITY & REGISTRATION", color = TextTertiary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        SettingsToggle(
            title = "Limit event capacity",
            subtitle = "Set a maximum number of attendees",
            checked = limitCapacity,
            onChecked = onLimitCapacityChange
        )

        AnimatedVisibility(visible = limitCapacity) {
            OutlinedTextField(
                value = capacity.toString(),
                onValueChange = { value ->
                    onCapacityChange(value.filter { it.isDigit() }.take(4).toIntOrNull() ?: 0)
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                label = { Text("Max Attendees") },
                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = p.primary,
                    unfocusedBorderColor = p.border,
                    cursorColor = p.primary,
                    focusedLabelColor = p.primary,
                    unfocusedLabelColor = p.text3,
                    focusedContainerColor = p.card,
                    unfocusedContainerColor = p.card,
                ),
            )
        }

        SettingsToggle(
            title = "RSVP required",
            subtitle = "Attendees must register to join",
            checked = rsvpRequired,
            onChecked = onRsvpRequiredChange
        )

        // Other Settings
        Text("OTHER SETTINGS", color = TextTertiary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        SettingsToggle(
            title = "Allow comments",
            subtitle = "People can post questions and comments",
            checked = allowComments,
            onChecked = onAllowCommentsChange
        )
        SettingsToggle(
            title = "Share to campus feed",
            subtitle = "Show on the main discover page",
            checked = shareToFeed,
            onChecked = onShareToFeedChange
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Common Components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StepperTopBar(currentStep: Int, onBack: () -> Unit, p: ScreenPalette) {
    Surface(color = p.bg, shadowElevation = 2.dp) {
        Column(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(bottom = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(p.card)
                        .border(1.dp, p.border, CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = p.text2, modifier = Modifier.size(20.dp)) }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text("Create Event", color = p.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Step $currentStep of 4 — ${listOf("Basics", "Details", "Media", "Settings")[currentStep - 1]}", color = p.text3, fontSize = 13.sp)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(4) { idx ->
                    val stepNum = idx + 1
                    val isActive = stepNum <= currentStep
                    val color = if (isActive) p.accent else p.border
                    Box(
                        modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(color)
                    )
                }
            }
        }
    }
}

@Composable
private fun StepperBottom(
    currentStep: Int,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    nextEnabled: Boolean,
    isLastStep: Boolean,
    onPublish: () -> Unit,
    allStepsValid: Boolean,
    p: ScreenPalette,
) {
    Surface(color = p.bg, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Prev Button
            if (currentStep > 1) {
                OutlinedButton(
                    onClick = onPrev,
                    modifier = Modifier.height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = p.text2),
                    border = BorderStroke(1.dp, p.border)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                }
            }

            // Next / Publish Button
            Button(
                onClick = if (isLastStep) onPublish else onNext,
                modifier = Modifier.weight(1f).height(52.dp),
                enabled = if (isLastStep) allStepsValid else nextEnabled,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = p.accent,
                    contentColor = Color.Black,
                    disabledContainerColor = p.card,
                    disabledContentColor = p.text3,
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 2.dp)
            ) {
                Text(
                    text = when {
                        isLastStep && allStepsValid -> "🚀 Publish Event"
                        isLastStep && !allStepsValid -> "Complete all steps"
                        else -> "Continue"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                if (!isLastStep) {
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                }
            }
        }
    }
}

@Composable
private fun EventTextField(
    value: String,
    onValue: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    maxLength: Int = Int.MAX_VALUE,
) {
    val p = CreateEventPalette()
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= maxLength) onValue(it) },
        modifier = modifier.fillMaxWidth(),
        label = { Text(label, fontSize = 13.sp) },
        placeholder = { Text(placeholder, color = p.text3, fontSize = 13.sp) },
        leadingIcon = {
            Icon(icon, null, tint = if (value.isNotBlank()) p.primary else p.text3, modifier = Modifier.size(18.dp))
        },
        trailingIcon = if (value.isNotBlank() && singleLine) ({
            Icon(
                imageVector = Icons.Default.Cancel,
                contentDescription = "Clear",
                tint = p.text3,
                modifier = Modifier.size(17.dp).clickable { onValue("") },
            )
        }) else null,
        singleLine = singleLine,
        maxLines = maxLines,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = p.primary,
            unfocusedBorderColor = p.border,
            cursorColor = p.primary,
            focusedLabelColor = p.primary,
            unfocusedLabelColor = p.text3,
            focusedContainerColor = p.card,
            unfocusedContainerColor = p.card,
        ),
    )
}

@Composable
private fun EmojiPickerRow(selected: String, onSelect: (String) -> Unit) {
    val p = CreateEventPalette()
    val emojis = listOf("🎉", "🎓", "💼", "💻", "🎨", "🎵", "🔬", "🏆")
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        emojis.forEach { emoji ->
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (selected == emoji) p.primarySoft else p.card)
                    .border(1.dp, if (selected == emoji) p.primary else p.border, CircleShape)
                    .clickable { onSelect(emoji) },
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 22.sp)
            }
        }
    }
}

@Composable
private fun CategoryGrid(
    selected: CategoryChoice?,
    customCategories: List<CategoryChoice.Custom>,
    onSelect: (CategoryChoice) -> Unit,
    onAddCustom: () -> Unit,
) {
    val p = CreateEventPalette()
    val predefinedCats = listOf(
        CategoryChoice.Predefined("Academic", "🎓", p.primary),
        CategoryChoice.Predefined("Social", "🎉", p.accent),
        CategoryChoice.Predefined("Career", "💼", p.primary),
        CategoryChoice.Predefined("Workshop", "💻", p.primary),
    )
    val allCats = predefinedCats + customCategories
    val chunkedCats = allCats.chunked(4)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        chunkedCats.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { choice ->
                    val isSelected = selected == choice
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) choice.color.copy(alpha = 0.18f) else p.card)
                            .border(1.5.dp, if (isSelected) choice.color else p.border, RoundedCornerShape(14.dp))
                            .clickable { onSelect(choice) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(choice.emoji, fontSize = 18.sp)
                            Text(
                                text = choice.label,
                                color = if (isSelected) choice.color else p.text3,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                            )
                        }
                    }
                }
                if (row.size < 4) {
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        // Add custom button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(p.card)
                .border(1.dp, p.border, RoundedCornerShape(14.dp))
                .clickable { onAddCustom() }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Add, null, tint = p.text2)
                Text("Add Custom Category", color = p.text2, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun CustomCategoryDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit,
) {
    var customCatLabel by remember { mutableStateOf("") }
    var customCatEmoji by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SurfaceDark,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("New Category", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = customCatLabel,
                    onValueChange = { customCatLabel = it },
                    label = { Text("Category Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = customCatEmoji,
                    onValueChange = { customCatEmoji = it },
                    label = { Text("Emoji") },
                    singleLine = true
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onAdd(customCatLabel, customCatEmoji) },
                        enabled = customCatLabel.isNotBlank() && customCatEmoji.isNotBlank()
                    ) {
                        Text("Add")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
) {
    val p = CreateEventPalette()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(p.card)
            .border(1.dp, p.border, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = p.text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = p.text3, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(
                checkedThumbColor = p.accent,
                checkedTrackColor = p.accent.copy(alpha = 0.35f),
                uncheckedThumbColor = p.text3,
                uncheckedTrackColor = p.surface
            ),
        )
    }
}

@Composable
private fun ChipWithRemove(label: String, onRemove: () -> Unit, emoji: String? = null) {
    val p = CreateEventPalette()
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(p.primarySoft.copy(alpha = 0.55f))
            .border(1.dp, p.primary.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (emoji != null) {
                Text(emoji, fontSize = 12.sp)
            }
            Text(
                text = if (label.length > 25) label.take(25) + "…" else label,
                color = p.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove",
                tint = p.primary.copy(alpha = 0.7f),
                modifier = Modifier
                    .size(14.dp)
                    .clickable(onClick = onRemove),
            )
        }
    }
}

data class MediaPickers(
    val photo: androidx.activity.result.ActivityResultLauncher<String>,
    val video: androidx.activity.result.ActivityResultLauncher<String>,
    val doc: androidx.activity.result.ActivityResultLauncher<String>,
)

@Composable
fun rememberMediaPickers(onResult: (Uri?, MediaType) -> Unit) = MediaPickers(
    photo = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        onResult(it, MediaType.PHOTO)
    },
    video = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        onResult(it, MediaType.VIDEO)
    },
    doc = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        onResult(it, MediaType.DOCUMENT)
    },
)