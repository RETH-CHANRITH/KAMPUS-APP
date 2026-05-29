package com.example.kampus.navigation

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.kampus.ui.onboarding.OnboardingScreen
import com.example.kampus.ui.splash.SplashScreen
import com.example.kampus.ui.auth.ForgotPasswordScreen
import com.example.kampus.ui.auth.LoginScreen
import com.example.kampus.ui.auth.OtpScreen
import com.example.kampus.ui.auth.RegisterScreen
import com.example.kampus.ui.auth.ResetPasswordScreen
import com.example.kampus.ui.chat.ChatListScreen
import com.example.kampus.ui.chat.ChatScreen
import com.example.kampus.ui.chat.ChatViewModel
import com.example.kampus.ui.events.CreateEventScreen
import com.example.kampus.ui.events.NewEventData
import com.example.kampus.ui.feed.CreatePostScreen
import com.example.kampus.ui.feed.FeedViewModel
import com.example.kampus.ui.feed.HomeScreen
import com.example.kampus.ui.admin.AdminPanelScreen
import com.example.kampus.ui.screens.groups.groupsNavGraph
import com.example.kampus.viewmodel.GroupsViewModel
import com.example.kampus.ui.screens.groups.GroupRoutes
import com.example.kampus.ui.notifications.NotificationScreen
import com.example.kampus.ui.events.EventDetailScreen
import com.example.kampus.ui.events.EventListScreen
import com.example.kampus.ui.events.EventViewModel
import com.example.kampus.ui.profile.AccountSettingsScreen
import com.example.kampus.ui.profile.AboutScreen
import com.example.kampus.ui.profile.AppearanceSettingsScreen
import com.example.kampus.ui.profile.BlockedUsersScreen
import com.example.kampus.ui.profile.EditProfileScreen
import com.example.kampus.ui.profile.DiscoverPeopleScreen
import com.example.kampus.ui.profile.LanguageRegionScreen
import com.example.kampus.ui.profile.FriendRequestsScreen
import com.example.kampus.ui.profile.FriendsScreen
import com.example.kampus.ui.profile.HelpSupportScreen
import com.example.kampus.ui.profile.NotificationSettingsScreen
import com.example.kampus.ui.profile.PrivacySecurityScreen
import com.example.kampus.ui.profile.ProfileScreen
import com.example.kampus.ui.profile.PublicProfileScreen
import com.example.kampus.ui.profile.PublicFriendsScreen
import com.example.kampus.ui.profile.SettingsScreen
import com.example.kampus.ui.post.PostDetailScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object Routes {
    const val SPLASH          = "splash"
    const val ONBOARDING      = "onboarding"
    const val LOGIN           = "login"
    const val LOGIN_WITH_EMAIL = "login?email={email}"
    const val REGISTER        = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val OTP             = "otp/{method}/{contact}"
    const val RESET_PASSWORD  = "reset_password"
    const val HOME            = "home"
    const val ADMIN_PANEL     = "admin_panel"
    const val NOTIFICATIONS   = "notifications"

    // Feed
    const val POST_CREATE     = "post_create"
    const val POST_DETAIL     = "post_detail/{postId}?openComposer={openComposer}"

    // Groups
    const val GROUP_LIST   = "groups_graph"

    // Events
    const val EVENT_LIST   = "event_list"
    const val EVENT_DETAIL = "event_detail/{eventId}?openComposer={openComposer}"
    const val EVENT_CREATE = "event_create"

    // Chat
    const val CHAT_LIST   = "chat_list"
    const val CHAT_SCREEN = "chat_screen/{chatId}"
    const val CHAT_WITH_USER = "chat_with_user/{userId}"
    const val CALL_SCREEN = "call_screen/{chatId}/{callType}?callId={callId}"
    const val INCOMING_CALL_SCREEN = "incoming_call/{chatId}/{callType}?callId={callId}"

    // Profile
    const val PROFILE      = "profile"
    const val PROFILE_PUBLIC = "profile_public/{userId}"
    const val FRIENDS_PUBLIC = "friends/{userId}?tab={tab}"
    const val FRIEND_REQUESTS = "friend_requests"
    const val FRIENDS = "friends"
    const val DISCOVER_PEOPLE = "discover_people"
    const val SETTINGS     = "settings"
    const val ACCOUNT_SETTINGS = "account_settings"
    const val NOTIFICATION_SETTINGS = "notification_settings"
    const val PRIVACY_SECURITY_SETTINGS = "privacy_security_settings"
    const val APPEARANCE_SETTINGS = "appearance_settings"
    const val BLOCKED_USERS_SETTINGS = "blocked_users_settings"
    const val HELP_SUPPORT_SETTINGS = "help_support_settings"
    const val ABOUT_SETTINGS = "about_settings"
    const val LANGUAGE_REGION_SETTINGS = "language_region_settings"
    const val PROFILE_EDIT = "profile_edit"

    fun eventDetail(eventId: Int, openComposer: Boolean = false) = "event_detail/$eventId?openComposer=$openComposer"
    fun postDetail(postId: Int, openComposer: Boolean = false) = "post_detail/$postId?openComposer=$openComposer"
    fun profilePublic(userId: String) = "profile_public/$userId"
    fun friendsPublic(userId: String, tab: Int = 1) = "friends/$userId?tab=$tab"
    fun chatScreen(chatId: String)   = "chat_screen/$chatId"
    fun chatWithUser(userId: String) = "chat_with_user/$userId"
    fun callScreen(chatId: String, callType: String, callId: String = "") =
        "call_screen/$chatId/$callType?callId=$callId"
    fun incomingCallScreen(chatId: String, callType: String, callId: String = "") =
        "incoming_call/$chatId/$callType?callId=$callId"
    fun loginWithEmail(email: String) = "login?email=${email.encodeUrl()}"

    fun otp(method: String, contact: String) =
        "otp/$method/${contact.encodeUrl()}"

    private fun String.encodeUrl() =
        java.net.URLEncoder.encode(this, "UTF-8")
}

@SuppressLint("StateFlowValueCalledInComposition", "UnrememberedGetBackStackEntry")
@Composable
fun NavGraph(navController: NavHostController) {
    val globalChatViewModel: ChatViewModel = viewModel()
    val context = navController.context
    val scope = rememberCoroutineScope()

    suspend fun resolveRoleBasedDestination(): String {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return Routes.LOGIN
        return runCatching {
            val snapshot = FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUser.uid)
                .get()
                .await()
            if (snapshot.getString("role").equals("admin", ignoreCase = true)) {
                Routes.ADMIN_PANEL
            } else {
                Routes.HOME
            }
        }.getOrDefault(Routes.HOME)
    }

    fun navigateToRoleAwareDestination(clearFrom: String) {
        scope.launch {
            val destination = resolveRoleBasedDestination()
            navController.navigate(destination) {
                popUpTo(clearFrom) { inclusive = true }
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            val activity = context as? android.app.Activity
            val extras = activity?.intent?.extras
            val openChatId = extras?.getString("openChatId")
            val storyId = extras?.getString("storyId")
            val replyId = extras?.getString("replyId")
            if (!openChatId.isNullOrBlank()) {
                navController.navigate(Routes.chatScreen(openChatId))
                if (!replyId.isNullOrBlank()) {
                    globalChatViewModel.openChatAndFocusMessage(openChatId, replyId)
                }
            }
        } catch (_: Exception) {
        }
    }
    val incomingCall = globalChatViewModel.incomingCallState.collectAsStateWithLifecycle()
    val groupsViewModel: GroupsViewModel = viewModel()

    LaunchedEffect(Unit) {
        globalChatViewModel.startIncomingCallListener()
    }

    LaunchedEffect(incomingCall.value?.callId) {
        val invite = incomingCall.value ?: return@LaunchedEffect
        val currentRoute = navController.currentBackStackEntry?.destination?.route.orEmpty()

        if (!currentRoute.startsWith("incoming_call")) {
            navController.navigate(Routes.incomingCallScreen(invite.chatId, invite.callType, invite.callId))
        }
    }

    NavHost(navController = navController, startDestination = Routes.SPLASH) {

        // ── Splash ─────────────────────────────────────────────────────────────
        composable(Routes.SPLASH) {
            SplashScreen(
                onNavigateToOnboarding = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToAuthenticated = {
                    navigateToRoleAwareDestination(Routes.SPLASH)
                },
            )
        }

        // ── Onboarding ─────────────────────────────────────────────────────────
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinished = {
                    // Persist onboarding completion so it doesn't show again.
                    navController.context
                        .getSharedPreferences("kampus_prefs", android.content.Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean("has_seen_onboarding", true)
                        .apply()

                    val isLoggedIn = FirebaseAuth.getInstance().currentUser != null
                    if (isLoggedIn) {
                        navigateToRoleAwareDestination(Routes.ONBOARDING)
                    } else {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    }
                }
            )
        }

        // ── Login ──────────────────────────────────────────────────────────────
        composable(
            route = Routes.LOGIN_WITH_EMAIL,
            arguments = listOf(
                navArgument("email") {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = false
                }
            )
        ) { back ->
            val prefilledEmail = java.net.URLDecoder.decode(
                back.arguments?.getString("email") ?: "",
                "UTF-8"
            )
            LoginScreen(
                onLoginSuccess   = {
                    navigateToRoleAwareDestination(Routes.LOGIN)
                },
                onRegisterClick  = { navController.navigate(Routes.REGISTER) },
                onForgotPassword = { navController.navigate(Routes.FORGOT_PASSWORD) },
                onGoogleClick    = { },
                onAppleClick     = { },
                onBackClick      = { navController.popBackStack() },
                prefilledEmail   = prefilledEmail,
            )
        }

        // ── Register ───────────────────────────────────────────────────────────
        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = { email ->
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate(Routes.loginWithEmail(email)) {
                        popUpTo(Routes.REGISTER) { inclusive = true }
                    }
                },
                onLoginClick      = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onGoogleClick = { },
                onAppleClick  = { },
                onBackClick   = { navController.popBackStack() }
            )
        }

        // ── Forgot Password ────────────────────────────────────────────────────
        composable(Routes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(
                onNextClick = { method, contact ->
                    navController.navigate(Routes.otp(method, contact))
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        // ── OTP ────────────────────────────────────────────────────────────────
        composable(
            route     = Routes.OTP,
            arguments = listOf(
                navArgument("method")  { type = NavType.StringType },
                navArgument("contact") { type = NavType.StringType },
            )
        ) { back ->
            val method  = back.arguments?.getString("method") ?: "email"
            val contact = java.net.URLDecoder.decode(
                back.arguments?.getString("contact") ?: "", "UTF-8"
            )
            OtpScreen(
                method          = method,
                contact         = contact,
                onVerifySuccess = {
                    navController.navigate(Routes.RESET_PASSWORD) {
                        popUpTo(Routes.FORGOT_PASSWORD) { inclusive = true }
                    }
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        // ── Reset Password ─────────────────────────────────────────────────────
        composable(Routes.RESET_PASSWORD) {
            ResetPasswordScreen(
                onResetSuccess = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        // ── Home ───────────────────────────────────────────────────────────────
        composable(Routes.HOME) {
            val homeEntry = remember(it) { navController.getBackStackEntry(Routes.HOME) }
            val feedViewModel: FeedViewModel = viewModel(homeEntry)
            HomeScreen(
                onCreatePost   = { navController.navigate(Routes.POST_CREATE) },
                onProfileClick = { navController.navigate(Routes.PROFILE) },
                onNotifClick   = { navController.navigate(Routes.NOTIFICATIONS) },
                onSearchClick  = { },
                onPostClick    = { postId -> navController.navigate(Routes.postDetail(postId)) },
                onGroupsClick  = { navController.navigate(Routes.GROUP_LIST) },
                onEventsClick  = { navController.navigate(Routes.EVENT_LIST) },
                onChatClick    = { navController.navigate(Routes.CHAT_LIST) },
                onAdminClick   = { navController.navigate(Routes.ADMIN_PANEL) },
                viewModel      = feedViewModel,
                chatViewModel  = globalChatViewModel,
            )
        }

        // ── Admin Panel ────────────────────────────────────────────────────────
        composable(Routes.ADMIN_PANEL) {
            AdminPanelScreen(
                onLogout = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onViewUserProfile = { userId ->
                    navController.navigate(Routes.profilePublic(userId))
                },
                onChatClick = {
                    navController.navigate(Routes.CHAT_LIST)
                },
                onChatWithUser = { userId ->
                    navController.navigate(Routes.chatWithUser(userId))
                }
            )
        }

        composable(Routes.NOTIFICATIONS) {
            NotificationScreen(
                onBack = { navController.popBackStack() },
                onNavigate = { route -> navController.navigate(route) }
            )
        }

        // ── Create Post ───────────────────────────────────────────────────────
        composable(Routes.POST_CREATE) {
            val homeEntry = remember { navController.getBackStackEntry(Routes.HOME) }
            val feedViewModel: FeedViewModel = viewModel(homeEntry)
            CreatePostScreen(
                onClose = { navController.popBackStack() },
                onPost = { text, mediaUris, mediaTypes, visibility, allowComments, taggedPeople, feelingEmoji, location ->
                    feedViewModel.addPost(
                        text = text,
                        mediaUris = mediaUris,
                        mediaTypes = mediaTypes,
                        visibility = visibility,
                        allowComments = allowComments,
                        taggedPeople = taggedPeople,
                        feelingEmoji = feelingEmoji,
                        location = location,
                    )
                },
            )
        }

        // ── Post Detail ───────────────────────────────────────────────────────
        composable(
            route = Routes.POST_DETAIL,
            arguments = listOf(
                navArgument("postId") { type = NavType.IntType },
                navArgument("openComposer") { type = NavType.BoolType; defaultValue = false },
            ),
        ) { back ->
            val postId = back.arguments?.getInt("postId") ?: return@composable
            val openComposer = back.arguments?.getBoolean("openComposer") ?: false
            PostDetailScreen(
                postId = postId,
                openComposer = openComposer,
                onBack = { navController.popBackStack() },
            )
        }

        // ── Group List ─────────────────────────────────────────────────────────
        // ── Groups Feature ────────────────────────────────────────────────────
        groupsNavGraph(
            navController = navController,
            sharedViewModel = groupsViewModel,
        )

        // ── Event List ─────────────────────────────────────────────────────────
        composable(Routes.EVENT_LIST) {
            val vm: EventViewModel = viewModel()
            EventListScreen(
                viewModel      = vm,
                onEventClick   = { navController.navigate(Routes.eventDetail(it.id)) },
                onCommentOpen  = { navController.navigate(Routes.eventDetail(it.id, true)) },
                onCreateClick  = { navController.navigate(Routes.EVENT_CREATE) },
                onHomeClick    = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = false }
                    }
                },
                onGroupsClick  = { navController.navigate(Routes.GROUP_LIST) },
                onChatClick    = { navController.navigate(Routes.CHAT_LIST) },
                onAdminClick   = { navController.navigate(Routes.ADMIN_PANEL) },
                onFabClick     = { navController.navigate(Routes.POST_CREATE) },
                onProfileClick = { navController.navigate(Routes.PROFILE) },
            )
        }

        // ── Event Detail ───────────────────────────────────────────────────────
        composable(
            route     = Routes.EVENT_DETAIL,
            arguments = listOf(
                navArgument("eventId") { type = NavType.IntType },
                navArgument("openComposer") { type = NavType.BoolType; defaultValue = false }
            )
        ) { back ->
            val eventId   = back.arguments?.getInt("eventId") ?: return@composable
            val openComposer = back.arguments?.getBoolean("openComposer") ?: false
            // Safe: when navigating from profile screens, EVENT_LIST may not be in the back stack.
            // Try to get the shared ViewModel; fall back to a standalone one if EVENT_LIST is absent.
            val hasEventList = runCatching { navController.getBackStackEntry(Routes.EVENT_LIST) }.isSuccess
            val vm: EventViewModel = if (hasEventList) {
                viewModel(navController.getBackStackEntry(Routes.EVENT_LIST))
            } else {
                viewModel(back)
            }
            val state = vm.uiState.collectAsStateWithLifecycle().value
            val event = state.events.firstOrNull { it.id == eventId }
            if (event == null) {
                // Still loading — show spinner until the ViewModel fetches events
                Box(
                    modifier = Modifier.fillMaxSize()
                        .background(com.example.kampus.ui.events.EventColors.Bg),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = com.example.kampus.ui.events.EventColors.Blue)
                }
            } else {
                EventDetailScreen(
                    event        = event,
                    isInterested = event.id in state.interestedIds,
                    isLiked      = event.id in state.likedIds,
                    isSaved      = event.id in state.savedIds,
                    onInterested = { vm.toggleInterested(event) },
                    onLike       = { vm.toggleLike(event) },
                    onSave       = { vm.toggleSave(event) },
                    onBack       = { navController.popBackStack() },
                    viewModel    = vm,
                    openComposer = openComposer,
                )
            }
        }

        // ── Event Create ───────────────────────────────────────────────────────
        composable(Routes.EVENT_CREATE) { backStackEntry ->
            val listEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Routes.EVENT_LIST)
            }
            val vm: EventViewModel = viewModel(listEntry)
            val handleBack: () -> Unit    = { navController.popBackStack() }
            val handlePublish: suspend (NewEventData) -> Result<String> = { data -> vm.createEvent(data) }
            CreateEventScreen(
                onBack    = handleBack,
                onPublish = handlePublish,
            )
        }

        // ── Chat List ──────────────────────────────────────────────────────────
        composable(Routes.CHAT_LIST) {
            ChatListScreen(
                viewModel      = globalChatViewModel,
                onChatClick    = { chatId -> navController.navigate(Routes.chatScreen(chatId)) },
                onHomeClick    = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = false }
                    }
                },
                onGroupsClick  = { navController.navigate(Routes.GROUP_LIST) },
                onEventsClick  = { navController.navigate(Routes.EVENT_LIST) },
                onAdminClick   = { navController.navigate(Routes.ADMIN_PANEL) },
                onProfileClick = { navController.navigate(Routes.PROFILE) },
                onCreatePost   = { navController.navigate(Routes.POST_CREATE) },
            )
        }

        // ── Public Profile (Share Link Target) ───────────────────────────────
        composable(
            route = Routes.PROFILE_PUBLIC,
            arguments = listOf(navArgument("userId") { type = NavType.StringType }),
            deepLinks = listOf(
                navDeepLink { uriPattern = "https://kampus.app/profile/{userId}" },
                navDeepLink { uriPattern = "kampus://profile/{userId}" },
            ),
        ) { back ->
            val userId = back.arguments?.getString("userId") ?: return@composable
            PublicProfileScreen(
                userId = userId,
                onBack = { navController.popBackStack() },
                onOpenActivity = { activity ->
                    when (activity.type) {
                        "create_event", "interested_event" -> {
                            val eventId = activity.eventId
                            if (eventId != null) {
                                navController.navigate(Routes.eventDetail(eventId))
                            } else {
                                navController.navigate(Routes.EVENT_LIST)
                            }
                        }

                        "create_post", "share_post" -> {
                            val postId = activity.postId
                            if (postId != null) {
                                navController.navigate(Routes.postDetail(postId))
                            } else {
                                navController.navigate(Routes.HOME) {
                                    popUpTo(Routes.HOME) { inclusive = false }
                                }
                            }
                        }

                        "create_group" -> {
                            val groupId = activity.groupId
                            if (!groupId.isNullOrBlank()) {
                                navController.navigate(GroupRoutes.detail(groupId))
                            } else {
                                navController.navigate(GroupRoutes.LIST)
                            }
                        }

                        else -> {
                            navController.navigate(Routes.profilePublic(userId))
                        }
                    }
                },
                onCommentClick = { activity ->
                    when (activity.type) {
                        "create_event", "interested_event" -> {
                            val eventId = activity.eventId
                            if (eventId != null) {
                                navController.navigate(Routes.eventDetail(eventId, openComposer = true))
                            }
                        }

                        "create_post", "share_post" ->
                            activity.postId?.let {
                                navController.navigate(Routes.postDetail(it, openComposer = true))
                            }
                    }
                },
                onOpenFollowers = { targetUserId ->
                    navController.navigate(Routes.friendsPublic(targetUserId, 1))
                },
                onOpenFollowing = { targetUserId ->
                    navController.navigate(Routes.friendsPublic(targetUserId, 2))
                },
                onOpenProfile = { targetUserId ->
                    navController.navigate(Routes.profilePublic(targetUserId))
                },
            )
        }

        // ── Profile ───────────────────────────────────────────────────────────
        composable(Routes.PROFILE) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onEditProfile = { navController.navigate(Routes.PROFILE_EDIT) },
                onOpenFriendRequests = { navController.navigate(Routes.FRIEND_REQUESTS) },
                onOpenFriends = { navController.navigate(Routes.FRIENDS) },
                onOpenDiscoverPeople = { navController.navigate(Routes.DISCOVER_PEOPLE) },
                onHomeClick = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = false }
                    }
                },
                onGroupsClick = { navController.navigate(Routes.GROUP_LIST) },
                onEventsClick = { navController.navigate(Routes.EVENT_LIST) },
                onChatClick = { navController.navigate(Routes.CHAT_LIST) },
                onAdminClick = { navController.navigate(Routes.ADMIN_PANEL) },
                onCreatePost = { navController.navigate(Routes.POST_CREATE) },
                onOpenActivity = { activity ->
                    when (activity.type) {
                        "create_event", "interested_event" -> {
                            val eventId = activity.eventId
                            if (eventId != null) {
                                navController.navigate(Routes.eventDetail(eventId))
                            } else {
                                navController.navigate(Routes.EVENT_LIST)
                            }
                        }

                        "create_post", "share_post" -> {
                            val postId = activity.postId
                            if (postId != null) {
                                navController.navigate(Routes.postDetail(postId))
                            } else {
                                navController.navigate(Routes.HOME) {
                                    popUpTo(Routes.HOME) { inclusive = false }
                                }
                            }
                        }

                        "create_group" -> {
                            val groupId = activity.groupId
                            if (!groupId.isNullOrBlank()) {
                                navController.navigate(GroupRoutes.detail(groupId))
                            } else {
                                navController.navigate(GroupRoutes.LIST)
                            }
                        }

                        else -> {
                            navController.navigate(Routes.PROFILE) {
                                popUpTo(Routes.PROFILE) { inclusive = false }
                            }
                        }
                    }
                },
                onCommentClick = { activity ->
                    when (activity.type) {
                        "create_event", "interested_event" -> {
                            val eventId = activity.eventId
                            if (eventId != null) {
                                navController.navigate(Routes.eventDetail(eventId, openComposer = true))
                            }
                        }

                        "create_post", "share_post" ->
                            activity.postId?.let {
                                navController.navigate(Routes.postDetail(it, openComposer = true))
                            }
                    }
                },
            )
        }

        composable(
            route = Routes.FRIENDS_PUBLIC,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("tab") { type = NavType.IntType; defaultValue = 1 }
            )
        ) { back ->
            val userId = back.arguments?.getString("userId") ?: return@composable
            val tab = back.arguments?.getInt("tab") ?: 1
            PublicFriendsScreen(
                userId = userId,
                initialTab = tab,
                onBack = { navController.popBackStack() },
                onOpenProfile = { id: String -> navController.navigate(Routes.profilePublic(id)) },
                onOpenChat = { id: String -> navController.navigate(Routes.chatWithUser(id)) },
            )
        }

        // ── Friend Requests ───────────────────────────────────────────────────
        composable(Routes.FRIEND_REQUESTS) {
            FriendRequestsScreen(
                onBack = { navController.popBackStack() },
                onOpenProfile = { userId ->
                    navController.navigate(Routes.profilePublic(userId))
                },
            )
        }

        // ── Friends ───────────────────────────────────────────────────────────
        composable(Routes.FRIENDS) {
            FriendsScreen(
                onBack = { navController.popBackStack() },
                onOpenProfile = { userId ->
                    navController.navigate(Routes.profilePublic(userId))
                },
                onOpenChat = { userId ->
                    navController.navigate(Routes.chatWithUser(userId))
                },
            )
        }

        // ── Open/Create Direct Chat By User ────────────────────────────────
        composable(
            route = Routes.CHAT_WITH_USER,
            arguments = listOf(navArgument("userId") { type = NavType.StringType }),
        ) { back ->
            val userId = back.arguments?.getString("userId") ?: return@composable
            val vm: ChatViewModel = viewModel()
            LaunchedEffect(userId) {
                val chatId = vm.getOrCreateDirectChatWithUser(userId)
                if (chatId != null) {
                    navController.navigate(Routes.chatScreen(chatId)) {
                        popUpTo(Routes.CHAT_WITH_USER) { inclusive = true }
                    }
                } else {
                    navController.popBackStack()
                }
            }
        }

        // ── Discover People ───────────────────────────────────────────────────
        composable(Routes.DISCOVER_PEOPLE) {
            DiscoverPeopleScreen(
                onBack = { navController.popBackStack() },
                onOpenProfile = { userId ->
                    navController.navigate(Routes.profilePublic(userId))
                },
            )
        }

        // ── Settings ──────────────────────────────────────────────────────────
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onEditProfile = { navController.navigate(Routes.PROFILE_EDIT) },
                onOpenAccountSettings = { navController.navigate(Routes.ACCOUNT_SETTINGS) },
                onOpenNotificationSettings = { navController.navigate(Routes.NOTIFICATION_SETTINGS) },
                onOpenPrivacySecurity = { navController.navigate(Routes.PRIVACY_SECURITY_SETTINGS) },
                onOpenAppearance = { navController.navigate(Routes.APPEARANCE_SETTINGS) },
                onOpenLanguageRegion = { navController.navigate(Routes.LANGUAGE_REGION_SETTINGS) },
                onOpenBlockedUsers = { navController.navigate(Routes.BLOCKED_USERS_SETTINGS) },
                onOpenHelpSupport = { navController.navigate(Routes.HELP_SUPPORT_SETTINGS) },
                onOpenAbout = { navController.navigate(Routes.ABOUT_SETTINGS) },
                onLogout = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        // ── Account Settings ────────────────────────────────────────────────
        composable(Routes.ACCOUNT_SETTINGS) {
            AccountSettingsScreen(onBack = { navController.popBackStack() })
        }

        // ── Notification Settings ─────────────────────────────────────────────
        composable(Routes.NOTIFICATION_SETTINGS) {
            NotificationSettingsScreen(onBack = { navController.popBackStack() })
        }

        // ── Appearance Settings ───────────────────────────────────────────────
        composable(Routes.APPEARANCE_SETTINGS) {
            AppearanceSettingsScreen(onBack = { navController.popBackStack() })
        }

        // ── Language & Region ───────────────────────────────────────────────
        composable(Routes.LANGUAGE_REGION_SETTINGS) {
            LanguageRegionScreen(onBack = { navController.popBackStack() })
        }

        // ── Blocked Users ─────────────────────────────────────────────────────
        composable(Routes.BLOCKED_USERS_SETTINGS) {
            BlockedUsersScreen(onBack = { navController.popBackStack() })
        }

        // ── Help & Support ───────────────────────────────────────────────────
        composable(Routes.HELP_SUPPORT_SETTINGS) {
            HelpSupportScreen(onBack = { navController.popBackStack() })
        }

        // ── About ────────────────────────────────────────────────────────────
        composable(Routes.ABOUT_SETTINGS) {
            AboutScreen(
                onBack = { navController.popBackStack() },
                onHomeClick = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = false }
                    }
                },
                onGroupsClick = { navController.navigate(Routes.GROUP_LIST) },
                onEventsClick = { navController.navigate(Routes.EVENT_LIST) },
                onChatClick = { navController.navigate(Routes.CHAT_LIST) },
                onAdminClick = { navController.navigate(Routes.ADMIN_PANEL) },
                onCreatePost = { navController.navigate(Routes.POST_CREATE) },
                onProfileClick = { navController.navigate(Routes.PROFILE) },
            )
        }

        // ── Privacy & Security ───────────────────────────────────────────────
        composable(Routes.PRIVACY_SECURITY_SETTINGS) {
            PrivacySecurityScreen(
                onBack = { navController.popBackStack() },
                // Closest existing flow for password/account actions.
                onChangePassword = { navController.navigate(Routes.ACCOUNT_SETTINGS) },
                // Reuse in-app activity feed screen until dedicated login-history screen exists.
                onLoginActivity = { navController.navigate(Routes.NOTIFICATIONS) },
                // Route data export to support flow for now.
                onDownloadData = { navController.navigate(Routes.HELP_SUPPORT_SETTINGS) },
                // Route history management to account settings for now.
                onSearchHistory = { navController.navigate(Routes.ACCOUNT_SETTINGS) },
            )
        }

        // ── Edit Profile ──────────────────────────────────────────────────────
        composable(Routes.PROFILE_EDIT) {
            EditProfileScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }

        // ── Chat Screen ────────────────────────────────────────────────────────
        composable(
            route     = Routes.CHAT_SCREEN,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType }),
        ) { back ->
            val chatId    = back.arguments?.getString("chatId") ?: return@composable
            ChatScreen(
                chatId    = chatId,
                onBack    = { navController.popBackStack() },
                onOpenProfile = { userId -> navController.navigate(Routes.profilePublic(userId)) },
                onVoiceCallClick = {
                    globalChatViewModel.startOutgoingCall(chatId, "voice") { callId ->
                        navController.navigate(Routes.callScreen(chatId, "voice", callId))
                    }
                },
                onVideoCallClick = {
                    globalChatViewModel.startOutgoingCall(chatId, "video") { callId ->
                        navController.navigate(Routes.callScreen(chatId, "video", callId))
                    }
                },
                onCallAgainClick = { callChatId, callType ->
                    val normalizedCallType = if (callType.equals("audio", ignoreCase = true)) "voice" else callType
                    globalChatViewModel.startOutgoingCall(callChatId, normalizedCallType) { callId ->
                        navController.navigate(Routes.callScreen(callChatId, normalizedCallType, callId))
                    }
                },
                onDiagnosticsClick = { globalChatViewModel.requestDiagnostics() },
                onRotateKeysClick = { globalChatViewModel.rotateKeysForCurrentChat() },
                chatViewModel = globalChatViewModel,
            )
        }

        // ── Call Screens ─────────────────────────────────────────────────────
        composable(
            route = Routes.CALL_SCREEN,
            arguments = listOf(
                navArgument("chatId") { type = NavType.StringType },
                navArgument("callType") { type = NavType.StringType },
                navArgument("callId") { type = NavType.StringType; defaultValue = "" },
            ),
        ) { back ->
            val chatId = back.arguments?.getString("chatId") ?: return@composable
            val callType = back.arguments?.getString("callType") ?: "voice"
            val callId = back.arguments?.getString("callId") ?: ""
            com.example.kampus.ui.chat.CallScreen(
                chatId = chatId,
                callType = callType,
                callId = callId,
                onBack = { navController.popBackStack() },
                viewModel = globalChatViewModel,
            )
        }

        composable(
            route = Routes.INCOMING_CALL_SCREEN,
            arguments = listOf(
                navArgument("chatId") { type = NavType.StringType },
                navArgument("callType") { type = NavType.StringType },
                navArgument("callId") { type = NavType.StringType; defaultValue = "" },
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = "kampus://incoming_call/{chatId}/{callType}?callId={callId}" },
            ),
        ) { back ->
            val chatId = back.arguments?.getString("chatId") ?: return@composable
            val callType = back.arguments?.getString("callType") ?: "voice"
            val callId = back.arguments?.getString("callId") ?: ""
            com.example.kampus.ui.chat.IncomingCallScreen(
                chatId = chatId,
                callType = callType,
                onAccept = {
                    globalChatViewModel.acceptIncomingCall(chatId, callId)
                    globalChatViewModel.consumeIncomingCall()
                    navController.navigate(Routes.callScreen(chatId, callType, callId)) {
                        popUpTo(Routes.INCOMING_CALL_SCREEN) { inclusive = true }
                    }
                },
                onDecline = {
                    globalChatViewModel.declineIncomingCall(chatId, callId)
                    globalChatViewModel.consumeIncomingCall()
                    navController.popBackStack()
                },
                viewModel = globalChatViewModel,
            )
        }

    }
}