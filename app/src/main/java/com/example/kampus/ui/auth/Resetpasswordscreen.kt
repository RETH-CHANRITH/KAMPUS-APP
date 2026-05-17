package com.example.kampus.ui.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kampus.ui.localization.rememberUiStrings

private val UiIsDark = true
private val RPBg     get() = Color(0xFF0A0D14)
private val RPCard   get() = Color(0xFF111827)
private val RPBorder get() = Color(0xFF1E2A3A)
private val RPBlue   get() = Color(0xFF0D7FFF)
private val RPGlow   get() = RPBlue.copy(alpha = 0.75f)
private val RPWhite  get() = Color(0xFFFFFFFF)
private val RPGray5  get() = Color(0xFF6B7280)
private val RPGray6  get() = Color(0xFF9CA3AF)
private val RPOk     get() = Color(0xFF22C55E)
private val RPErr    get() = Color(0xFFFF4D6A)
private val RPYellow get() = Color(0xFFFACC15)

@Composable
fun ResetPasswordScreen(
    modifier       : Modifier = Modifier,
    onResetSuccess : () -> Unit = {},
    onBackClick    : () -> Unit = {}
) {
    val strings = rememberUiStrings()
    var newPass     by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var passVis     by remember { mutableStateOf(false) }
    var confVis     by remember { mutableStateOf(false) }
    var passErr     by remember { mutableStateOf("") }
    var confErr     by remember { mutableStateOf("") }
    var isDone      by remember { mutableStateOf(false) }
    val focus        = LocalFocusManager.current

    val inf = rememberInfiniteTransition(label = "rp")
    val glow by inf.animateFloat(
        initialValue  = 0.05f, targetValue = 0.13f,
        animationSpec = infiniteRepeatable(tween(2200), RepeatMode.Reverse), label = "g"
    )
    val bounce by inf.animateFloat(
        initialValue  = 0f, targetValue = -8f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "b"
    )

    val strength = when {
        newPass.length >= 10 && newPass.any { it.isUpperCase() } && newPass.any { it.isDigit() } -> 3
        newPass.length >= 8 -> 2
        newPass.isNotEmpty() -> 1
        else -> 0
    }
    val strengthColor = when (strength) { 3 -> RPOk; 2 -> RPYellow; 1 -> RPErr; else -> RPGray6 }
    val strengthLabel = when (strength) { 3 -> "Strong 💪"; 2 -> "Medium"; 1 -> "Weak"; else -> "" }

    fun validate(): Boolean {
        var ok = true
        passErr = when {
            newPass.isBlank()  -> { ok = false; "Password is required" }
            newPass.length < 6 -> { ok = false; "At least 6 characters" }
            else -> ""
        }
        confErr = when {
            confirmPass.isBlank()    -> { ok = false; "Please confirm password" }
            confirmPass != newPass   -> { ok = false; "Passwords do not match" }
            else -> ""
        }
        return ok
    }

    if (isDone) {
        RPSuccessScreen(modifier = modifier, onContinue = onResetSuccess, strings = strings)
        return
    }

    Box(modifier = modifier.fillMaxSize().background(RPBg)) {

        Box(
            modifier = Modifier
                .fillMaxWidth().height(260.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.radialGradient(
                        listOf(RPBlue.copy(alpha = glow), Color.Transparent), radius = 600f
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

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                AuthBackButton(onClick = onBackClick)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(RPBlue, RPGlow),
                                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                end   = androidx.compose.ui.geometry.Offset(80f, 80f)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) { Text("🎓", fontSize = 20.sp) }
            }

            Spacer(Modifier.height(32.dp))

            // Bouncing key icon
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .graphicsLayer { translationY = bounce }
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(RPBlue.copy(alpha = 0.18f), RPCard)))
                    .border(1.dp, RPBlue.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) { Text("🔑", fontSize = 34.sp) }

            Spacer(Modifier.height(20.dp))

            Text(
                "New Password",
                color = RPWhite, fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Create a strong new password\nfor your account",
                color = RPGray5, fontSize = 14.sp, lineHeight = 22.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(28.dp))

            // Form card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(RPCard)
                    .border(1.dp, RPBorder, RoundedCornerShape(24.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // New password + strength
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PremiumInputField(
                        value            = newPass,
                        onValueChange    = { newPass = it; passErr = "" },
                        label            = "New Password",
                        placeholder      = "Create a strong password",
                        icon             = Icons.Outlined.Lock,
                        error            = passErr,
                        isPassword       = true,
                        passwordVisible  = passVis,
                        onTogglePassword = { passVis = !passVis },
                        imeAction        = ImeAction.Next
                    )
                    AnimatedVisibility(visible = newPass.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                repeat(3) { i ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f).height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(
                                                if (i < strength) strengthColor
                                                else RPGray6.copy(alpha = 0.25f)
                                            )
                                    )
                                }
                            }
                            if (strengthLabel.isNotEmpty()) {
                                Text(
                                    "Strength: $strengthLabel",
                                    color = strengthColor, fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Confirm password
                PremiumInputField(
                    value            = confirmPass,
                    onValueChange    = { confirmPass = it; confErr = "" },
                    label            = "Confirm Password",
                    placeholder      = "Re-enter your new password",
                    icon             = Icons.Outlined.Lock,
                    error            = confErr,
                    isPassword       = true,
                    passwordVisible  = confVis,
                    onTogglePassword = { confVis = !confVis },
                    imeAction        = ImeAction.Done,
                    onDone           = { focus.clearFocus() }
                )

                // Requirements checklist
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Requirements:",
                        color = RPGray5, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                    )
                    PasswordRequirementRow("At least 6 characters",   newPass.length >= 6)
                    PasswordRequirementRow("Uppercase letter",         newPass.any { it.isUpperCase() })
                    PasswordRequirementRow("Contains a number",        newPass.any { it.isDigit() })
                    PasswordRequirementRow("Passwords match",          newPass.isNotEmpty() && newPass == confirmPass)
                }
            }

            Spacer(Modifier.height(24.dp))

            PremiumButton(
                text    = "Reset Password",
                onClick = {
                    if (validate()) { focus.clearFocus(); isDone = true }
                }
            )

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun PasswordRequirementRow(text: String, met: Boolean) {
    val color by animateColorAsState(
        targetValue   = if (met) RPOk else RPGray6,
        animationSpec = tween(300),
        label         = "req_color"
    )
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector        = if (met) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint               = color,
            modifier           = Modifier.size(14.dp)
        )
        Text(text, color = color, fontSize = 12.sp)
    }
}

@Composable
private fun RPSuccessScreen(
    modifier   : Modifier = Modifier,
    onContinue : () -> Unit,
    strings    : com.example.kampus.ui.localization.UiStrings,
) {
    val inf = rememberInfiniteTransition(label = "succ")
    val pulse by inf.animateFloat(
        initialValue  = 1f, targetValue = 1.09f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "p"
    )
    val glowA by inf.animateFloat(
        initialValue  = 0.15f, targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
        label = "ga"
    )

    Box(
        modifier         = modifier.fillMaxSize().background(RPBg),
        contentAlignment = Alignment.Center
    ) {
        // Green ambient glow
        Box(
            modifier = Modifier
                .size(300.dp)
                .blur(60.dp)
                .background(
                    Brush.radialGradient(listOf(RPOk.copy(alpha = glowA), Color.Transparent))
                )
        )

        Column(
            modifier            = Modifier.padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .graphicsLayer { scaleX = pulse; scaleY = pulse }
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(RPOk.copy(alpha = 0.22f), RPCard)))
                    .border(2.dp, RPOk.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) { Text("✅", fontSize = 48.sp) }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(strings.allDone, color = RPWhite, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    strings.passwordResetSuccessfully,
                    color = RPOk, fontSize = 15.sp, fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${strings.yourPasswordHasBeenChanged}\n${strings.loginWithNewPassword}",
                    color = RPGray5, fontSize = 14.sp,
                    lineHeight = 22.sp, textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(4.dp))
            PremiumButton(text = "Back to Login", onClick = onContinue)
        }
    }
}
