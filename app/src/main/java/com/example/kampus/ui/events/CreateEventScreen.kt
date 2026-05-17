@file:Suppress("SpellCheckingInspection")
package com.example.kampus.ui.events

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Tag
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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
    val coverImageUri: Uri? = null,
    val coverImageUrl: String = "",
    val category: EventCategory = EventCategory.CAMPUS,
    val eventType: String = "",
    val audienceMax: String = "",
    val registrationDeadline: String = "",
    val onlineEvent: Boolean = false,
    val certificateAvailable: Boolean = false,
    val paidEvent: Boolean = false,
    val allowGuest: Boolean = false,
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

// ─────────────────────────────────────────────────────────────────────────────
//  Cambodia Locations
// ─────────────────────────────────────────────────────────────────────────────

private val CAMBODIA_LOCATIONS = listOf(
    "Phnom Penh",
    "Siem Reap",
    "Battambang",
    "Sihanoukville",
    "Kampong Thom",
    "Kratié",
    "Mondulkiri",
    "Ratanakiri",
    "Takéo",
    "Kampot",
    "Kep",
    "Kampong Cham",
    "Pursat",
    "Oddar Meanchey",
    "Preah Vihear",
    "Stung Treng",
    "Online"
)

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
    onPublish: suspend (NewEventData) -> Result<String>,
) {
    CreateEventModernScreen(onBack = onBack, onPublish = onPublish)
}

@Composable
private fun CreateEventModernScreen(
    onBack: () -> Unit,
    onPublish: suspend (NewEventData) -> Result<String>,
) {
    val p = CreateEventPalette()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Campus") }
    var eventType by remember { mutableStateOf("Conference") }
    var audience by remember { mutableStateOf("150") }
    var registrationDeadline by remember { mutableStateOf("") }

    var organiser by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    var coverAttachment by remember { mutableStateOf<MediaAttachment?>(null) }
    var attachments by remember { mutableStateOf<List<MediaAttachment>>(emptyList()) }
    var tags by remember { mutableStateOf(listOf("campus")) }
    var tagInput by remember { mutableStateOf("") }
    var speaker by remember { mutableStateOf("") }
    var showTagInput by remember { mutableStateOf(false) }

    var onlineEvent by remember { mutableStateOf(false) }
    var certificateAvailable by remember { mutableStateOf(false) }
    var paidEvent by remember { mutableStateOf(false) }
    var allowGuest by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<String?>(null) }
    var submitSuccess by remember { mutableStateOf<String?>(null) }

    val coverPicker = rememberMediaPickers(
        onResult = { uri, type ->
            uri?.let {
                val name = it.getFileName(context)
                val media = MediaAttachment(it, type, name)
                coverAttachment = media
                if (attachments.isEmpty()) attachments = attachments + media
            }
        }
    )

    val filePicker = rememberMediaPickers(
        onResult = { uri, type ->
            uri?.let {
                val name = it.getFileName(context)
                attachments = attachments + MediaAttachment(it, type, name)
            }
        }
    )

    val dateValue = remember { Calendar.getInstance() }

    fun submit() {
        if (isSubmitting) return

        scope.launch {
            isSubmitting = true
            submitError = null
            submitSuccess = null

            val result = onPublish(
                NewEventData(
                    title = title,
                    description = description,
                    date = date,
                    time = time,
                    location = location,
                    organiser = organiser,
                    website = website,
                    coverEmoji = coverAttachment?.type?.emoji ?: "🎉",
                    coverImageUri = coverAttachment?.uri,
                    coverImageUrl = "",
                    category = when (category) {
                        "Campus" -> EventCategory.CAMPUS
                        "Tech" -> EventCategory.TECH
                        "Music" -> EventCategory.MUSIC
                        "Art" -> EventCategory.ART
                        else -> EventCategory.CAMPUS
                    },
                    eventType = eventType,
                    audienceMax = audience,
                    registrationDeadline = registrationDeadline,
                    onlineEvent = onlineEvent,
                    certificateAvailable = certificateAvailable,
                    paidEvent = paidEvent,
                    allowGuest = allowGuest,
                    tags = tags,
                    attachments = attachments,
                    textInput = description,
                    visibility = EventVisibility.PUBLIC,
                    limitCapacity = false,
                    capacity = audience.toIntOrNull() ?: 0,
                    rsvpRequired = true,
                    allowComments = true,
                    shareToFeed = true,
                )
            )

            isSubmitting = false
            result.onSuccess {
                submitSuccess = "Event created successfully"
                onBack()
            }.onFailure { error ->
                submitError = error.message ?: "Failed to create event"
            }
        }
    }

    Scaffold(
        containerColor = p.bg,
        topBar = {
            Surface(color = Color.Transparent) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(p.card)
                            .border(1.dp, p.border, RoundedCornerShape(14.dp))
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = p.primary)
                    }
                    Text(
                        "Create Event",
                        modifier = Modifier.weight(1f),
                        color = p.text,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Save Draft",
                        color = p.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { }
                    )
                }
            }
        },
        bottomBar = {
            Surface(color = Color.Transparent) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Button(
                        onClick = { submit() },
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(p.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Icon(Icons.Default.Publish, null, tint = Color.White)
                                    Text("Publish Event", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {},
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                HeroCoverSection(
                    coverAttachment = coverAttachment,
                    onUploadCover = { coverPicker.photo.launch("image/*") }
                )
            }

            item {
                Text("Event Information", color = p.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            item {
                ModernCard(p) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ModernInputField(
                            p,
                            value = title,
                            onValueChange = { title = it.take(80) },
                            label = "Event Title",
                            placeholder = "Enter event title",
                            icon = Icons.Outlined.Title,
                            counter = "${title.length}/80"
                        )
                        ModernInputField(
                            p,
                            value = description,
                            onValueChange = { description = it.take(500) },
                            label = "Description",
                            placeholder = "Tell people what your event is about...",
                            icon = Icons.Outlined.Edit,
                            maxLines = 4,
                            counter = "${description.length}/500"
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            PickerCardField(
                                p,
                                modifier = Modifier.weight(1f),
                                value = date,
                                label = "Date",
                                icon = Icons.Outlined.DateRange,
                                placeholder = "Select date",
                                onClick = {
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            dateValue.set(year, month, dayOfMonth)
                                            val formatted = SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH).format(dateValue.time)
                                            date = formatted
                                        },
                                        dateValue.get(Calendar.YEAR),
                                        dateValue.get(Calendar.MONTH),
                                        dateValue.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                            )
                            PickerCardField(
                                p,
                                modifier = Modifier.weight(1f),
                                value = time,
                                label = "Time",
                                icon = Icons.Outlined.Schedule,
                                placeholder = "Select time",
                                onClick = {
                                    TimePickerDialog(
                                        context,
                                        { _, hourOfDay, minute ->
                                            time = String.format(
                                                Locale.ENGLISH,
                                                "%d:%02d %s",
                                                if (hourOfDay % 12 == 0) 12 else hourOfDay % 12,
                                                minute,
                                                if (hourOfDay < 12) "AM" else "PM"
                                            )
                                        },
                                        dateValue.get(Calendar.HOUR_OF_DAY),
                                        dateValue.get(Calendar.MINUTE),
                                        false
                                    ).show()
                                }
                            )
                        }
                        LocationDropdown(selectedLocation = location, onLocationChange = { location = it })

                        if (!submitError.isNullOrBlank()) {
                            Text(
                                submitError!!,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        if (!submitSuccess.isNullOrBlank()) {
                            Text(
                                submitSuccess!!,
                                color = p.primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            DropdownCardField(
                                p,
                                modifier = Modifier.weight(1f),
                                value = category,
                                label = "Category",
                                icon = Icons.Outlined.Title,
                                options = listOf("Campus", "Tech", "Music", "Art"),
                                onValueChange = { category = it }
                            )
                            DropdownCardField(
                                p,
                                modifier = Modifier.weight(1f),
                                value = eventType,
                                label = "Event Type",
                                icon = Icons.Outlined.Public,
                                options = listOf("Conference", "Workshop", "Meetup", "Seminar", "Festival"),
                                onValueChange = { eventType = it }
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            DropdownCardField(
                                p,
                                modifier = Modifier.weight(1f),
                                value = audience,
                                label = "Audience / Max Students",
                                icon = Icons.Outlined.Person,
                                options = listOf("50", "100", "150", "250", "500", "1000"),
                                placeholder = "e.g. 150",
                                onValueChange = { audience = it }
                            )
                            PickerCardField(
                                p,
                                modifier = Modifier.weight(1f),
                                value = registrationDeadline,
                                label = "Registration Deadline",
                                icon = Icons.Outlined.Schedule,
                                placeholder = "Select deadline",
                                onClick = {
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            val cal = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
                                            registrationDeadline = SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH).format(cal.time)
                                        },
                                        dateValue.get(Calendar.YEAR),
                                        dateValue.get(Calendar.MONTH),
                                        dateValue.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                            )
                        }
                    }
                }
            }

            item {
                Text("Event Options", color = p.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            item {
                ModernCard(p) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        ToggleRow(p, "Online Event", "This is an online / virtual event", onlineEvent) { onlineEvent = it }
                        ToggleRow(p, "Certificate Available", "Participants will get a certificate", certificateAvailable) { certificateAvailable = it }
                        ToggleRow(p, "Paid Event", "This event has a ticket or fee", paidEvent) { paidEvent = it }
                        ToggleRow(p, "Allow Guest", "Allow students to bring guests", allowGuest) { allowGuest = it }
                    }
                }
            }

            item {
                Text("Add More Details (Optional)", color = p.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    ActionCard(p, "Add Speaker", Icons.Outlined.MicNone, Modifier.weight(1f)) { speaker = if (speaker.isBlank()) "Speaker" else speaker }
                    ActionCard(p, "Attach File", Icons.Outlined.AttachFile, Modifier.weight(1f)) { filePicker.doc.launch("*/*") }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    ActionCard(p, "Add Tags", Icons.Outlined.Tag, Modifier.weight(1f)) { showTagInput = !showTagInput }
                    ActionCard(p, "Registration Link", Icons.Outlined.Link, Modifier.weight(1f)) { website = if (website.isBlank()) "https://" else website }
                }
            }

            if (showTagInput) {
                item {
                    ModernCard(p) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = tagInput,
                                onValueChange = { if (it.length <= 24) tagInput = it },
                                modifier = Modifier.weight(1f),
                                label = { Text("Add Tag", color = p.text3) },
                                placeholder = { Text("#music, #tech", color = p.text3) },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                textStyle = androidx.compose.material3.LocalTextStyle.current.copy(color = p.text),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = p.primary,
                                    unfocusedBorderColor = p.border,
                                    focusedContainerColor = p.surface,
                                    unfocusedContainerColor = p.surface,
                                    focusedLabelColor = p.primary,
                                    unfocusedLabelColor = p.text3,
                                    focusedTextColor = p.text,
                                    unfocusedTextColor = p.text,
                                    cursorColor = p.primary,
                                )
                            )
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(p.primary)
                                    .clickable {
                                        if (tagInput.isNotBlank()) {
                                            tags = tags + tagInput.trim()
                                            tagInput = ""
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, null, tint = Color.White)
                            }
                        }
                    }
                }
            }

            if (tags.isNotEmpty()) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        tags.forEach { tag ->
                            ChipWithRemove(label = "#$tag", onRemove = { tags = tags - tag })
                        }
                    }
                }
            }

            if (attachments.isNotEmpty()) {
                item {
                    ModernCard(p) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Attached Files", color = p.text, fontWeight = FontWeight.Bold)
                            attachments.forEach { att ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(p.surface)
                                        .border(1.dp, p.border, RoundedCornerShape(14.dp))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(att.type.emoji)
                                    Text(att.displayName, modifier = Modifier.weight(1f), color = p.text, fontSize = 13.sp)
                                    Icon(Icons.Default.Close, null, tint = p.primary, modifier = Modifier.clickable { attachments = attachments - att })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroCoverSection(
    coverAttachment: MediaAttachment?,
    onUploadCover: () -> Unit,
) {
    val p = CreateEventPalette()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(p.card)
            .border(1.dp, p.border, RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (coverAttachment != null) {
                AsyncImage(
                    model = coverAttachment.uri,
                    contentDescription = coverAttachment.displayName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, p.border, RoundedCornerShape(18.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(p.primarySoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Public, null, tint = p.primary, modifier = Modifier.size(34.dp))
                }
            }

            Text("Add Event Cover", color = p.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Upload a photo or poster for your event", color = p.text3, fontSize = 13.sp, textAlign = TextAlign.Center)
            OutlinedButton(
                onClick = onUploadCover,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, p.primary),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = p.primary)
            ) {
                Icon(Icons.Default.FileUpload, null)
                Spacer(Modifier.width(8.dp))
                Text("Upload Cover")
            }
        }
    }
}

@Composable
private fun ModernCard(palette: ScreenPalette, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(palette.card)
            .border(1.dp, palette.border, RoundedCornerShape(22.dp))
            .padding(14.dp),
        content = content
    )
}

@Composable
private fun ModernInputField(
    palette: ScreenPalette,
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: ImageVector,
    counter: String? = null,
    maxLines: Int = 1,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = palette.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            if (counter != null) Text(counter, color = palette.text3, fontSize = 11.sp)
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder) },
            leadingIcon = { Icon(icon, null, tint = palette.primary) },
            shape = RoundedCornerShape(18.dp),
            maxLines = maxLines,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = palette.primary,
                unfocusedBorderColor = palette.border,
                focusedContainerColor = palette.card,
                unfocusedContainerColor = palette.card,
            )
        )
    }
}

@Composable
private fun PickerCardField(
    palette: ScreenPalette,
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    placeholder: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Column(modifier = modifier) {
        Text(label, color = palette.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(palette.card)
                .border(1.dp, palette.border, RoundedCornerShape(18.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icon, null, tint = palette.primary)
                Text(if (value.isBlank()) placeholder else value, color = if (value.isBlank()) palette.text3 else palette.text, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun DropdownCardField(
    palette: ScreenPalette,
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    icon: ImageVector,
    options: List<String>,
    placeholder: String = "Select",
    onValueChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        Text(label, color = palette.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Box {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(palette.card)
                    .border(1.dp, palette.border, RoundedCornerShape(18.dp))
                    .clickable { expanded = true }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(icon, null, tint = palette.primary)
                    Text(if (value.isBlank()) placeholder else value, color = if (value.isBlank()) palette.text3 else palette.text, fontSize = 13.sp)
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach {
                    DropdownMenuItem(
                        text = { Text(it) },
                        onClick = {
                            onValueChange(it)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(palette: ScreenPalette, title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = palette.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = palette.text3, fontSize = 11.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = palette.primary))
    }
}

@Composable
private fun ActionCard(palette: ScreenPalette, text: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .height(92.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(palette.card)
            .border(1.dp, palette.border, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = palette.primary)
        Spacer(Modifier.height(10.dp))
        Text(text, color = palette.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
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
            DatePickerField(
                value = date,
                onValue = onDateChange,
            )
        }
        item {
            TimePickerField(
                value = time,
                onValue = onTimeChange,
            )
        }
        item {
            LocationDropdown(
                selectedLocation = location,
                onLocationChange = onLocationChange
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
private fun DatePickerField(
    value: String,
    onValue: (String) -> Unit,
) {
    val p = CreateEventPalette()
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }
    val formatter = remember { SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Date *", fontSize = 13.sp) },
            placeholder = { Text("Pick event date", color = p.text3, fontSize = 13.sp) },
            leadingIcon = {
                Icon(
                    Icons.Outlined.DateRange,
                    null,
                    tint = if (value.isNotBlank()) p.primary else p.text3,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "Pick date",
                    tint = p.text3,
                    modifier = Modifier.size(18.dp)
                )
            },
            singleLine = true,
            readOnly = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = p.primary,
                unfocusedBorderColor = p.border,
                focusedLabelColor = p.primary,
                unfocusedLabelColor = p.text3,
                focusedContainerColor = p.card,
                unfocusedContainerColor = p.card,
            ),
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            calendar.set(year, month, dayOfMonth)
                            onValue(formatter.format(calendar.time))
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }
        )
    }
}

@Composable
private fun TimePickerField(
    value: String,
    onValue: (String) -> Unit,
) {
    val p = CreateEventPalette()
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Time *", fontSize = 13.sp) },
            placeholder = { Text("Pick event time", color = p.text3, fontSize = 13.sp) },
            leadingIcon = {
                Icon(
                    Icons.Outlined.Schedule,
                    null,
                    tint = if (value.isNotBlank()) p.primary else p.text3,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "Pick time",
                    tint = p.text3,
                    modifier = Modifier.size(18.dp)
                )
            },
            singleLine = true,
            readOnly = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = p.primary,
                unfocusedBorderColor = p.border,
                focusedLabelColor = p.primary,
                unfocusedLabelColor = p.text3,
                focusedContainerColor = p.card,
                unfocusedContainerColor = p.card,
            ),
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    TimePickerDialog(
                        context,
                        { _, hourOfDay, minute ->
                            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                            calendar.set(Calendar.MINUTE, minute)
                            val formatted = String.format(
                                Locale.ENGLISH,
                                "%d:%02d %s",
                                if (hourOfDay % 12 == 0) 12 else hourOfDay % 12,
                                minute,
                                if (hourOfDay < 12) "AM" else "PM"
                            )
                            onValue(formatted)
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        false
                    ).show()
                }
        )
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
private fun LocationDropdown(
    selectedLocation: String,
    onLocationChange: (String) -> Unit,
) {
    val p = CreateEventPalette()
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedLocation,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Location / Room *", fontSize = 13.sp) },
            placeholder = { Text("Select a location in Cambodia", color = p.text3, fontSize = 13.sp) },
            leadingIcon = {
                Icon(Icons.Outlined.LocationOn, null, tint = if (selectedLocation.isNotBlank()) p.primary else p.text3, modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Select location",
                    tint = p.text3,
                    modifier = Modifier.size(18.dp)
                )
            },
            singleLine = true,
            readOnly = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = p.primary,
                unfocusedBorderColor = p.border,
                focusedLabelColor = p.primary,
                unfocusedLabelColor = p.text3,
                focusedContainerColor = p.card,
                unfocusedContainerColor = p.card,
            ),
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { expanded = true }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .background(p.card)
        ) {
            CAMBODIA_LOCATIONS.forEach { city ->
                DropdownMenuItem(
                    text = {
                        Text(
                            city,
                            color = if (selectedLocation == city) p.primary else p.text,
                            fontWeight = if (selectedLocation == city) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onLocationChange(city)
                        expanded = false
                    }
                )
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