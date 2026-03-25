package com.example.campussocial.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")
    object Otp : Screen("otp/{email}") {
        fun createRoute(email: String) = "otp/$email"
    }
    object ResetPassword : Screen("reset_password")
    object Feed : Screen("feed")
    object Groups : Screen("groups")
    object Events : Screen("events")
    object Profile : Screen("profile")
    object Chat : Screen("chat")
    object Notifications : Screen("notifications")
    object Admin : Screen("admin")
}