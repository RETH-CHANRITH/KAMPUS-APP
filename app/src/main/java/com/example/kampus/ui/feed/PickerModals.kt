package com.example.kampus.ui.feed

import android.Manifest
import android.content.Context
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import android.os.Looper
import kotlin.concurrent.thread

// Palette definition
internal data class ComposerPalette(
    val bg: Color,
    val card: Color,
    val border: Color,
    val primary: Color,
    val text: Color,
    val textMuted: Color,
    val placeholder: Color,
)

internal val FbDark = ComposerPalette(
    bg = Color(0xFF080B11),
    card = Color(0xFF0F1520),
    border = Color(0xFF1A2333),
    primary = Color(0xFF3B82F6),
    text = Color(0xFFFFFFFF),
    textMuted = Color(0xFF9CA3AF),
    placeholder = Color(0xFF374151),
)

internal val FbLight = ComposerPalette(
    bg = Color(0xFFFFFFFF),
    card = Color(0xFFF3F4F6),
    border = Color(0xFFE5E7EB),
    primary = Color(0xFF1877F2),
    text = Color(0xFF111827),
    textMuted = Color(0xFF6B7280),
    placeholder = Color(0xFF9CA3AF),
)

// Mock data
private val mockFriends = listOf("John", "Sarah", "Mike", "Emma", "Carlos", "Luna", "Jacob", "Mia")
private val mockLocations = listOf(
    "New York", "San Francisco", "Los Angeles", "Chicago", "Boston", "Seattle", "Austin", "Denver",
    "Miami", "Atlanta", "Portland", "Las Vegas", "Phoenix", "Philadelphia", "San Diego", "Dallas",
    "Houston", "Washington DC", "London", "Paris", "Tokyo", "Sydney", "Toronto", "Dubai",
    "Singapore", "Bangkok", "Amsterdam", "Berlin", "Madrid", "Barcelona", "Rome", "Venice",
    "Bangkok", "Mumbai", "Delhi", "Bangkok", "Istanbul", "Cairo", "Johannesburg", "Mexico City",
    "São Paulo", "Buenos Aires", "Santiago", "Lima", "Bogotá", "Cartagena", "Cancún", "Montego Bay"
)
private val mockEvents = listOf("Birthday", "Anniversary", "Wedding", "Graduation", "New Job", "Vacation", "Festival", "Concert", "Sports Event", "Movie Night", "Dinner", "Party")
private val feelingEmojis = listOf(
    "😊", "😂", "❤️", "😍", "🤔", "😎", "🥳", "😴",
    "😤", "😢", "😱", "🤨", "😌", "🤗", "😳", "🙃"
)
private val mockGifs = listOf(
    "Thinking", "Happy Dance", "Laughing", "Clapping", "Waving", "Thumbs Up",
    "Fire", "Rocket", "Star", "Heart", "Sunglasses", "Party Hat"
)

@Composable
internal fun FeelingEmojiPicker(
    p: ComposerPalette,
    onSelect: (String) -> Unit,
    onClose: () -> Unit,
) {
    var search by remember { mutableStateOf("") }
    val feelingLabels = mapOf(
        "😊" to "happy", "😂" to "laughing", "❤️" to "love", "😍" to "love struck",
        "🤔" to "thinking", "😎" to "cool", "🥳" to "party", "😴" to "tired",
        "😤" to "frustrated", "😢" to "sad", "😱" to "shocked", "🤨" to "skeptical",
        "😌" to "peaceful", "🤗" to "hugging", "😳" to "blushing", "🙃" to "smirk"
    )
    val filtered = feelingEmojis.filter { emoji ->
        feelingLabels[emoji]?.contains(search, ignoreCase = true) == true ||
                search.isEmpty()
    }

    Surface(color = p.bg) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Choose your feeling", color = p.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(p.card)
                        .clickable(remember { MutableInteractionSource() }, null, onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Close, "Close", tint = p.text, modifier = Modifier.size(16.dp))
                }
            }

            HorizontalDivider(color = p.border.copy(alpha = 0.5f), thickness = 0.5.dp)

            // Search box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(p.card)
                    .border(1.dp, p.border, RoundedCornerShape(10.dp))
                    .padding(12.dp),
            ) {
                BasicTextField(
                    value = search,
                    onValueChange = { search = it },
                    textStyle = TextStyle(color = p.text, fontSize = 14.sp),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (search.isBlank()) {
                            Text("Search feelings...", color = p.placeholder, fontSize = 14.sp)
                        }
                        inner()
                    },
                )
            }

            // Emoji grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(filtered) { emoji ->
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(p.card)
                            .border(1.dp, p.border, RoundedCornerShape(12.dp))
                            .clickable(remember { MutableInteractionSource() }, null) {
                                onSelect(emoji)
                                onClose()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(emoji, fontSize = 28.sp)
                    }
                }
            }
        }
    }
}

@Composable
internal fun PeoplePicker(
    p: ComposerPalette,
    selected: List<String>,
    onSelect: (List<String>) -> Unit,
    onClose: () -> Unit,
) {
    var localSelected by remember { mutableStateOf(selected) }
    var search by remember { mutableStateOf("") }
    val filtered = mockFriends.filter { it.contains(search, ignoreCase = true) }

    Surface(color = p.bg) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Tag people", color = p.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(p.card)
                        .clickable(remember { MutableInteractionSource() }, null) {
                            onSelect(localSelected)
                            onClose()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Close, "Close", tint = p.text, modifier = Modifier.size(16.dp))
                }
            }

            HorizontalDivider(color = p.border.copy(alpha = 0.5f), thickness = 0.5.dp)

            // Search box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(p.card)
                    .border(1.dp, p.border, RoundedCornerShape(10.dp))
                    .padding(12.dp),
            ) {
                BasicTextField(
                    value = search,
                    onValueChange = { search = it },
                    textStyle = TextStyle(color = p.text, fontSize = 14.sp),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (search.isBlank()) {
                            Text("Search friends...", color = p.placeholder, fontSize = 14.sp)
                        }
                        inner()
                    },
                )
            }

            // Friends list - scrollable
            if (filtered.isEmpty() && search.isNotEmpty()) {
                Text("No friends found", color = p.textMuted, fontSize = 13.sp, modifier = Modifier.padding(16.dp))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    lazyItems(if (search.isEmpty()) mockFriends else filtered) { friend ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (friend in localSelected) p.primary.copy(alpha = 0.2f) else p.card)
                                .border(1.dp, if (friend in localSelected) p.primary else p.border, RoundedCornerShape(10.dp))
                                .clickable(remember { MutableInteractionSource() }, null) {
                                    localSelected = if (friend in localSelected) {
                                        localSelected - friend
                                    } else {
                                        localSelected + friend
                                    }
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(friend, color = p.text, fontSize = 14.sp)
                            if (friend in localSelected) {
                                Text("✓", color = p.primary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun LocationPicker(
    p: ComposerPalette,
    onSelect: (String) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    var currentLocation by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var hasPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            hasPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (hasPermission) {
                isLoading = true
            } else {
                isLoading = false
                currentLocation = "Permission denied"
            }
        }
    )

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // Set up real-time location updates when permissions granted
    DisposableEffect(hasPermission) {
        if (hasPermission) {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .setMaxUpdateDelayMillis(1500)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(500)
                .build()

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    val location = locationResult.lastLocation
                    if (location != null) {
                        isLoading = false
                        convertLocationToAddress(context, location) { address ->
                            currentLocation = address
                        }
                    }
                }
            }

            // Start requesting real-time location updates
            @Suppress("MissingPermission")
            fusedClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

            // Cleanup when composable is disposed
            onDispose {
                fusedClient.removeLocationUpdates(locationCallback)
            }
        } else {
            onDispose { }
        }
    }

    Surface(color = p.bg) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Add Location", color = p.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(p.card)
                        .clickable(remember { MutableInteractionSource() }, null, onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Close, "Close", tint = p.text, modifier = Modifier.size(16.dp))
                }
            }

            HorizontalDivider(color = p.border.copy(alpha = 0.5f), thickness = 0.5.dp)

            // Live location section
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(p.card)
                        .border(1.dp, p.border, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("📍", fontSize = 24.sp)
                        Text("Getting your location...", color = p.textMuted, fontSize = 13.sp)
                    }
                }
            } else if (currentLocation != null && !currentLocation!!.contains("Permission") && !currentLocation!!.contains("denied")) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(p.primary.copy(alpha = 0.12f))
                        .border(1.5.dp, p.primary, RoundedCornerShape(12.dp))
                        .clickable(remember { MutableInteractionSource() }, null) {
                            onSelect(currentLocation!!)
                            onClose()
                        }
                        .padding(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("📍", fontSize = 24.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("My Location", color = p.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                currentLocation!!,
                                color = p.text,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 2
                            )
                        }
                    }
                }
            } else if (currentLocation != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(p.card)
                        .border(1.dp, p.border, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("⚠️", fontSize = 24.sp)
                        Text(currentLocation!!, color = p.textMuted, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
        }
    }
}

private fun fetchUserLocation(context: Context, onLocationFetched: (String) -> Unit) {
    thread {
        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            var locationFound = false

            // Request fresh, high-accuracy location
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .setMaxUpdateDelayMillis(1500)
                .setWaitForAccurateLocation(true)
                .setMinUpdateIntervalMillis(500)
                .build()

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    val location = locationResult.lastLocation
                    if (location != null && !locationFound) {
                        locationFound = true
                        convertLocationToAddress(context, location, onLocationFetched)
                        fusedClient.removeLocationUpdates(this)
                    }
                }
            }

            // Start requesting fresh location immediately (skip lastLocation cache)
            @Suppress("MissingPermission")
            fusedClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

            // Timeout: stop requesting after 30 seconds for GPS lock
            Thread.sleep(30000)
            if (!locationFound) {
                fusedClient.removeLocationUpdates(locationCallback)
                onLocationFetched("Unable to get location - check GPS and try again")
            }
        } catch (e: Exception) {
            onLocationFetched("Location access denied")
        }
    }
}

private fun convertLocationToAddress(context: Context, location: android.location.Location, onLocationFetched: (String) -> Unit) {
    thread {
        try {
            val geocoder = Geocoder(context)
            val lat = location.latitude.format(4)
            val lng = location.longitude.format(4)

            try {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val locationName = buildString {
                        // Show: Street Address, City, Country
                        if (address.thoroughfare != null) {
                            append(address.thoroughfare)
                            if (address.subThoroughfare != null) {
                                append(" ")
                                append(address.subThoroughfare)
                            }
                        }
                        if (address.locality != null) {
                            if (isNotEmpty()) append(", ")
                            append(address.locality)
                        } else if (address.adminArea != null) {
                            if (isNotEmpty()) append(", ")
                            append(address.adminArea)
                        }
                        // Always add country for clarity
                        if (address.countryName != null) {
                            if (isNotEmpty()) append(", ")
                            append(address.countryName)
                        }
                    }

                    if (locationName.isNotEmpty()) {
                        onLocationFetched(locationName)
                    } else {
                        // Fallback: city or coordinates
                        val fallback = address.locality ?: address.adminArea ?: address.countryName ?:
                        "$lat, $lng"
                        onLocationFetched(fallback)
                    }
                } else {
                    // No address found, show coordinates
                    onLocationFetched("$lat, $lng")
                }
            } catch (e: Exception) {
                // Geocoding error, show coordinates
                onLocationFetched("$lat, $lng")
            }
        } catch (e: Exception) {
            onLocationFetched("${location.latitude.format(4)}, ${location.longitude.format(4)}")
        }
    }
}

private fun Double.format(digits: Int) = "%.${digits}f".format(this)

@Composable
internal fun GifPicker(
    p: ComposerPalette,
    onSelect: (String) -> Unit,
    onClose: () -> Unit,
) {
    var search by remember { mutableStateOf("") }
    val filtered = mockGifs.filter { it.contains(search, ignoreCase = true) }

    Surface(color = p.bg) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Find GIF", color = p.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(p.card)
                        .clickable(remember { MutableInteractionSource() }, null, onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Close, "Close", tint = p.text, modifier = Modifier.size(16.dp))
                }
            }

            HorizontalDivider(color = p.border.copy(alpha = 0.5f), thickness = 0.5.dp)

            // Search box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(p.card)
                    .border(1.dp, p.border, RoundedCornerShape(10.dp))
                    .padding(12.dp),
            ) {
                BasicTextField(
                    value = search,
                    onValueChange = { search = it },
                    textStyle = TextStyle(color = p.text, fontSize = 14.sp),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (search.isBlank()) {
                            Text("Search GIFs...", color = p.placeholder, fontSize = 14.sp)
                        }
                        inner()
                    },
                )
            }

            // GIF grid
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalItemSpacing = 12.dp,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(if (search.isBlank()) mockGifs else filtered) { gif ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(p.card)
                            .border(1.dp, p.border, RoundedCornerShape(10.dp))
                            .clickable(remember { MutableInteractionSource() }, null) {
                                onSelect(gif)
                                onClose()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(gif, color = p.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
internal fun EventPicker(
    p: ComposerPalette,
    onSelect: (String) -> Unit,
    onClose: () -> Unit,
) {
    var search by remember { mutableStateOf("") }
    val filtered = mockEvents.filter { it.contains(search, ignoreCase = true) }

    Surface(color = p.bg) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Event", color = p.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(p.card)
                        .clickable(remember { MutableInteractionSource() }, null, onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Close, "Close", tint = p.text, modifier = Modifier.size(16.dp))
                }
            }

            HorizontalDivider(color = p.border.copy(alpha = 0.5f), thickness = 0.5.dp)

            // Search box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(p.card)
                    .border(1.dp, p.border, RoundedCornerShape(10.dp))
                    .padding(12.dp),
            ) {
                BasicTextField(
                    value = search,
                    onValueChange = { search = it },
                    textStyle = TextStyle(color = p.text, fontSize = 14.sp),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (search.isBlank()) {
                            Text("Search event...", color = p.placeholder, fontSize = 14.sp)
                        }
                        inner()
                    },
                )
            }

            // Results - scrollable list
            if (filtered.isEmpty() && search.isNotEmpty()) {
                Text("No events found", color = p.textMuted, fontSize = 13.sp, modifier = Modifier.padding(16.dp))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    lazyItems(if (search.isEmpty()) mockEvents else filtered) { event ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(p.card)
                                .border(1.dp, p.border, RoundedCornerShape(10.dp))
                                .clickable(remember { MutableInteractionSource() }, null) {
                                    onSelect(event)
                                    onClose()
                                }
                                .padding(12.dp),
                        ) {
                            Text(event, color = p.text, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}
