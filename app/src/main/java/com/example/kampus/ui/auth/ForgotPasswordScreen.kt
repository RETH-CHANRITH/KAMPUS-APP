package com.example.kampus.ui.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kampus.ui.localization.rememberUiStrings

private val UiIsDark = true
private val FBg      get() = Color(0xFF0A0D14)
private val FCard    get() = Color(0xFF111827)
private val FBorder  get() = Color(0xFF1E2A3A)
private val FBlue    get() = Color(0xFF0D7FFF)
private val FGlow    get() = FBlue.copy(alpha = 0.75f)
private val FWhite   get() = Color(0xFFFFFFFF)
private val FGray5   get() = Color(0xFF6B7280)

@Composable
fun ForgotPasswordScreen(
    onNextClick   : (method: String, contact: String) -> Unit = { _, _ -> },
    onBackClick   : () -> Unit    = {},
    authViewModel : AuthViewModel = viewModel()
) {
    val strings = rememberUiStrings()
    var selectedOption by remember { mutableIntStateOf(0) }
    var emailInput     by remember { mutableStateOf("") }
    var phoneInput     by remember { mutableStateOf("") }
    var inputError     by remember { mutableStateOf("") }

    val authState by authViewModel.authState.collectAsState()
    val isLoading  = authState is AuthState.Loading

    val inf = rememberInfiniteTransition(label = "fg")
    val glow by inf.animateFloat(
        initialValue  = 0.05f, targetValue = 0.12f,
        animationSpec = infiniteRepeatable(tween(2400), RepeatMode.Reverse), label = "g"
    )
    val bounce by inf.animateFloat(
        initialValue  = 0f, targetValue = -8f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "b"
    )

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            val contact = if (selectedOption == 0) emailInput else phoneInput
            val method  = if (selectedOption == 0) "email" else "phone"
            onNextClick(method, contact)
            authViewModel.resetState()
        }
    }

    fun validate(): Boolean {
        return if (selectedOption == 0) {
            if (emailInput.isBlank() || !emailInput.contains("@")) {
                inputError = "Enter a valid email address"; false
            } else { inputError = ""; true }
        } else {
            if (phoneInput.isBlank() || phoneInput.length < 8) {
                inputError = "Enter a valid phone number"; false
            } else { inputError = ""; true }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(FBg)
    ) {
        // Ambient glow
        Box(
            modifier = Modifier
                .fillMaxWidth().height(280.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.radialGradient(
                        listOf(FBlue.copy(alpha = glow), Color.Transparent), radius = 600f
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

            // Top row
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                AuthBackButton(onClick = onBackClick)
                Box(
                    modifier         = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(FBlue, FGlow),
                                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                end   = androidx.compose.ui.geometry.Offset(80f, 80f)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) { Text("🎓", fontSize = 20.sp) }
            }

            Spacer(Modifier.height(32.dp))

            // Bouncing lock icon
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .graphicsLayer { translationY = bounce }
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(FBlue.copy(alpha = 0.18f), FCard)))
                    .border(1.dp, FBlue.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) { Text("🔐", fontSize = 34.sp) }

            Spacer(Modifier.height(20.dp))

            Text(
                "Forgot Password?",
                color = FWhite, fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Select how you'd like to receive\na verification code",
                color = FGray5, fontSize = 14.sp, lineHeight = 22.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            // Option cards
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FPOptionCard(
                    emoji = "📧", title = "Email Address",
                    subtitle = "Send code to your email",
                    isSelected = selectedOption == 0,
                    onClick = { selectedOption = 0; inputError = "" }
                )
                FPOptionCard(
                    emoji = "📱", title = "Phone Number",
                    subtitle = "Send code via SMS",
                    isSelected = selectedOption == 1,
                    onClick = { selectedOption = 1; inputError = "" }
                )
            }

            Spacer(Modifier.height(24.dp))

            // Input field — switches with animation
            AnimatedContent(
                targetState  = selectedOption,
                transitionSpec = {
                    (fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 3 })
                        .togetherWith(fadeOut(tween(150)))
                },
                label = "input_anim"
            ) { option ->
                if (option == 0) {
                    PremiumInputField(
                        value         = emailInput,
                        onValueChange = { emailInput = it; inputError = "" },
                        label         = "Email Address",
                        placeholder   = "Enter your email address",
                        icon          = Icons.Outlined.Email,
                        error         = inputError,
                        keyboardType  = KeyboardType.Email
                    )
                } else {
                    PremiumInputField(
                        value         = phoneInput,
                        onValueChange = { phoneInput = it; inputError = "" },
                        label         = "Phone Number",
                        placeholder   = "+1 (555) 000-0000",
                        icon          = Icons.Outlined.Phone,
                        error         = inputError,
                        keyboardType  = KeyboardType.Phone
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            PremiumButton(
                text      = "Send Verification Code",
                isLoading = isLoading,
                onClick   = {
                    if (validate()) {
                        val contact = if (selectedOption == 0) emailInput else phoneInput
                        authViewModel.sendOtp(contact)
                    }
                }
            )

            Spacer(Modifier.height(16.dp))

            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.Info, null,
                    tint = FGray5.copy(alpha = 0.6f), modifier = Modifier.size(13.dp)
                )
                Text(strings.youWillReceiveCodeShortly, color = FGray5, fontSize = 12.sp)
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun FPOptionCard(
    modifier   : Modifier = Modifier,
    emoji      : String,
    title      : String,
    subtitle   : String,
    isSelected : Boolean,
    onClick    : () -> Unit
) {
    val borderColor by animateColorAsState(
        if (isSelected) FBlue else FBorder, tween(200), label = "bc"
    )
    val bgColor by animateColorAsState(
        if (isSelected) FBlue.copy(alpha = 0.08f) else FCard, tween(200), label = "bg"
    )
    val iconBg by animateColorAsState(
        if (isSelected) FBlue else Color(0xFF1E2A3A), tween(200), label = "ib"
    )
    val scale by animateFloatAsState(
        if (isSelected) 1.02f else 1f,
        spring(stiffness = Spring.StiffnessMedium), label = "sc"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(18.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick
            )
            .padding(16.dp)
    ) {
        Box(
            modifier         = Modifier.size(52.dp).clip(CircleShape).background(iconBg),
            contentAlignment = Alignment.Center
        ) { Text(emoji, fontSize = 22.sp) }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = FWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = FGray5, fontSize = 12.sp)
        }

        AnimatedVisibility(
            visible = isSelected,
            enter   = scaleIn() + fadeIn(),
            exit    = scaleOut() + fadeOut()
        ) {
            Box(
                modifier         = Modifier.size(26.dp).clip(CircleShape).background(FBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, null, tint = FWhite, modifier = Modifier.size(15.dp))
            }
        }
    }
}