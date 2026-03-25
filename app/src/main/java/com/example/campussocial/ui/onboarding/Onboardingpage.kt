package com.example.campussocial.ui.onboarding

import androidx.annotation.DrawableRes
import com.example.kampus.R

data class OnboardingPage(
    val title       : String,
    val description : String,
    @get:DrawableRes val imageRes : Int
)

val onboardingPages = listOf(
    OnboardingPage(
        title       = "Welcome To The Fun\nMagic Media",
        description = "Dummy text is also used to demonstrate\nthe appearance of different typefaces\nand layouts",
        imageRes    = R.drawable.pic
    ),
    OnboardingPage(
        title       = "Best Social App To\nMake New Friends",
        description = "Dummy text is also used to demonstrate\nthe appearance of different typefaces\nand layouts",
        imageRes    = R.drawable.pic2
    ),
    OnboardingPage(
        title       = "Enjoy Your Life\nEvery Time",
        description = "Dummy text is also used to demonstrate\nthe appearance of different typefaces\nand layouts",
        imageRes    = R.drawable.pic3
    )
)
