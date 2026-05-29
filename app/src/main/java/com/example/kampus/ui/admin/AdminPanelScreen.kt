package com.example.kampus.ui.admin

import android.content.Intent
import android.text.format.DateUtils
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.window.*
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import android.util.Log
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import com.example.kampus.ui.components.CampusBottomNavBar
import com.example.kampus.ui.components.NavItem
import com.example.kampus.ui.theme.ThemeController
import com.example.kampus.ui.localization.rememberUiStrings
import com.example.kampus.ui.localization.UiStrings
import com.example.kampus.ui.profile.ProfileViewModel
import com.example.kampus.ui.profile.EditProfileScreen
import com.example.kampus.ui.notifications.NotificationScreen
import com.example.kampus.ui.profile.*
import com.example.kampus.utils.LanguageManager
import com.example.kampus.ui.theme.AppSettingsStore

// ─────────────────────────────────────────────────────────────────────────────
// Design Tokens — adaptive to Light/Dark mode
// ─────────────────────────────────────────────────────────────────────────────
object KampusAdminTheme {
    val IsDark get() = ThemeController.isDark
    
    val NavyDeep    get() = if (IsDark) Color(0xFF0D1B2A) else Color(0xFFF3F4F8)
    val NavyMid     get() = if (IsDark) Color(0xFF1A2D44) else Color(0xFFFFFFFF)
    val NavySurface get() = if (IsDark) Color(0xFF1E3452) else Color(0xFFE5E7EB)
    val NavyCard    get() = if (IsDark) Color(0xFF243B55) else Color(0xFFFFFFFF)
    
    val TealPrimary = Color(0xFF00C9A7)
    val TealDim     = Color(0xFF007A65)
    
    val AccentOrange= Color(0xFFFF6B35)
    val AccentPurple= Color(0xFF8B5CF6)
    val AccentBlue  = Color(0xFF3B9EFF)
    
    val TextPrimary get() = if (IsDark) Color(0xFFF0F4F8) else Color(0xFF111827)
    val TextSecond  get() = if (IsDark) Color(0xFF8FA7C0) else Color(0xFF4B5563)
    val TextMuted   get() = if (IsDark) Color(0xFF4A6380) else Color(0xFF9CA3AF)
    
    val DangerRed   = Color(0xFFEF4444)
    val SuccessGreen= Color(0xFF22C55E)
    val WarnYellow  = Color(0xFFFBBF24)
    
    val Border      get() = if (IsDark) Color(0xFF2C3552) else Color(0xFFD1D5DB)

    // Helper for high-contrast text on accent backgrounds
    val OnTeal      = Color(0xFF0D1B2A)
}

enum class AdminTab { DASHBOARD, USERS, REPORTS, CONTENT, NOTIFICATIONS, PROFILE }

// ─────────────────────────────────────────────────────────────────────────────
// Admin Panel Root
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    onLogout: () -> Unit = {},
    onViewUserProfile: (String) -> Unit = {},
    onChatClick: () -> Unit = {},
    onChatWithUser: (String) -> Unit = {},
    viewModel: AdminViewModel = viewModel(),
    profileViewModel: ProfileViewModel = viewModel()
) {
    val strings = rememberUiStrings()
    var selectedTab by rememberSaveable { mutableStateOf(AdminTab.DASHBOARD) }
    var isEditingProfile by rememberSaveable { mutableStateOf(false) }
    var showNotifications by remember { mutableStateOf(false) }
    var showImagePicker by remember { mutableStateOf(false) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Image Picker for Admin Profile
    if (showImagePicker) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showImagePicker = false },
            sheetState = sheetState,
            containerColor = KampusAdminTheme.NavyCard,
            scrimColor = Color.Black.copy(alpha = 0.5f),
        ) {
            // Reusing ImagePickerDialog design logic but adapted for Admin Panel
            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Update cover image", color = KampusAdminTheme.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    
                    // Simple choice buttons (logic from original ImagePickerDialog)
                    // For brevity, assuming we have a picker logic or user can pick from gallery.
                    // This is a placeholder for the actual picker integration.
                    Button(
                        onClick = { showImagePicker = false }, // In real app, launch gallery
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = KampusAdminTheme.TealPrimary)
                    ) {
                        Text("Choose from Gallery", color = Color.Black)
                    }
                    TextButton(onClick = { showImagePicker = false }) {
                        Text("Cancel", color = KampusAdminTheme.TextMuted)
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KampusAdminTheme.NavyDeep)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (selectedTab != AdminTab.PROFILE) {
                AdminTopBar(
                    onLogout = onLogout, 
                    strings = strings,
                    onNotificationClick = { showNotifications = true }
                )
            }
            
            Box(modifier = Modifier.weight(1f)) {
                if (isEditingProfile && selectedTab == AdminTab.PROFILE) {
                    EditProfileScreen(
                        onBack = { isEditingProfile = false },
                        onSaved = { isEditingProfile = false },
                        viewModel = profileViewModel
                    )
                } else {
                    when (selectedTab) {
                        AdminTab.DASHBOARD     -> AdminDashboardTab(
                            state = state,
                            strings = strings,
                            onOpenProfile = { selectedTab = AdminTab.PROFILE },
                            onOpenNotifications = { selectedTab = AdminTab.NOTIFICATIONS },
                        )
                        AdminTab.USERS         -> AdminUsersTab(state, viewModel, strings, onViewUserProfile, onChatWithUser)
                        AdminTab.REPORTS       -> AdminReportsTab(state, viewModel, strings)
                        AdminTab.CONTENT       -> AdminContentTab(state, viewModel, strings)
                        AdminTab.NOTIFICATIONS -> AdminNotificationsTab(state, viewModel, strings)
                        AdminTab.PROFILE       -> AdminProfileTab(
                            onLogout = onLogout,
                            profileViewModel = profileViewModel,
                            adminUiState = state,
                            strings = strings,
                            onEditProfile = { isEditingProfile = true },
                            onEditCoverImage = { showImagePicker = true },
                            onChatClick = onChatClick
                        )
                    }
                }
            }
            
            // Dynamic Bottom Nav using the shared Campus design - NO BACKGROUND
            AdminBottomNavDynamic(selectedTab, strings) { 
                selectedTab = it 
                isEditingProfile = false
            }
        }
        
        // Show non-blocking error if firestore fails (e.g. Permission Denied)
        if (state.error != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 100.dp)
                    .padding(horizontal = 16.dp)
                    .background(KampusAdminTheme.DangerRed.copy(alpha = 0.9f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "Sync Error: ${state.error}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        if (showNotifications) {
            Box(modifier = Modifier.fillMaxSize().background(KampusAdminTheme.NavyDeep)) {
                NotificationScreen(
                    onBack = { showNotifications = false },
                    onNavigate = { /* Handle internal navigation if needed */ }
                )
            }
        }
    }
}

@Composable
fun AdminBottomNavDynamic(selected: AdminTab, strings: UiStrings, onSelect: (AdminTab) -> Unit) {
    val adminNavItems = listOf(
        NavItem(strings.adminDashboard, Icons.Outlined.Dashboard, Icons.Filled.Dashboard),
        NavItem(strings.adminUsers,     Icons.Outlined.People,    Icons.Filled.People),
        NavItem(strings.adminReports,   Icons.Outlined.Flag,      Icons.Filled.Flag),
        NavItem(strings.adminContent,   Icons.AutoMirrored.Outlined.Article,   Icons.AutoMirrored.Filled.Article),
    )

    val selectedIndex = when(selected) {
        AdminTab.DASHBOARD -> 0
        AdminTab.USERS -> 1
        AdminTab.REPORTS -> 2
        AdminTab.CONTENT -> 3
        else -> -1 
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .navigationBarsPadding(),
    ) {
        CampusBottomNavBar(
            navItems = adminNavItems,
            selectedIndex = selectedIndex,
            onItemSelected = { index ->
                when(index) {
                    0 -> onSelect(AdminTab.DASHBOARD)
                    1 -> onSelect(AdminTab.USERS)
                    2 -> onSelect(AdminTab.REPORTS)
                    3 -> onSelect(AdminTab.CONTENT)
                }
            },
            onFabClick = { onSelect(AdminTab.NOTIFICATIONS) },
            fabIcon = Icons.Default.Campaign,
            onProfileClick = { onSelect(AdminTab.PROFILE) },
            isProfileSelected = selected == AdminTab.PROFILE,
            backgroundColor = KampusAdminTheme.NavyMid
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Top Bar
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AdminTopBar(onLogout: () -> Unit = {}, strings: UiStrings, onNotificationClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(KampusAdminTheme.NavyDeep)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Shield badge
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        brush = Brush.linearGradient(
                            listOf(KampusAdminTheme.TealPrimary, KampusAdminTheme.TealDim)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = null,
                    tint = if (KampusAdminTheme.IsDark) KampusAdminTheme.NavyDeep else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    strings.adminPanel,
                    color = KampusAdminTheme.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp
                )
                Text(
                    strings.superAdmin,
                    color = KampusAdminTheme.TealPrimary,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
            }
        }
        // Logout / Notifications
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onLogout) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Logout",
                    tint = KampusAdminTheme.TextSecond
                )
            }
            Spacer(Modifier.width(4.dp))
            // Notification bell
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(KampusAdminTheme.NavyMid)
                    .clickable { onNotificationClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Notifications, null, tint = KampusAdminTheme.TealPrimary, modifier = Modifier.size(20.dp))
                // Dot
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp, end = 8.dp)
                        .align(Alignment.TopEnd)
                        .size(6.dp)
                        .background(KampusAdminTheme.AccentOrange, CircleShape)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DASHBOARD TAB
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AdminDashboardTab(
    state: AdminUiState,
    strings: UiStrings,
    onOpenProfile: () -> Unit,
    onOpenNotifications: () -> Unit,
) {
    val stats = listOf(
        AppStat(strings.totalUsers,    state.totalUsers.toString(), Icons.Filled.Person,      KampusAdminTheme.TealPrimary,  "+${state.usersToday} today"),
        AppStat(strings.activeGroups,  state.activeGroups.toString(),    Icons.Filled.Group,       KampusAdminTheme.AccentBlue,   "+${state.groupsThisWeek} this week"),
        AppStat(strings.postsToday,    state.postsToday.toString(),   Icons.AutoMirrored.Filled.Article,     KampusAdminTheme.AccentPurple, "${state.postsToday} today"),
        AppStat(strings.openReports,   state.openReports.toString(),     Icons.Filled.Flag,        KampusAdminTheme.AccentOrange, if(state.openReports > 0) "Review Actions" else "Dismiss"),
        AppStat(strings.totalEvents,         state.totalEvents.toString(),    Icons.Filled.Event,       KampusAdminTheme.SuccessGreen, "${state.upcomingEvents} upcoming"),
        AppStat(strings.bannedUsers,   state.bannedUsers.toString(),     Icons.Filled.Block,       KampusAdminTheme.DangerRed,    strings.all),
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            // Welcome Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(KampusAdminTheme.TealPrimary.copy(alpha = 0.2f), Color.Transparent)
                        )
                    )
                    .border(1.dp, KampusAdminTheme.TealPrimary.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Column {
                    Text(strings.welcomeBackAdmin, color = KampusAdminTheme.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(strings.happeningToday, color = KampusAdminTheme.TextSecond, fontSize = 13.sp)
                }
            }
        }

        item {
            Column {
                Text(strings.appStatistics, color = KampusAdminTheme.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
                Spacer(Modifier.height(16.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    maxItemsInEachRow = 2
                ) {
                    stats.forEach { stat -> StatCard(stat, Modifier.weight(1f)) }
                }
            }
        }

        item { QuickActionsSection(strings, onOpenProfile, onOpenNotifications) }
        item { RecentActivitySection(state.recentActivities, strings) }
        
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun StatCard(stat: AppStat, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(KampusAdminTheme.NavyCard)
            .border(1.dp, KampusAdminTheme.NavySurface, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(stat.color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(stat.icon, null, tint = stat.color, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(stat.value, color = KampusAdminTheme.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(stat.label, color = KampusAdminTheme.TextSecond, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Text(stat.trend, color = stat.color, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun QuickActionsSection(
    strings: UiStrings,
    onOpenProfile: () -> Unit,
    onOpenNotifications: () -> Unit,
) {
    Column {
        Text(strings.quickActions, color = KampusAdminTheme.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AdminActionButton(strings.viewProfile, Icons.Filled.Visibility, KampusAdminTheme.AccentBlue, Modifier.weight(1f), onClick = onOpenProfile)
            AdminActionButton(strings.massNotifications, Icons.Filled.Campaign, KampusAdminTheme.AccentOrange, Modifier.weight(1f), onClick = onOpenNotifications)
        }
    }
}

@Composable
fun RecentActivitySection(activities: List<Pair<String, Long>>, strings: UiStrings) {
    Column {
        Text(strings.recentActivity, color = KampusAdminTheme.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(KampusAdminTheme.NavyCard, RoundedCornerShape(20.dp))
                .border(1.dp, KampusAdminTheme.NavySurface, RoundedCornerShape(20.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (activities.isEmpty()) {
                Text(
                    text = "No recent activity recorded",
                    color = KampusAdminTheme.TextMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(8.dp)
                )
            } else {
                activities.forEach { (text, time) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).background(KampusAdminTheme.TealPrimary, CircleShape))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(text, color = KampusAdminTheme.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(
                                DateUtils.getRelativeTimeSpanString(time, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString(),
                                color = KampusAdminTheme.TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// USERS TAB
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AdminUsersTab(state: AdminUiState, viewModel: AdminViewModel, strings: UiStrings, onViewUserProfile: (String) -> Unit, onChatWithUser: (String) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = state.userList.filter { it.name.contains(searchQuery, ignoreCase = true) || it.major.contains(searchQuery, ignoreCase = true) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))
        // Search bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(KampusAdminTheme.NavyCard, RoundedCornerShape(14.dp))
                .border(1.dp, KampusAdminTheme.NavySurface, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Search, null, tint = KampusAdminTheme.TextMuted, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = KampusAdminTheme.TextPrimary, fontSize = 14.sp
                ),
                decorationBox = { inner ->
                    if (searchQuery.isEmpty()) Text(strings.searchUsersPlaceholder, color = KampusAdminTheme.TextMuted, fontSize = 14.sp)
                    inner()
                },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${filtered.size} ${strings.adminUsers}", color = KampusAdminTheme.TextSecond, fontSize = 12.sp, letterSpacing = 1.sp)
            Spacer(Modifier.weight(1f))
            Text("${state.userList.count { it.isBanned }} Banned", color = KampusAdminTheme.DangerRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
            items(filtered) { user -> UserListCard(user, viewModel, strings, onViewUserProfile, onChatWithUser) }
        }
    }
}

@Composable
fun UserListCard(user: AdminUser, viewModel: AdminViewModel, strings: UiStrings, onViewUserProfile: (String) -> Unit, onChatWithUser: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var showBanDialog by remember { mutableStateOf(false) }

    if (showBanDialog) {
        BanUserDialog(
            userName = user.name,
            onConfirm = {
                viewModel.banUser(user.uid, true)
                showBanDialog = false
            },
            onDismiss = { showBanDialog = false },
            strings = strings
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(KampusAdminTheme.NavyCard, RoundedCornerShape(14.dp))
            .border(
                1.dp,
                if (user.isBanned) KampusAdminTheme.DangerRed.copy(alpha = 0.3f) else KampusAdminTheme.NavySurface,
                RoundedCornerShape(14.dp)
            )
            .clickable { expanded = !expanded }
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (user.isBanned) KampusAdminTheme.DangerRed.copy(alpha = 0.2f)
                        else KampusAdminTheme.TealPrimary.copy(alpha = 0.2f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(user.avatarInitial, color = if (user.isBanned) KampusAdminTheme.DangerRed else KampusAdminTheme.TealPrimary,
                    fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(user.name, color = KampusAdminTheme.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(6.dp))
                    if (user.role == "admin") {
                        Box(modifier = Modifier
                            .background(KampusAdminTheme.TealPrimary.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) { Text("ADMIN", color = KampusAdminTheme.TealPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp) }
                    }
                }
                Text(user.major, color = KampusAdminTheme.TextSecond, fontSize = 12.sp)
            }
            if (user.isBanned) {
                Box(modifier = Modifier
                    .background(KampusAdminTheme.DangerRed.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .clickable { viewModel.banUser(user.uid, false) }
                ) { Text("UNBAN", color = KampusAdminTheme.DangerRed, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            } else {
                Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    null, tint = KampusAdminTheme.TextMuted, modifier = Modifier.size(20.dp))
            }
        }
        AnimatedVisibility(visible = expanded && !user.isBanned) {
            Column {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = KampusAdminTheme.NavySurface)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdminActionButton(strings.viewProfile, Icons.Filled.Visibility, KampusAdminTheme.AccentBlue, Modifier.weight(1f)) {
                        onViewUserProfile(user.uid)
                    }
                    AdminActionButton(strings.chat, Icons.Outlined.ChatBubbleOutline, KampusAdminTheme.TealPrimary, Modifier.weight(1f)) {
                        onChatWithUser(user.uid)
                    }
                    AdminActionButton(strings.changeRole, Icons.Filled.ManageAccounts, KampusAdminTheme.AccentPurple, Modifier.weight(1f)) {
                        val nextRole = if (user.role == "admin") "student" else "admin"
                        viewModel.changeUserRole(user.uid, nextRole)
                    }
                    AdminActionButton(strings.banUserAdmin, Icons.Filled.Block, KampusAdminTheme.DangerRed, Modifier.weight(1f)) {
                        showBanDialog = true
                    }
                }
            }
        }
    }
}

@Composable
fun AdminActionButton(label: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// REPORTS TAB
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AdminReportsTab(state: AdminUiState, viewModel: AdminViewModel, strings: UiStrings) {
    var selectedReport by remember { mutableStateOf<ReportedItem?>(null) }

    if (selectedReport != null) {
        ReportDetailDialog(
            report = selectedReport!!,
            onDismiss = { selectedReport = null },
            onAction = { action ->
                when (action) {
                    "DELETE" -> viewModel.deleteReportedContent(selectedReport!!.id, selectedReport!!.type, selectedReport!!.contentId, selectedReport!!.groupId)
                    "RESTRICT" -> viewModel.restrictContent(selectedReport!!.id, selectedReport!!.type, selectedReport!!.contentId)
                    "DISMISS" -> viewModel.dismissReport(selectedReport!!.id)
                }
                selectedReport = null
            },
            strings = strings
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))
        Text("${state.reportedItems.size} OPEN REPORTS", color = KampusAdminTheme.AccentOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
            items(state.reportedItems) { item -> 
                ReportCard(
                    item = item, 
                    onViewDetail = { selectedReport = item },
                    onDismiss = { viewModel.dismissReport(item.id) },
                    onDelete = { viewModel.deleteReportedContent(item.id, item.type, item.contentId, item.groupId) }
                ) 
            }
        }
    }
}

@Composable
fun ReportCard(
    item: ReportedItem, 
    onViewDetail: () -> Unit,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(KampusAdminTheme.NavyCard, RoundedCornerShape(16.dp))
            .border(1.dp, KampusAdminTheme.NavySurface, RoundedCornerShape(16.dp))
            .clickable { onViewDetail() }
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(KampusAdminTheme.AccentOrange.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) { Text(item.type, color = KampusAdminTheme.AccentOrange, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.weight(1f))
                Text("by ${item.reportedBy}", color = KampusAdminTheme.TextMuted, fontSize = 11.sp)
            }
            Spacer(Modifier.height(12.dp))
            Text(item.reason, color = KampusAdminTheme.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(item.content, color = KampusAdminTheme.TextSecond, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onViewDetail,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = KampusAdminTheme.NavySurface),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("View Detail", color = KampusAdminTheme.TextPrimary, fontSize = 12.sp) }
                
                Button(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = KampusAdminTheme.DangerRed),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Delete", color = Color.White, fontSize = 12.sp) }
            }
        }
    }
}

@Composable
fun ReportDetailDialog(
    report: ReportedItem,
    onDismiss: () -> Unit,
    onAction: (String) -> Unit,
    strings: UiStrings
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = KampusAdminTheme.NavyCard,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Report Details", style = MaterialTheme.typography.headlineSmall, color = KampusAdminTheme.TextPrimary)
                Spacer(Modifier.height(16.dp))
                
                DetailItem("Content Type", report.type)
                DetailItem("Reason", report.reason)
                DetailItem("Reported By", report.reportedBy)
                DetailItem(
                    "Reported At",
                    if (report.reportCreatedAt > 0L) {
                        DateUtils.getRelativeTimeSpanString(
                            report.reportCreatedAt,
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS,
                        ).toString()
                    } else {
                        "Unknown"
                    }
                )
                if (report.contentAuthor.isNotBlank()) {
                    DetailItem("Content Author", report.contentAuthor)
                }
                if (report.contentCreatedAt > 0L) {
                    DetailItem(
                        "Content Created",
                        DateUtils.getRelativeTimeSpanString(
                            report.contentCreatedAt,
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS,
                        ).toString(),
                    )
                }
                if (report.contentVisibility.isNotBlank()) {
                    DetailItem("Visibility", report.contentVisibility)
                }
                if (report.contentLikeCount > 0 || report.contentCommentCount > 0 || report.contentShareCount > 0) {
                    DetailItem(
                        "Engagement",
                        "${report.contentLikeCount} likes • ${report.contentCommentCount} comments • ${report.contentShareCount} shares",
                    )
                }
                
                Spacer(Modifier.height(16.dp))
                Text("Content Preview:", color = KampusAdminTheme.TextSecond, fontSize = 12.sp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(KampusAdminTheme.NavyDeep, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(report.content, color = KampusAdminTheme.TextPrimary, fontSize = 14.sp)
                }

                if (report.contentBody.isNotBlank() && report.contentBody != report.content) {
                    Spacer(Modifier.height(12.dp))
                    Text("Original Content:", color = KampusAdminTheme.TextSecond, fontSize = 12.sp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(KampusAdminTheme.NavyDeep, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(report.contentBody, color = KampusAdminTheme.TextPrimary, fontSize = 14.sp)
                    }
                }
                
                Spacer(Modifier.height(24.dp))
                
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { onAction("RESTRICT") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = KampusAdminTheme.AccentOrange),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Restrict Content (Hide)", color = Color.White) }
                    
                    Button(
                        onClick = { onAction("DELETE") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = KampusAdminTheme.DangerRed),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Delete Content Permanently", color = Color.White) }
                    
                    TextButton(
                        onClick = { onAction("DISMISS") },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Dismiss Report", color = KampusAdminTheme.TextSecond) }
                    
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Close", color = KampusAdminTheme.TextMuted) }
                }
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, color = KampusAdminTheme.TextMuted, fontSize = 11.sp)
        Text(value, color = KampusAdminTheme.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CONTENT MANAGEMENT TAB
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AdminContentTab(state: AdminUiState, viewModel: AdminViewModel, strings: UiStrings) {
    var selectedFilter by remember { mutableStateOf("POSTS") }
    val filters = listOf("POSTS", "GROUPS", "EVENTS")

    val items = when(selectedFilter) {
        "POSTS" -> state.manageablePosts
        "GROUPS" -> state.manageableGroups
        "EVENTS" -> state.manageableEvents
        else -> emptyList()
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filters.forEach { filter ->
                val active = selectedFilter == filter
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (active) KampusAdminTheme.TealPrimary else KampusAdminTheme.NavyCard)
                        .clickable { selectedFilter = filter }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(filter, color = if (active) KampusAdminTheme.OnTeal else KampusAdminTheme.TextSecond, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
            items(items) { item ->
                ContentItemCard(item) {
                    viewModel.deleteContent(item.type, item.id)
                }
            }
        }
    }
}

@Composable
fun ContentItemCard(item: ManageableContent, onDelete: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(KampusAdminTheme.NavyCard, RoundedCornerShape(16.dp))
            .border(1.dp, KampusAdminTheme.NavySurface, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val iconBg = when(item.type) {
                "POST" -> KampusAdminTheme.NavySurface
                "GROUP" -> KampusAdminTheme.AccentBlue.copy(alpha = 0.2f)
                "EVENT" -> KampusAdminTheme.AccentPurple.copy(alpha = 0.2f)
                else -> KampusAdminTheme.NavySurface
            }
            val shape = if (item.type == "GROUP") CircleShape else RoundedCornerShape(8.dp)
            
            Box(Modifier.size(40.dp).background(iconBg, shape))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, color = KampusAdminTheme.TextPrimary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.subtitle, color = KampusAdminTheme.TextMuted, fontSize = 11.sp)
            }
            IconButton(onClick = onDelete) { 
                Icon(Icons.Default.Delete, null, tint = KampusAdminTheme.DangerRed, modifier = Modifier.size(18.dp)) 
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// NOTIFICATIONS TAB
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AdminNotificationsTab(state: AdminUiState, viewModel: AdminViewModel, strings: UiStrings) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var selectedAudience by remember { mutableStateOf("ALL USERS") }
    var showSentDialog by remember { mutableStateOf(false) }

    if (showSentDialog) {
        NotificationSentDialog(selectedAudience, strings) { showSentDialog = false }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(24.dp))
        Text(strings.massNotifications, color = KampusAdminTheme.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(strings.sendAnnouncements, color = KampusAdminTheme.TextSecond, fontSize = 13.sp)
        
        Spacer(Modifier.height(24.dp))
        
        // Composition Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(KampusAdminTheme.NavyCard, RoundedCornerShape(20.dp))
                .border(1.dp, KampusAdminTheme.NavySurface, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(strings.composeAnnouncement, color = KampusAdminTheme.TealPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                
                // Audience Selector
                Column {
                    Text(strings.audience, color = KampusAdminTheme.TextSecond, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("ALL USERS", "ADMINS", "STUDENTS").forEach { aud ->
                            val active = selectedAudience == aud
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (active) KampusAdminTheme.TealPrimary.copy(alpha = 0.1f) else KampusAdminTheme.NavyDeep)
                                    .border(1.dp, if (active) KampusAdminTheme.TealPrimary else Color.Transparent, RoundedCornerShape(10.dp))
                                    .clickable { selectedAudience = aud }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) { Text(aud, color = if (active) KampusAdminTheme.TealPrimary else KampusAdminTheme.TextSecond, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
                
                // Input Fields
                Column {
                    Text(strings.title, color = KampusAdminTheme.TextSecond, fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    BasicTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(KampusAdminTheme.NavyDeep, RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(color = KampusAdminTheme.TextPrimary, fontSize = 14.sp)
                    )
                }

                Column {
                    Text(strings.message, color = KampusAdminTheme.TextSecond, fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    BasicTextField(
                        value = message,
                        onValueChange = { message = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp)
                            .background(KampusAdminTheme.NavyDeep, RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(color = KampusAdminTheme.TextPrimary, fontSize = 14.sp)
                    )
                }
                
                Button(
                    onClick = { 
                        viewModel.sendAnnouncement(title, message, selectedAudience)
                        title = ""
                        message = ""
                        showSentDialog = true
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KampusAdminTheme.TealPrimary),
                    shape = RoundedCornerShape(14.dp),
                    enabled = title.isNotBlank() && message.isNotBlank()
                ) {
                    Text("${strings.sendTo} $selectedAudience", color = KampusAdminTheme.OnTeal, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))
        Text(strings.pastAnnouncements, color = KampusAdminTheme.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        
        state.pastAnnouncements.forEach { (t, m, time) ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .background(KampusAdminTheme.NavyCard.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .border(1.dp, KampusAdminTheme.NavySurface.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(t, color = KampusAdminTheme.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Text(DateUtils.getRelativeTimeSpanString(time).toString(), color = KampusAdminTheme.TextMuted, fontSize = 10.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(m, color = KampusAdminTheme.TextSecond, fontSize = 12.sp, maxLines = 2)
                }
            }
        }
        
        Spacer(Modifier.height(100.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PROFILE TAB
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AdminProfileTab(
    onLogout: () -> Unit,
    profileViewModel: ProfileViewModel,
    adminUiState: AdminUiState,
    strings: UiStrings,
    onEditProfile: () -> Unit,
    onEditCoverImage: () -> Unit,
    onChatClick: () -> Unit
) {
    val profileState by profileViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showSettingsInProfile by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            ProfileHeader(
                state = profileState,
                onBack = { },
                onOpenSettings = { showSettingsInProfile = !showSettingsInProfile },
                isOnline = profileState.isOnline,
                onEditCoverImage = onEditCoverImage,
                showBackButton = false,
                showSettingsButton = true
            )
        }

        if (showSettingsInProfile) {
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.settings, color = KampusAdminTheme.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { showSettingsInProfile = false }) {
                            Icon(Icons.Default.Close, null, tint = KampusAdminTheme.TextPrimary)
                        }
                    }

                    val scope = rememberCoroutineScope()

                    // Settings Section (Theme & Interface)
                    var showLanguageMenu by remember { mutableStateOf(false) }

                    AdminSettingsSection(title = strings.appSettingsTitle) {
                        AdminSettingRow(
                            icon = Icons.Outlined.Palette,
                            label = strings.dark,
                            color = KampusAdminTheme.AccentPurple,
                            onClick = {
                                ThemeController.isDark = !ThemeController.isDark
                                scope.launch {
                                    AppSettingsStore.saveTheme(context, isDark = ThemeController.isDark)
                                }
                            }
                        ) {
                            Switch(
                                checked = ThemeController.isDark,
                                onCheckedChange = {
                                    ThemeController.isDark = it
                                    scope.launch {
                                        AppSettingsStore.saveTheme(context, isDark = it)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = KampusAdminTheme.TealPrimary,
                                    checkedTrackColor = KampusAdminTheme.TealPrimary.copy(alpha = 0.5f),
                                    uncheckedThumbColor = KampusAdminTheme.TextMuted,
                                    uncheckedTrackColor = KampusAdminTheme.NavySurface
                                )
                            )
                        }
                        HorizontalDivider(color = KampusAdminTheme.NavySurface)
                        
                        Box {
                            AdminSettingRow(
                                icon = Icons.Outlined.Language,
                                label = strings.languageAndRegion,
                                color = KampusAdminTheme.AccentBlue,
                                onClick = { showLanguageMenu = true }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        if (LanguageManager.getLanguageCode(context) == LanguageManager.KHMER) "ភាសាខ្មែរ" else "English",
                                        color = KampusAdminTheme.TextMuted,
                                        fontSize = 12.sp
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = KampusAdminTheme.TextMuted)
                                }
                            }

                            DropdownMenu(
                                expanded = showLanguageMenu,
                                onDismissRequest = { showLanguageMenu = false },
                                modifier = Modifier
                                    .background(KampusAdminTheme.NavyCard)
                                    .border(1.dp, KampusAdminTheme.Border.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            ) {
                                DropdownMenuItem(
                                    text = { Text("English", color = KampusAdminTheme.TextPrimary) },
                                    onClick = {
                                        LanguageManager.setLanguage(context, LanguageManager.ENGLISH)
                                        showLanguageMenu = false
                                    },
                                    leadingIcon = {
                                        if (LanguageManager.getLanguageCode(context) == LanguageManager.ENGLISH) {
                                            Icon(Icons.Default.Check, null, tint = KampusAdminTheme.TealPrimary, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("ភាសាខ្មែរ", color = KampusAdminTheme.TextPrimary) },
                                    onClick = {
                                        LanguageManager.setLanguage(context, LanguageManager.KHMER)
                                        showLanguageMenu = false
                                    },
                                    leadingIcon = {
                                        if (LanguageManager.getLanguageCode(context) == LanguageManager.KHMER) {
                                            Icon(Icons.Default.Check, null, tint = KampusAdminTheme.TealPrimary, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Account Details Section
                    AdminSettingsSection(title = strings.adminDetailsTitle) {
                        AdminDetailRow(Icons.Outlined.Shield, strings.permissions, "Full System Access")
                        HorizontalDivider(color = KampusAdminTheme.NavySurface)
                        AdminDetailRow(Icons.Outlined.History, strings.lastLogin, "Today, 10:24 AM")
                        HorizontalDivider(color = KampusAdminTheme.NavySurface)
                        AdminDetailRow(Icons.Outlined.Place, strings.region, "Phnom Penh, KH")
                    }

                    // Action Section
                    Button(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = KampusAdminTheme.NavyCard),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, KampusAdminTheme.DangerRed.copy(alpha = 0.3f))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, null, tint = KampusAdminTheme.DangerRed, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(strings.logOut, color = KampusAdminTheme.DangerRed, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 54.dp, start = 24.dp, end = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = profileState.displayName.ifBlank { "Super Admin" },
                        color = KampusAdminTheme.TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.superAdmin,
                        color = KampusAdminTheme.TealPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = profileState.email,
                        color = KampusAdminTheme.TextMuted,
                        fontSize = 13.sp
                    )
                }
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Admin Action Buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = onChatClick,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = KampusAdminTheme.TealPrimary, contentColor = KampusAdminTheme.OnTeal),
                        ) {
                            Icon(Icons.Outlined.ChatBubbleOutline, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(strings.chat, fontWeight = FontWeight.Medium)
                        }
                        
                        Button(
                            onClick = {
                                val link = profileViewModel.getShareProfileLink()
                                if (link != null) {
                                    profileViewModel.logShareProfileActivity()
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "KAMPUS Admin Profile")
                                        putExtra(Intent.EXTRA_TEXT, "KAMPUS Admin: $link")
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share profile"))
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = KampusAdminTheme.NavyCard, contentColor = KampusAdminTheme.TextPrimary),
                        ) {
                            Icon(Icons.Outlined.Share, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(strings.shareProfile, fontWeight = FontWeight.Medium)
                        }
                    }
                    
                    Button(
                        onClick = onEditProfile,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = KampusAdminTheme.NavyCard, contentColor = KampusAdminTheme.TextPrimary),
                        border = BorderStroke(1.dp, KampusAdminTheme.Border.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Outlined.ModeEdit, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(strings.editProfile, fontWeight = FontWeight.Medium)
                    }
                }
            }
            item {
                Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                    RecentActivitySection(
                        activities = adminUiState.recentActivities,
                        strings = strings
                    )
                }
            }
        }
    }
}

@Composable
fun AdminSettingsSection(title: String, content: @Composable (ColumnScope.() -> Unit)) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(KampusAdminTheme.NavyCard, RoundedCornerShape(20.dp))
            .border(1.dp, KampusAdminTheme.Border.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(title, color = KampusAdminTheme.TealPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable
fun AdminSettingRow(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit = {},
    action: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = KampusAdminTheme.TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
        action()
    }
}

@Composable
fun AdminDetailRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = KampusAdminTheme.TextMuted, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = KampusAdminTheme.TextSecond, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(value, color = KampusAdminTheme.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BasicTextField alias (avoid full import path)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun BasicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    textStyle: androidx.compose.ui.text.TextStyle = androidx.compose.ui.text.TextStyle.Default,
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit = { it() }
) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = singleLine,
        textStyle = textStyle,
        decorationBox = decorationBox
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Material 3 Custom Theme for Admin
// ─────────────────────────────────────────────────────────────────────────────
private val AdminColorScheme = darkColorScheme(
    primary = KampusAdminTheme.TealPrimary,
    onPrimary = KampusAdminTheme.OnTeal,
    secondary = KampusAdminTheme.AccentBlue,
    onSecondary = Color.White,
    surface = KampusAdminTheme.NavyMid,
    onSurface = KampusAdminTheme.TextPrimary,
    background = KampusAdminTheme.NavyDeep,
    onBackground = KampusAdminTheme.TextPrimary
)

@Composable
fun KampusAdminMaterialTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AdminColorScheme,
        content = content
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Ban User Confirmation Dialog
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun BanUserDialog(userName: String, onConfirm: () -> Unit, onDismiss: () -> Unit, strings: UiStrings) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(KampusAdminTheme.NavyCard, RoundedCornerShape(20.dp))
                .border(1.dp, KampusAdminTheme.DangerRed.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(KampusAdminTheme.DangerRed.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Block, null, tint = KampusAdminTheme.DangerRed, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.height(14.dp))
                Text(strings.banUserAdmin, color = KampusAdminTheme.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    strings.banConfirmation.format(userName),
                    color = KampusAdminTheme.TextSecond,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(KampusAdminTheme.NavySurface, RoundedCornerShape(12.dp))
                            .clickable(onClick = onDismiss)
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) { Text(strings.cancel, color = KampusAdminTheme.TextSecond, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(KampusAdminTheme.DangerRed, RoundedCornerShape(12.dp))
                            .clickable(onClick = onConfirm)
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) { Text(strings.banUserAdmin, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Mass Notification Sent Dialog
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun NotificationSentDialog(audience: String, strings: UiStrings, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(KampusAdminTheme.NavyCard, RoundedCornerShape(20.dp))
                .border(1.dp, KampusAdminTheme.TealPrimary.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(KampusAdminTheme.TealPrimary.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.CheckCircle, null, tint = KampusAdminTheme.TealPrimary, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.height(14.dp))
                Text(strings.notificationSentTitle, color = KampusAdminTheme.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "${strings.deliveredToLabel} $audience.",
                    color = KampusAdminTheme.TextSecond,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(KampusAdminTheme.TealPrimary, RoundedCornerShape(12.dp))
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) { Text(strings.doneButton, color = KampusAdminTheme.OnTeal, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}
