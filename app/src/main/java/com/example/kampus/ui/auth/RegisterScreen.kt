package com.example.kampus.ui.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kampus.R
import com.example.kampus.ui.localization.rememberUiStrings

// ── Design tokens (same as LoginScreen) ───────────────────────────────────────
private val UiIsDark      = true
private val RegBg         get() = Color(0xFF0A0D14)
private val RegCard       get() = Color(0xFF111827)
private val RegCardBorder get() = Color(0xFF1E2A3A)
private val RegBlue       get() = Color(0xFF0D7FFF)
private val RegBlueGlow   get() = RegBlue.copy(alpha = 0.75f)
private val RegFieldBg    get() = Color(0xFF0F1623)
private val RegWhite      get() = Color(0xFFFFFFFF)
private val RegGray300    get() = Color(0xFFD1D5DB)
private val RegGray500    get() = Color(0xFF6B7280)
private val RegGray600    get() = Color(0xFF9CA3AF)
private val RegErrorRed   get() = Color(0xFFFF4D6A)

@Composable
fun RegisterScreen(
    onRegisterSuccess : (String) -> Unit = {},
    onLoginClick      : () -> Unit = {},
    onGoogleClick     : () -> Unit = {},
    onAppleClick      : () -> Unit = {},
    onBackClick       : () -> Unit = {},
    authViewModel     : AuthViewModel = viewModel()
) {
    val strings = rememberUiStrings()
    var name            by remember { mutableStateOf("") }
    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passVisible     by remember { mutableStateOf(false) }
    var confirmVisible  by remember { mutableStateOf(false) }
    var nameError       by remember { mutableStateOf("") }
    var emailError      by remember { mutableStateOf("") }
    var passwordError   by remember { mutableStateOf("") }
    var confirmError    by remember { mutableStateOf("") }
    var agreedTerms     by remember { mutableStateOf(false) }

    val authState    by authViewModel.authState.collectAsState()
    val isLoading     = authState is AuthState.Loading
    val focusManager  = LocalFocusManager.current

    // Ambient glow
    val inf = rememberInfiniteTransition(label = "reg_glow")
    val glowAlpha by inf.animateFloat(
        initialValue  = 0.05f, targetValue = 0.12f,
        animationSpec = infiniteRepeatable(tween(2600), RepeatMode.Reverse),
        label = "glow"
    )

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) onRegisterSuccess(email.trim())
    }

    fun validate(): Boolean {
        var ok = true
        nameError = if (name.isBlank()) { ok = false; "Name is required" } else ""
        emailError = when {
            email.isBlank()      -> { ok = false; "Email is required" }
            !email.contains("@") -> { ok = false; "Enter a valid email" }
            !email.endsWith("@rupp.edu.kh") -> { ok = false; "Only RUPP emails (@rupp.edu.kh) allowed" }
            else -> ""
        }
        passwordError = when {
            password.isBlank()  -> { ok = false; "Password is required" }
            password.length < 6 -> { ok = false; "At least 6 characters" }
            else -> ""
        }
        confirmError = when {
            confirmPassword.isBlank()   -> { ok = false; "Please confirm password" }
            confirmPassword != password -> { ok = false; "Passwords do not match" }
            else -> ""
        }
        if (!agreedTerms) ok = false
        return ok
    }

    // Password strength
    val strength = when {
        password.length >= 10 && password.any { it.isUpperCase() } &&
                password.any { it.isDigit() } -> 3
        password.length >= 8 -> 2
        password.isNotEmpty() -> 1
        else -> 0
    }
    val strengthColor = when (strength) {
        3    -> Color(0xFF22C55E)
        2    -> Color(0xFFFACC15)
        1    -> RegErrorRed
        else -> RegGray600
    }
    val strengthLabel = when (strength) {
        3    -> "Strong"
        2    -> "Medium"
        1    -> "Weak"
        else -> ""
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RegBg)
    ) {
        // Ambient glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.radialGradient(
                        colors = listOf(RegBlue.copy(alpha = glowAlpha), Color.Transparent),
                        radius = 600f
                    )
                )
                .blur(40.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            // ── Top row ────────────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                AuthBackButton(onClick = onBackClick)

                Box(
                    contentAlignment = Alignment.Center,
                    modifier         = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(RegBlue, RegBlueGlow),
                                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                end   = androidx.compose.ui.geometry.Offset(80f, 80f)
                            )
                        )
                ) {
                    Text("🎓", fontSize = 20.sp)
                }
            }

            Spacer(Modifier.height(36.dp))

            // ── Header ─────────────────────────────────────────────────────────
            Column(
                modifier            = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    "Join us today ✨",
                    color      = RegGray500,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Create your\nAccount",
                    color         = RegWhite,
                    fontSize      = 32.sp,
                    fontWeight    = FontWeight.ExtraBold,
                    lineHeight    = 42.sp,
                    letterSpacing = (-0.5).sp
                )
            }

            Spacer(Modifier.height(28.dp))

            // ── Form card ──────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(RegCard)
                    .border(1.dp, RegCardBorder, RoundedCornerShape(24.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Full Name
                PremiumInputField(
                    value         = name,
                    onValueChange = { name = it; nameError = ""; authViewModel.resetState() },
                    label         = "Full Name",
                    placeholder   = "Your full name",
                    icon          = Icons.Outlined.Person,
                    error         = nameError,
                    imeAction     = ImeAction.Next
                )

                // Email
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PremiumInputField(
                        value         = email,
                        onValueChange = { email = it; emailError = ""; authViewModel.resetState() },
                        label         = "Email Address",
                        placeholder   = "reth.chanrith.2823@rupp.edu.kh",
                        icon          = Icons.Outlined.Email,
                        error         = emailError,
                        keyboardType  = KeyboardType.Email,
                        imeAction     = ImeAction.Next
                    )
                    // RUPP email hint
                    Text(
                        text = "🎓 Use your RUPP University email (@rupp.edu.kh)",
                        fontSize = 12.sp,
                        color = RegGray500,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                // Password + strength bar
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PremiumInputField(
                        value            = password,
                        onValueChange    = { password = it; passwordError = ""; authViewModel.resetState() },
                        label            = "Password",
                        placeholder      = "Create a strong password",
                        icon             = Icons.Outlined.Lock,
                        error            = passwordError,
                        isPassword       = true,
                        passwordVisible  = passVisible,
                        onTogglePassword = { passVisible = !passVisible },
                        imeAction        = ImeAction.Next
                    )
                    // Strength indicator
                    AnimatedVisibility(visible = password.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                repeat(3) { i ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(3.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(
                                                if (i < strength) strengthColor
                                                else RegGray600.copy(alpha = 0.3f)
                                            )
                                    )
                                }
                            }
                            if (strengthLabel.isNotEmpty()) {
                                Text(
                                    "Password strength: $strengthLabel",
                                    color    = strengthColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Confirm Password
                PremiumInputField(
                    value            = confirmPassword,
                    onValueChange    = { confirmPassword = it; confirmError = ""; authViewModel.resetState() },
                    label            = "Confirm Password",
                    placeholder      = "Re-enter your password",
                    icon             = Icons.Outlined.Lock,
                    error            = confirmError,
                    isPassword       = true,
                    passwordVisible  = confirmVisible,
                    onTogglePassword = { confirmVisible = !confirmVisible },
                    imeAction        = ImeAction.Done,
                    onDone           = { focusManager.clearFocus() }
                )

                // Terms checkbox
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier              = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(if (agreedTerms) RegBlue else RegFieldBg)
                            .border(
                                1.dp,
                                if (agreedTerms) RegBlue else RegCardBorder,
                                RoundedCornerShape(5.dp)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication        = null
                            ) { agreedTerms = !agreedTerms }
                    ) {
                        if (agreedTerms) {
                            Icon(Icons.Default.Check, null,
                                tint = RegWhite, modifier = Modifier.size(13.dp))
                        }
                    }
                    Row {
                        Text("I agree to the ", color = RegGray500, fontSize = 13.sp)
                        Text(
                            strings.termsOfService,
                            color      = RegBlue,
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(" & ", color = RegGray500, fontSize = 13.sp)
                        Text(
                            strings.privacyPolicy,
                            color      = RegBlue,
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Server error
                AnimatedVisibility(visible = authState is AuthState.Error) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(RegErrorRed.copy(alpha = 0.1f))
                            .padding(10.dp)
                    ) {
                        Icon(Icons.Default.ErrorOutline, null,
                            tint = RegErrorRed, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            (authState as? AuthState.Error)?.message ?: "",
                            color    = RegErrorRed,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Register button ────────────────────────────────────────────────
            PremiumButton(
                text      = strings.createAccount,
                isLoading = isLoading,
                onClick   = {
                    if (validate()) {
                        focusManager.clearFocus()
                        authViewModel.register(name, email, password)
                    }
                }
            )

            Spacer(Modifier.height(20.dp))

            // ── Sign in link ───────────────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("Already have an account? ", color = RegGray500, fontSize = 14.sp)
                Text(
                    strings.signIn,
                    color      = RegBlue,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.clickable(onClick = onLoginClick)
                )
            }

            Spacer(Modifier.height(28.dp))

            // ── Divider ────────────────────────────────────────────────────────
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier              = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider(Modifier.weight(1f), color = RegCardBorder)
                Text(strings.orContinueWith, color = RegGray600, fontSize = 12.sp)
                HorizontalDivider(Modifier.weight(1f), color = RegCardBorder)
            }

            Spacer(Modifier.height(16.dp))

            // ── Social buttons ─────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PremiumSocialButton(
                    name     = "Google",
                    logoRes  = R.drawable.logo_google,
                    onClick  = onGoogleClick,
                    modifier = Modifier.weight(1f)
                )
                PremiumSocialButton(
                    name     = "Apple",
                    logoRes  = R.drawable.logo_apple,
                    onClick  = onAppleClick,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}
