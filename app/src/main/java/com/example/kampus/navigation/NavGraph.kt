package com.example.kampus.navigation

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
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
import com.example.kampus.ui.groups.CreateGroupScreen
import com.example.kampus.ui.groups.GroupDetailScreen
import com.example.kampus.ui.groups.GroupListScreen
import com.example.kampus.ui.groups.GroupViewModel
import com.example.kampus.ui.events.EventDetailScreen
import com.example.kampus.ui.events.EventListScreen
import com.example.kampus.ui.events.EventViewModel
import com.example.kampus.ui.profile.AccountSettingsScreen
import com.example.kampus.ui.profile.AboutScreen
import com.example.kampus.ui.profile.AppearanceSettingsScreen
import com.example.kampus.ui.profile.BlockedUsersScreen
import com.example.kampus.ui.profile.EditProfileScreen
import com.example.kampus.ui.profile.DiscoverPeopleScreen
import com.example.kampus.ui.profile.FriendRequestsScreen
import com.example.kampus.ui.profile.FriendsScreen
import com.example.kampus.ui.profile.HelpSupportScreen
import com.example.kampus.ui.profile.NotificationSettingsScreen
import com.example.kampus.ui.profile.PrivacySecurityScreen
import com.example.kampus.ui.profile.ProfileScreen
import com.example.kampus.ui.profile.SettingsScreen
import com.google.firebase.auth.FirebaseAuth

object Routes {
    const val SPLASH          = "splash"
    const val ONBOARDING      = "onboarding"
    const val LOGIN           = "login"
    const val REGISTER        = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val OTP             = "otp/{method}/{contact}"
    const val RESET_PASSWORD  = "reset_password"
    const val HOME            = "home"

    // Feed
    const val POST_CREATE     = "post_create"

    // Groups
    const val GROUP_LIST   = "group_list"
    const val GROUP_DETAIL = "group_detail/{groupId}"
    const val GROUP_CREATE = "group_create"

    // Events
    const val EVENT_LIST   = "event_list"
    const val EVENT_DETAIL = "event_detail/{eventId}"
    const val EVENT_CREATE = "event_create"

    // Chat
    const val CHAT_LIST   = "chat_list"
    const val CHAT_SCREEN = "chat_screen/{chatId}"

    // Profile
    const val PROFILE      = "profile"
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
    const val PROFILE_EDIT = "profile_edit"

    fun groupDetail(groupId: Int) = "group_detail/$groupId"
    fun eventDetail(eventId: Int) = "event_detail/$eventId"
    fun chatScreen(chatId: Int)   = "chat_screen/$chatId"

    fun otp(method: String, contact: String) =
        "otp/$method/${contact.encodeUrl()}"

    private fun String.encodeUrl() =
        java.net.URLEncoder.encode(this, "UTF-8")
}

@SuppressLint("StateFlowValueCalledInComposition", "UnrememberedGetBackStackEntry")
@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.SPLASH) {

        // ── Splash ─────────────────────────────────────────────────────────────
        composable(Routes.SPLASH) {
            SplashScreen(
                onNavigateToOnboarding = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
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
                    navController.navigate(if (isLoggedIn) Routes.HOME else Routes.LOGIN) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        // ── Login ──────────────────────────────────────────────────────────────
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess   = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onRegisterClick  = { navController.navigate(Routes.REGISTER) },
                onForgotPassword = { navController.navigate(Routes.FORGOT_PASSWORD) },
                onGoogleClick    = { },
                onAppleClick     = { },
                onBackClick      = { navController.popBackStack() }
            )
        }

        // ── Register ───────────────────────────────────────────────────────────
        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = { },
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
            HomeScreen(
                onCreatePost   = { navController.navigate(Routes.POST_CREATE) },
                onProfileClick = { navController.navigate(Routes.PROFILE) },
                onNotifClick   = { },
                onSearchClick  = { },
                onGroupsClick  = { navController.navigate(Routes.GROUP_LIST) },
                onEventsClick  = { navController.navigate(Routes.EVENT_LIST) },
                onChatClick    = { navController.navigate(Routes.CHAT_LIST) },
            )
        }

        // ── Create Post ───────────────────────────────────────────────────────
        composable(Routes.POST_CREATE) {
            val homeEntry = remember { navController.getBackStackEntry(Routes.HOME) }
            val feedViewModel: FeedViewModel = viewModel(homeEntry)
            CreatePostScreen(
                onClose = { navController.popBackStack() },
                onPost = { text, mediaUri, mediaType, visibility, allowComments, taggedPeople, feelingEmoji, location ->
                    feedViewModel.addPost(
                        text = text,
                        imageUri = mediaUri,
                        mediaType = mediaType,
                        feeling = null,
                        location = location,
                        visibility = visibility,
                        allowComments = allowComments,
                        tags = emptyList(),
                        taggedPeople = taggedPeople,
                        feelingEmoji = feelingEmoji,
                    )
                    navController.popBackStack()
                },
            )
        }

        // ── Group List ─────────────────────────────────────────────────────────
        composable(Routes.GROUP_LIST) {
            val vm: GroupViewModel = viewModel()
            GroupListScreen(
                viewModel          = vm,
                onGroupClick       = { navController.navigate(Routes.groupDetail(it.id)) },
                onCreateGroupClick = { navController.navigate(Routes.GROUP_CREATE) },
                onHomeClick        = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = false }
                    }
                },
                onEventsClick  = { navController.navigate(Routes.EVENT_LIST) },
                onChatClick    = { navController.navigate(Routes.CHAT_LIST) },
                onFabClick     = { },
                onProfileClick = { navController.navigate(Routes.PROFILE) },
            )
        }

        // ── Group Detail ───────────────────────────────────────────────────────
        composable(
            route     = Routes.GROUP_DETAIL,
            arguments = listOf(navArgument("groupId") { type = NavType.IntType })
        ) { back ->
            val groupId   = back.arguments?.getInt("groupId") ?: return@composable
            val listEntry = remember(back) { navController.getBackStackEntry(Routes.GROUP_LIST) }
            val vm: GroupViewModel = viewModel(listEntry)
            val state = vm.uiState.value
            val group = (state.myGroups + state.discoverGroups)
                .firstOrNull { it.id == groupId } ?: return@composable
            GroupDetailScreen(
                group     = group,
                viewModel = vm,
                onBack    = { navController.popBackStack() },
            )
        }

        // ── Group Create ───────────────────────────────────────────────────────
        composable(Routes.GROUP_CREATE) {
            CreateGroupScreen(
                onBack   = { navController.popBackStack() },
                onCreate = { navController.popBackStack() },
            )
        }

        // ── Event List ─────────────────────────────────────────────────────────
        composable(Routes.EVENT_LIST) {
            val vm: EventViewModel = viewModel()
            EventListScreen(
                viewModel      = vm,
                onEventClick   = { navController.navigate(Routes.eventDetail(it.id)) },
                onCreateClick  = { navController.navigate(Routes.EVENT_CREATE) },
                onHomeClick    = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = false }
                    }
                },
                onGroupsClick  = { navController.navigate(Routes.GROUP_LIST) },
                onChatClick    = { navController.navigate(Routes.CHAT_LIST) },
                onFabClick     = { navController.navigate(Routes.EVENT_CREATE) },
                onProfileClick = { navController.navigate(Routes.PROFILE) },
            )
        }

        // ── Event Detail ───────────────────────────────────────────────────────
        composable(
            route     = Routes.EVENT_DETAIL,
            arguments = listOf(navArgument("eventId") { type = NavType.IntType })
        ) { back ->
            val eventId   = back.arguments?.getInt("eventId") ?: return@composable
            val listEntry = remember(back) { navController.getBackStackEntry(Routes.EVENT_LIST) }
            val vm: EventViewModel = viewModel(listEntry)
            val state = vm.uiState.value
            val event = state.events.firstOrNull { it.id == eventId } ?: return@composable
            EventDetailScreen(
                event        = event,
                isInterested = event.id in state.interestedIds,
                isLiked      = event.id in state.likedIds,
                isSaved      = event.id in state.savedIds,
                onInterested = { vm.toggleInterested(event.id) },
                onLike       = { vm.toggleLike(event.id) },
                onSave       = { vm.toggleSave(event.id) },
                onBack       = { navController.popBackStack() },
            )
        }

        // ── Event Create ───────────────────────────────────────────────────────
        composable(Routes.EVENT_CREATE) { backStackEntry ->
            val listEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Routes.EVENT_LIST)
            }
            val vm: EventViewModel = viewModel(listEntry)
            val handleBack: () -> Unit    = { navController.popBackStack() }
            val handlePublish: (NewEventData) -> Unit = { data ->
                vm.addEvent(data)
                navController.popBackStack()
            }
            CreateEventScreen(
                onBack    = handleBack,
                onPublish = handlePublish,
            )
        }

        // ── Chat List ──────────────────────────────────────────────────────────
        composable(Routes.CHAT_LIST) {
            val vm: ChatViewModel = viewModel()
            ChatListScreen(
                viewModel      = vm,
                onChatClick    = { chatId -> navController.navigate(Routes.chatScreen(chatId)) },
                onHomeClick    = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = false }
                    }
                },
                onGroupsClick  = { navController.navigate(Routes.GROUP_LIST) },
                onEventsClick  = { navController.navigate(Routes.EVENT_LIST) },
                onProfileClick = { navController.navigate(Routes.PROFILE) },
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
                onCreatePost = { navController.navigate(Routes.POST_CREATE) },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
            )
        }

        // ── Friend Requests ───────────────────────────────────────────────────
        composable(Routes.FRIEND_REQUESTS) {
            FriendRequestsScreen(onBack = { navController.popBackStack() })
        }

        // ── Friends ───────────────────────────────────────────────────────────
        composable(Routes.FRIENDS) {
            FriendsScreen(onBack = { navController.popBackStack() })
        }

        // ── Discover People ───────────────────────────────────────────────────
        composable(Routes.DISCOVER_PEOPLE) {
            DiscoverPeopleScreen(onBack = { navController.popBackStack() })
        }

        // ── Settings ──────────────────────────────────────────────────────────
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenAccountSettings = { navController.navigate(Routes.ACCOUNT_SETTINGS) },
                onOpenNotificationSettings = { navController.navigate(Routes.NOTIFICATION_SETTINGS) },
                onOpenPrivacySecurity = { navController.navigate(Routes.PRIVACY_SECURITY_SETTINGS) },
                onOpenAppearance = { navController.navigate(Routes.APPEARANCE_SETTINGS) },
                onOpenBlockedUsers = { navController.navigate(Routes.BLOCKED_USERS_SETTINGS) },
                onOpenHelpSupport = { navController.navigate(Routes.HELP_SUPPORT_SETTINGS) },
                onOpenAbout = { navController.navigate(Routes.ABOUT_SETTINGS) },
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
                onCreatePost = { navController.navigate(Routes.POST_CREATE) },
                onProfileClick = { navController.navigate(Routes.PROFILE) },
            )
        }

        // ── Privacy & Security ───────────────────────────────────────────────
        composable(Routes.PRIVACY_SECURITY_SETTINGS) {
            PrivacySecurityScreen(onBack = { navController.popBackStack() })
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
            arguments = listOf(navArgument("chatId") { type = NavType.IntType }),
        ) { back ->
            val chatId    = back.arguments?.getInt("chatId") ?: return@composable
            val listEntry = remember(back) { navController.getBackStackEntry(Routes.CHAT_LIST) }
            val vm: ChatViewModel = viewModel(listEntry)
            ChatScreen(
                chatId    = chatId,
                onBack    = { navController.popBackStack() },
                viewModel = vm,
            )
        }

    }
}