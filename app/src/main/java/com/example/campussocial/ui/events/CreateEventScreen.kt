@file:Suppress("SpellCheckingInspection")
package com.example.kampus.ui.events
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kampus.ui.events.EventColors as C

// ─────────────────────────────────────────────────────────────────────────────
//  Custom category model (user-created)
// ─────────────────────────────────────────────────────────────────────────────
data class CustomCategory(
    val label : String,
    val emoji : String,
    val color : Color,
)

// ─────────────────────────────────────────────────────────────────────────────
//  Sealed category choice — preset or user-created
// ─────────────────────────────────────────────────────────────────────────────
sealed class CategoryChoice {
    data class Preset(val category: EventCategory) : CategoryChoice()
    data class Custom(val category: CustomCategory) : CategoryChoice()

    val label: String get() = when (this) {
        is Preset -> category.label
        is Custom -> category.label
    }
    val emoji: String get() = when (this) {
        is Preset -> category.emoji
        is Custom -> category.emoji
    }
    val color: Color get() = when (this) {
        is Preset -> category.color
        is Custom -> category.color
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Media attachment
// ─────────────────────────────────────────────────────────────────────────────
enum class MediaType(val label: String, val emoji: String) {
    PHOTO("Photo", "🖼️"),
    VIDEO("Video", "🎬"),
    FLYER("Flyer", "📄"),
    LINK ("Link",  "🔗"),
}

data class MediaAttachment(
    val type  : MediaType,
    val value : String = "",
)

// ─────────────────────────────────────────────────────────────────────────────
//  Visibility
// ─────────────────────────────────────────────────────────────────────────────
enum class EventVisibility(val label: String, val icon: String, val desc: String) {
    PUBLIC ("Public",  "🌍", "Anyone can see & join"),
    CAMPUS ("Campus",  "🎓", "Campus members only"),
    PRIVATE("Private", "🔒", "Invite only"),
}

// ─────────────────────────────────────────────────────────────────────────────
//  Output data
// ─────────────────────────────────────────────────────────────────────────────
data class NewEventData(
    val title       : String,
    val description : String,
    val date        : String,
    val time        : String,
    val location    : String,
    val category    : EventCategory,
    val coverEmoji  : String,
    val tags        : List<String>     = emptyList(),
    val mediaUrls   : List<String>     = emptyList(),
    val visibility  : EventVisibility  = EventVisibility.PUBLIC,
    val capacity    : String           = "",
)

// ─────────────────────────────────────────────────────────────────────────────
//  Screen
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    onBack    : () -> Unit = {},
    onPublish : (NewEventData) -> Unit = {},
) {
    var title           by remember { mutableStateOf("") }
    var description     by remember { mutableStateOf("") }
    var date            by remember { mutableStateOf("") }
    var time            by remember { mutableStateOf("") }
    var location        by remember { mutableStateOf("") }
    var capacity        by remember { mutableStateOf("") }
    var selectedChoice  by remember { mutableStateOf<CategoryChoice?>(null) }
    var selectedEmoji   by remember { mutableStateOf("🎉") }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var tagInput        by remember { mutableStateOf("") }
    var tags            by remember { mutableStateOf(listOf<String>()) }
    var attachments     by remember { mutableStateOf(listOf<MediaAttachment>()) }
    var linkInput       by remember { mutableStateOf("") }
    var showLinkInput   by remember { mutableStateOf(false) }
    var visibility      by remember { mutableStateOf(EventVisibility.PUBLIC) }

    // custom category dialog state
    var showCustomCatDialog by remember { mutableStateOf(false) }
    var customCategories    by remember { mutableStateOf(listOf<CustomCategory>()) }
    var customCatLabel      by remember { mutableStateOf("") }
    var customCatEmoji      by remember { mutableStateOf("⭐") }

    val isValid = title.isNotBlank() && date.isNotBlank() &&
            time.isNotBlank() && location.isNotBlank() && selectedChoice != null

    val filledFields = listOf(
        title.isNotBlank(), description.isNotBlank(), date.isNotBlank(),
        time.isNotBlank(), location.isNotBlank(), selectedChoice != null,
    ).count { it }
    val progress by animateFloatAsState(
        targetValue   = filledFields / 6f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label         = "progress",
    )

    val handlePublish: (NewEventData) -> Unit = onPublish

    // ── Custom category dialog ────────────────────────────────────────────────
    if (showCustomCatDialog) {
        AlertDialog(
            onDismissRequest = { showCustomCatDialog = false },
            containerColor   = C.Card,
            shape            = RoundedCornerShape(20.dp),
            title = {
                Text("New Category", color = C.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Pick an emoji and name your category.", color = C.Gray3, fontSize = 13.sp)
                    Row(
                        modifier              = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf("⭐","🎯","🧪","🏋️","🌿","🎲","🧠","🎪","🚂","🌊").forEach { e ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (customCatEmoji == e) C.BlueSoft else C.Surface)
                                    .border(1.dp, if (customCatEmoji == e) C.Blue else C.Border, RoundedCornerShape(10.dp))
                                    .clickable(remember { MutableInteractionSource() }, null) { customCatEmoji = e },
                                contentAlignment = Alignment.Center,
                            ) { Text(e, fontSize = 20.sp) }
                        }
                    }
                    OutlinedTextField(
                        value         = customCatLabel,
                        onValueChange = { if (it.length <= 20) customCatLabel = it },
                        label         = { Text("Category Name", fontSize = 12.sp) },
                        placeholder   = { Text("e.g. Wellness, Gaming…", color = C.Gray5, fontSize = 12.sp) },
                        singleLine    = true,
                        shape         = RoundedCornerShape(12.dp),
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedTextColor        = C.White,
                            unfocusedTextColor      = C.White,
                            focusedBorderColor      = C.Blue,
                            unfocusedBorderColor    = C.Border,
                            cursorColor             = C.Blue,
                            focusedLabelColor       = C.Blue,
                            unfocusedLabelColor     = C.Gray4,
                            focusedContainerColor   = C.Surface,
                            unfocusedContainerColor = C.Surface,
                        ),
                    )
                }
            },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (customCatLabel.isNotBlank()) C.Blue else C.Gray5)
                        .clickable(
                            enabled           = customCatLabel.isNotBlank(),
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = null,
                        ) {
                            val newCat = CustomCategory(
                                label = customCatLabel.trim(),
                                emoji = customCatEmoji,
                                color = Color(0xFF3B82F6),
                            )
                            customCategories    = customCategories + newCat
                            selectedChoice      = CategoryChoice.Custom(newCat)
                            customCatLabel      = ""
                            customCatEmoji      = "⭐"
                            showCustomCatDialog = false
                        }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                ) {
                    Text("Add", color = C.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomCatDialog = false }) {
                    Text("Cancel", color = C.Gray3, fontSize = 13.sp)
                }
            },
        )
    }

    Scaffold(
        containerColor = C.Bg,
        topBar  = { CreateEventTopBar(onBack = onBack, progress = progress) },
        bottomBar = {
            PublishButton(
                enabled = isValid,
                onClick = {
                    val resolvedCat = when (val c = selectedChoice) {
                        is CategoryChoice.Preset -> c.category
                        else                     -> EventCategory.CAMPUS
                    }
                    handlePublish(
                        NewEventData(
                            title       = title,
                            description = description,
                            date        = date,
                            time        = time,
                            location    = location,
                            category    = resolvedCat,
                            coverEmoji  = selectedEmoji,
                            tags        = tags,
                            mediaUrls   = attachments.map { it.value },
                            visibility  = visibility,
                            capacity    = capacity,
                        )
                    )
                }
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {

            // ── Cover preview ─────────────────────────────────────────────────
            CoverPreviewCard(
                emoji       = selectedEmoji,
                color1      = selectedChoice?.color?.copy(alpha = 0.28f) ?: C.Surface,
                color2      = selectedChoice?.color?.copy(alpha = 0.06f) ?: C.Card,
                choiceLabel = selectedChoice?.label,
                choiceEmoji = selectedChoice?.emoji,
                choiceColor = selectedChoice?.color,
                title       = title.ifBlank { "Event Title" },
                hasImage    = attachments.any { it.type == MediaType.PHOTO },
                onEmojiClick = { showEmojiPicker = !showEmojiPicker },
            )

            AnimatedVisibility(
                visible = showEmojiPicker,
                enter   = fadeIn(tween(200)) + expandVertically(tween(250)),
                exit    = fadeOut(tween(150)) + shrinkVertically(tween(200)),
            ) {
                EmojiPickerRow(selected = selectedEmoji, onSelect = { selectedEmoji = it; showEmojiPicker = false })
            }

            // ── Media ─────────────────────────────────────────────────────────
            SectionHeader(icon = "📎", title = "Media & Attachments")
            MediaSection(
                attachments   = attachments,
                showLinkInput = showLinkInput,
                linkInput     = linkInput,
                onLinkInput   = { linkInput = it },
                onAddMedia    = { type ->
                    if (type == MediaType.LINK) {
                        showLinkInput = !showLinkInput
                    } else if (attachments.none { it.type == type }) {
                        attachments = attachments + MediaAttachment(type, "placeholder_${type.name}")
                    }
                },
                onAddLink = {
                    if (linkInput.isNotBlank()) {
                        attachments   = attachments + MediaAttachment(MediaType.LINK, linkInput)
                        linkInput     = ""
                        showLinkInput = false
                    }
                },
                onRemove = { att -> attachments = attachments - att },
            )

            // ── Event Details ─────────────────────────────────────────────────
            SectionHeader(icon = "✏️", title = "Event Details")

            EventTextField(
                value = title, onValue = { title = it },
                label = "Event Title *", placeholder = "e.g. Campus Hackathon 2026",
                icon = Icons.Outlined.EmojiEvents, maxLength = 60,
            )
            EventTextField(
                value = description, onValue = { description = it },
                label = "Description", placeholder = "Tell people what to expect…",
                icon = Icons.Outlined.Notes, singleLine = false, maxLines = 5,
            )

            // ── Category ──────────────────────────────────────────────────────
            SectionHeader(icon = "🏷️", title = "Category *")
            CategoryGrid(
                selected         = selectedChoice,
                customCategories = customCategories,
                onSelect         = { selectedChoice = it },
                onAddCustom      = { showCustomCatDialog = true },
            )

            // ── Tags ──────────────────────────────────────────────────────────
            SectionHeader(icon = "🔖", title = "Tags")
            TagsSection(
                tags     = tags,
                input    = tagInput,
                onInput  = { tagInput = it },
                onAdd    = {
                    val t = tagInput.trim().lowercase().removePrefix("#")
                    if (t.isNotBlank() && tags.size < 8 && !tags.contains(t)) {
                        tags = tags + t; tagInput = ""
                    }
                },
                onRemove = { tags = tags - it },
            )

            // ── Date & Time ───────────────────────────────────────────────────
            SectionHeader(icon = "📅", title = "Date & Time *")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                EventTextField(
                    value = date, onValue = { date = it },
                    label = "Date", placeholder = "March 28, 2026",
                    icon = Icons.Outlined.CalendarMonth, modifier = Modifier.weight(1f),
                )
                EventTextField(
                    value = time, onValue = { time = it },
                    label = "Time", placeholder = "6:00 PM",
                    icon = Icons.Outlined.Schedule, modifier = Modifier.weight(1f),
                )
            }

            // ── Location ──────────────────────────────────────────────────────
            SectionHeader(icon = "📍", title = "Location *")
            EventTextField(
                value = location, onValue = { location = it },
                label = "Venue / Address", placeholder = "e.g. Engineering Hall, Room 201",
                icon = Icons.Outlined.LocationOn,
            )

            // ── Event Settings ────────────────────────────────────────────────
            SectionHeader(icon = "⚙️", title = "Event Settings")
            VisibilitySelector(selected = visibility, onSelect = { visibility = it })
            EventTextField(
                value = capacity, onValue = { capacity = it },
                label = "Max Capacity", placeholder = "e.g. 100  (leave blank = unlimited)",
                icon = Icons.Outlined.People,
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Media Section
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun MediaSection(
    attachments   : List<MediaAttachment>,
    showLinkInput : Boolean,
    linkInput     : String,
    onLinkInput   : (String) -> Unit,
    onAddMedia    : (MediaType) -> Unit,
    onAddLink     : () -> Unit,
    onRemove      : (MediaAttachment) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MediaType.entries.forEach { type ->
                val isAdded = attachments.any { it.type == type }
                val scale by animateFloatAsState(
                    targetValue   = if (isAdded) 0.96f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label         = "mediaScale${type.name}",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .graphicsLayer(scaleX = scale, scaleY = scale)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isAdded) C.BlueSoft else C.Card)
                        .border(
                            width = if (isAdded) 1.5.dp else 1.dp,
                            color = if (isAdded) C.Blue else C.Border,
                            shape = RoundedCornerShape(14.dp),
                        )
                        .clickable(remember { MutableInteractionSource() }, null) { onAddMedia(type) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(type.emoji, fontSize = 20.sp)
                        Text(
                            text       = type.label,
                            color      = if (isAdded) C.Blue else C.Gray4,
                            fontSize   = 10.sp,
                            fontWeight = if (isAdded) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showLinkInput,
            enter   = fadeIn(tween(200)) + expandVertically(tween(250)),
            exit    = fadeOut(tween(150)) + shrinkVertically(tween(200)),
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value         = linkInput,
                    onValueChange = onLinkInput,
                    modifier      = Modifier.weight(1f),
                    label         = { Text("Paste URL", fontSize = 12.sp) },
                    placeholder   = { Text("https://…", color = C.Gray5, fontSize = 12.sp) },
                    singleLine    = true,
                    shape         = RoundedCornerShape(12.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedTextColor        = C.White,
                        unfocusedTextColor      = C.White,
                        focusedBorderColor      = C.Blue,
                        unfocusedBorderColor    = C.Border,
                        cursorColor             = C.Blue,
                        focusedLabelColor       = C.Blue,
                        unfocusedLabelColor     = C.Gray4,
                        focusedContainerColor   = C.Card,
                        unfocusedContainerColor = C.Card,
                    ),
                )
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (linkInput.isNotBlank()) C.Blue else C.Card)
                        .border(1.dp, if (linkInput.isNotBlank()) C.Blue else C.Border, RoundedCornerShape(12.dp))
                        .clickable(
                            enabled           = linkInput.isNotBlank(),
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = null,
                            onClick           = onAddLink,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Add, null, tint = C.White, modifier = Modifier.size(20.dp))
                }
            }
        }

        if (attachments.isNotEmpty()) {
            Row(
                modifier              = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                attachments.forEach { att ->
                    val chipLabel = if (att.type == MediaType.LINK && att.value.length > 22)
                        att.value.take(22) + "…" else att.type.label
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(C.Surface)
                            .border(1.dp, C.Blue.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(att.type.emoji, fontSize = 13.sp)
                            Text(chipLabel, color = C.Gray2, fontSize = 12.sp)
                            Icon(
                                imageVector        = Icons.Default.Close,
                                contentDescription = null,
                                tint               = C.Gray4,
                                modifier           = Modifier
                                    .size(14.dp)
                                    .clickable(remember { MutableInteractionSource() }, null) { onRemove(att) },
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Tags Section
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagsSection(
    tags    : List<String>,
    input   : String,
    onInput : (String) -> Unit,
    onAdd   : () -> Unit,
    onRemove: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value         = input,
                onValueChange = { if (it.length <= 20) onInput(it) },
                modifier      = Modifier.weight(1f),
                label         = { Text("Add Tag", fontSize = 12.sp) },
                placeholder   = { Text("#music, #free, #outdoor…", color = C.Gray5, fontSize = 12.sp) },
                singleLine    = true,
                shape         = RoundedCornerShape(12.dp),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedTextColor        = C.White,
                    unfocusedTextColor      = C.White,
                    focusedBorderColor      = C.Blue,
                    unfocusedBorderColor    = C.Border,
                    cursorColor             = C.Blue,
                    focusedLabelColor       = C.Blue,
                    unfocusedLabelColor     = C.Gray4,
                    focusedContainerColor   = C.Card,
                    unfocusedContainerColor = C.Card,
                ),
            )
            val canAdd = input.isNotBlank() && tags.size < 8
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (canAdd) C.Blue else C.Card)
                    .border(1.dp, if (canAdd) C.Blue else C.Border, RoundedCornerShape(12.dp))
                    .clickable(
                        enabled           = canAdd,
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null,
                        onClick           = onAdd,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Add, null, tint = C.White, modifier = Modifier.size(20.dp))
            }
        }

        if (tags.isNotEmpty()) {
            Row(
                modifier              = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                tags.forEach { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(C.BlueSoft.copy(alpha = 0.5f))
                            .border(1.dp, C.Blue.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Text("#$tag", color = C.Blue, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Icon(
                                imageVector        = Icons.Default.Close,
                                contentDescription = null,
                                tint               = C.Blue.copy(alpha = 0.7f),
                                modifier           = Modifier
                                    .size(13.dp)
                                    .clickable(remember { MutableInteractionSource() }, null) { onRemove(tag) },
                            )
                        }
                    }
                }
            }
        }

        if (tags.size >= 8) {
            Text("Max 8 tags reached", color = C.Amber, fontSize = 11.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Visibility Selector
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun VisibilitySelector(
    selected : EventVisibility,
    onSelect : (EventVisibility) -> Unit,
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        EventVisibility.entries.forEach { v ->
            val isSelected = v == selected
            val scale by animateFloatAsState(
                targetValue   = if (isSelected) 1.03f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label         = "visScale${v.name}",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer(scaleX = scale, scaleY = scale)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSelected) C.BlueSoft else C.Card)
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) C.Blue else C.Border,
                        shape = RoundedCornerShape(14.dp),
                    )
                    .clickable(remember { MutableInteractionSource() }, null) { onSelect(v) }
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(v.icon, fontSize = 20.sp)
                    Text(
                        text       = v.label,
                        color      = if (isSelected) C.Blue else C.Gray4,
                        fontSize   = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        textAlign  = TextAlign.Center,
                    )
                    Text(
                        text      = v.desc,
                        color     = C.Gray5,
                        fontSize  = 9.sp,
                        textAlign = TextAlign.Center,
                        maxLines  = 2,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Cover Preview Card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CoverPreviewCard(
    emoji        : String,
    color1       : Color,
    color2       : Color,
    choiceLabel  : String?,
    choiceEmoji  : String?,
    choiceColor  : Color?,
    title        : String,
    hasImage     : Boolean,
    onEmojiClick : () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(color1, color2)))
            .border(1.dp, C.Border, RoundedCornerShape(20.dp)),
    ) {
        if (choiceLabel != null && choiceColor != null) {
            Box(
                modifier = Modifier
                    .padding(14.dp).align(Alignment.TopStart)
                    .clip(RoundedCornerShape(20.dp))
                    .background(choiceColor.copy(alpha = 0.25f))
                    .border(1.dp, choiceColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text("${choiceEmoji ?: ""} $choiceLabel", color = choiceColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (hasImage) {
            Box(
                modifier = Modifier
                    .padding(14.dp).align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(20.dp))
                    .background(C.Green.copy(alpha = 0.2f))
                    .border(1.dp, C.Green.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text("🖼️ Photo", color = C.Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center).size(72.dp).clip(CircleShape)
                .background(C.Card.copy(alpha = 0.6f))
                .border(1.5.dp, C.Border, CircleShape)
                .clickable(remember { MutableInteractionSource() }, null, onClick = onEmojiClick),
            contentAlignment = Alignment.Center,
        ) { Text(emoji, fontSize = 30.sp) }

        Box(
            modifier = Modifier
                .align(Alignment.Center).offset(x = 24.dp, y = 24.dp)
                .size(22.dp).clip(CircleShape)
                .background(C.Blue).border(1.5.dp, C.Bg, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Outlined.Edit, null, tint = C.White, modifier = Modifier.size(11.dp)) }

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))))
                .padding(14.dp),
        ) {
            Text(
                text       = title,
                color      = if (title == "Event Title") C.Gray4 else C.White,
                fontSize   = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines   = 1,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Emoji Picker Row
// ─────────────────────────────────────────────────────────────────────────────
private val eventEmojis = listOf(
    "🎉","🎸","🎷","🎨","💡","🚀","⚡","🏆","🎓","🎭",
    "🍕","🏅","🎤","🎬","🎮","🌟","🔥","💫","🎊","🌈",
)

@Composable
private fun EmojiPickerRow(selected: String, onSelect: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(C.Card).border(1.dp, C.Border, RoundedCornerShape(16.dp))
            .padding(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Cover Emoji", color = C.Gray3, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Row(
                modifier              = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                eventEmojis.forEach { emoji ->
                    Box(
                        modifier = Modifier
                            .size(44.dp).clip(RoundedCornerShape(12.dp))
                            .background(if (emoji == selected) C.BlueSoft else C.Surface)
                            .border(1.5.dp, if (emoji == selected) C.Blue else C.Border, RoundedCornerShape(12.dp))
                            .clickable(remember { MutableInteractionSource() }, null) { onSelect(emoji) },
                        contentAlignment = Alignment.Center,
                    ) { Text(emoji, fontSize = 22.sp) }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Category Grid — preset + custom + "Add" button
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CategoryGrid(
    selected         : CategoryChoice?,
    customCategories : List<CustomCategory>,
    onSelect         : (CategoryChoice) -> Unit,
    onAddCustom      : () -> Unit,
) {
    val presets : List<CategoryChoice> = EventCategory.entries.map { CategoryChoice.Preset(it) }
    val customs : List<CategoryChoice> = customCategories.map { CategoryChoice.Custom(it) }
    val all = presets + customs

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        (all + listOf(null)).chunked(4).forEach { row ->
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { choice ->
                    if (choice == null) {
                        // "Add Custom" button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(C.Card)
                                .border(1.dp, C.Border, RoundedCornerShape(14.dp))
                                .clickable(remember { MutableInteractionSource() }, null, onClick = onAddCustom)
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp).clip(CircleShape)
                                        .background(C.BlueSoft)
                                        .border(1.dp, C.Blue.copy(alpha = 0.5f), CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Default.Add, null, tint = C.Blue, modifier = Modifier.size(16.dp))
                                }
                                Text("Custom", color = C.Blue, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    } else {
                        val isSelected = selected == choice
                        val scale by animateFloatAsState(
                            targetValue   = if (isSelected) 1.04f else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label         = "catScale${choice.label}",
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .graphicsLayer(scaleX = scale, scaleY = scale)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) choice.color.copy(alpha = 0.2f) else C.Card)
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) choice.color else C.Border,
                                    shape = RoundedCornerShape(14.dp),
                                )
                                .clickable(remember { MutableInteractionSource() }, null) { onSelect(choice) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Text(choice.emoji, fontSize = 18.sp)
                                Text(
                                    text       = choice.label,
                                    color      = if (isSelected) choice.color else C.Gray4,
                                    fontSize   = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    textAlign  = TextAlign.Center,
                                    maxLines   = 1,
                                )
                            }
                        }
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Top Bar
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CreateEventTopBar(onBack: () -> Unit, progress: Float) {
    Surface(color = C.Bg, shadowElevation = 0.dp) {
        Column(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(bottom = 12.dp),
        ) {
            Row(
                modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp).clip(CircleShape)
                        .background(C.Card).border(1.dp, C.Border, CircleShape)
                        .clickable(remember { MutableInteractionSource() }, null, onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Default.ArrowBack, "Back", tint = C.Gray2, modifier = Modifier.size(20.dp)) }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text("Create Event", color = C.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Fill in the details below", color = C.Gray4, fontSize = 13.sp)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(C.BlueSoft.copy(alpha = 0.5f))
                        .border(1.dp, C.Blue.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text("${(progress * 100).toInt()}%", color = C.Blue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth().padding(horizontal = 16.dp)
                    .height(3.dp).clip(RoundedCornerShape(2.dp))
                    .background(C.Border),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress).fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(Brush.horizontalGradient(listOf(C.Blue, Color(0xFF60A5FA)))),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Section Header
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SectionHeader(icon: String, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(icon, fontSize = 16.sp)
        Text(title, color = C.Gray2, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Text Field
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventTextField(
    value       : String,
    onValue     : (String) -> Unit,
    label       : String,
    placeholder : String,
    icon        : androidx.compose.ui.graphics.vector.ImageVector,
    modifier    : Modifier = Modifier,
    singleLine  : Boolean  = true,
    maxLines    : Int      = 1,
    maxLength   : Int      = Int.MAX_VALUE,
) {
    OutlinedTextField(
        value         = value,
        onValueChange = { if (it.length <= maxLength) onValue(it) },
        modifier      = modifier.fillMaxWidth(),
        label         = { Text(label, fontSize = 13.sp) },
        placeholder   = { Text(placeholder, color = C.Gray5, fontSize = 13.sp) },
        leadingIcon   = {
            Icon(icon, null, tint = if (value.isNotBlank()) C.Blue else C.Gray4, modifier = Modifier.size(18.dp))
        },
        trailingIcon = if (value.isNotBlank() && singleLine) ({
            Icon(
                imageVector        = Icons.Default.Cancel,
                contentDescription = "Clear",
                tint               = C.Gray4,
                modifier           = Modifier
                    .size(17.dp)
                    .clickable(remember { MutableInteractionSource() }, null) { onValue("") },
            )
        }) else null,
        singleLine  = singleLine,
        maxLines    = maxLines,
        shape       = RoundedCornerShape(14.dp),
        colors      = OutlinedTextFieldDefaults.colors(
            focusedTextColor        = C.White,
            unfocusedTextColor      = C.White,
            focusedBorderColor      = C.Blue,
            unfocusedBorderColor    = C.Border,
            cursorColor             = C.Blue,
            focusedLabelColor       = C.Blue,
            unfocusedLabelColor     = C.Gray4,
            focusedContainerColor   = C.Card,
            unfocusedContainerColor = C.Card,
        ),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  Publish Button
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PublishButton(enabled: Boolean, onClick: () -> Unit) {
    val bgAlpha by animateFloatAsState(
        targetValue   = if (enabled) 1f else 0.4f,
        animationSpec = tween(300),
        label         = "publishAlpha",
    )
    Surface(color = C.Bg, shadowElevation = 0.dp) {
        Box(
            modifier = Modifier
                .fillMaxWidth().navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth().height(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(C.Blue.copy(alpha = bgAlpha), Color(0xFF2563EB).copy(alpha = bgAlpha))
                        )
                    )
                    .then(
                        if (enabled) Modifier.shadow(8.dp, RoundedCornerShape(16.dp), ambientColor = C.Blue.copy(0.4f))
                        else Modifier
                    )
                    .clickable(
                        enabled           = enabled,
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null,
                        onClick           = onClick,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Default.Rocket, null, tint = C.White, modifier = Modifier.size(20.dp))
                    Text(
                        text       = if (enabled) "Publish Event" else "Complete Required Fields",
                        color      = C.White,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}