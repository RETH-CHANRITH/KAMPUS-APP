package com.example.campussocial.ui.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

private val OBg     = Color(0xFF0A0D14)
private val OCard   = Color(0xFF111827)
private val OBorder = Color(0xFF1E2A3A)
private val OBlue   = Color(0xFF3B82F6)
private val OGlow   = Color(0xFF2563EB)
private val OFBg    = Color(0xFF0F1623)
private val OFBrd   = Color(0xFF1E2A3A)
private val OWhite  = Color(0xFFFFFFFF)
private val OGray3  = Color(0xFFD1D5DB)
private val OGray5  = Color(0xFF6B7280)
private val OGray6  = Color(0xFF4B5563)
private val OErr    = Color(0xFFFF4D6A)
private val OOk     = Color(0xFF22C55E)

@Composable
fun OtpScreen(
    modifier        : Modifier      = Modifier,
    onVerifySuccess : () -> Unit    = {},
    onBackClick     : () -> Unit    = {},
    method          : String        = "email",
    contact         : String        = "",
    email           : String        = "",
    authViewModel   : AuthViewModel = viewModel()
) {
    val displayContact = contact.ifBlank { email }
    val isEmail        = method == "email" || displayContact.contains("@")
    val channelIcon    = if (isEmail) "📧" else "📱"
    val channelLabel   = if (isEmail) "email" else "phone number"

    var d1 by remember { mutableStateOf("") }
    var d2 by remember { mutableStateOf("") }
    var d3 by remember { mutableStateOf("") }
    var d4 by remember { mutableStateOf("") }
    var d5 by remember { mutableStateOf("") }
    var d6 by remember { mutableStateOf("") }
    var resendTimer by remember { mutableIntStateOf(60) }

    val authState by authViewModel.authState.collectAsState()
    val isLoading  = authState is AuthState.Loading
    val focusReqs  = remember { List(6) { FocusRequester() } }

    val inf = rememberInfiniteTransition(label = "otp")
    val glow by inf.animateFloat(
        initialValue  = 0.05f, targetValue = 0.13f,
        animationSpec = infiniteRepeatable(tween(2200), RepeatMode.Reverse), label = "g"
    )
    val bounce by inf.animateFloat(
        initialValue  = 0f, targetValue = -8f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "b"
    )

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) onVerifySuccess()
    }
    LaunchedEffect(Unit) {
        while (resendTimer > 0) { kotlinx.coroutines.delay(1000); resendTimer-- }
    }

    val digits  = listOf(d1, d2, d3, d4, d5, d6)
    val setters = listOf<(String) -> Unit>(
        { d1 = it }, { d2 = it }, { d3 = it }, { d4 = it }, { d5 = it }, { d6 = it }
    )
    val allFilled = digits.all { it.isNotEmpty() }

    Box(modifier = modifier.fillMaxSize().background(OBg)) {
        Box(
            modifier = Modifier
                .fillMaxWidth().height(260.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.radialGradient(
                        listOf(OBlue.copy(alpha = glow), Color.Transparent), radius = 600f
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
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(OBlue, OGlow),
                                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                end   = androidx.compose.ui.geometry.Offset(80f, 80f)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) { Text("🎓", fontSize = 20.sp) }
            }

            Spacer(Modifier.height(28.dp))

            // Channel icon bouncing (📧 or 📱)
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .graphicsLayer { translationY = bounce }
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(OBlue.copy(alpha = 0.20f), OCard)))
                    .border(1.dp, OBlue.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) { Text(channelIcon, fontSize = 38.sp) }

            Spacer(Modifier.height(20.dp))

            Text(
                "Verification Code",
                color = OWhite, fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp
            )

            Spacer(Modifier.height(10.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "We sent a 6-digit code to your $channelLabel",
                    color = OGray5, fontSize = 14.sp, textAlign = TextAlign.Center
                )

                if (displayContact.isNotBlank()) {
                    // Masked contact badge
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier              = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(OBlue.copy(alpha = 0.10f))
                            .border(1.dp, OBlue.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(channelIcon, fontSize = 14.sp)
                        Text(
                            maskContact(displayContact, isEmail),
                            color = OBlue, fontSize = 14.sp, fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Text(
                    "Wrong ${if (isEmail) "email" else "number"}? Go back",
                    color    = OBlue.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null,
                        onClick           = onBackClick
                    )
                )
            }

            Spacer(Modifier.height(28.dp))

            // OTP card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(OCard)
                    .border(1.dp, OBorder, RoundedCornerShape(24.dp))
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    "Enter 6-digit code",
                    color = OGray5, fontSize = 13.sp,
                    fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp
                )

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    digits.forEachIndexed { i, d ->
                        OtpDigitBox(
                            modifier       = Modifier.weight(1f),
                            value          = d,
                            onValueChange  = { nv ->
                                if (nv.length <= 1 && nv.all { it.isDigit() }) {
                                    setters[i](nv)
                                    if (nv.isNotEmpty() && i < 5) focusReqs[i + 1].requestFocus()
                                }
                                if (nv.isEmpty() && i > 0) focusReqs[i - 1].requestFocus()
                            },
                            focusRequester = focusReqs[i],
                            isSuccess      = allFilled
                        )
                    }
                }

                // Progress bar
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    digits.forEachIndexed { i, d ->
                        val barColor by animateColorAsState(
                            when {
                                allFilled      -> OOk
                                d.isNotEmpty() -> OBlue
                                else           -> OGray6.copy(alpha = 0.3f)
                            }, tween(200), label = "bar$i"
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f).height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(barColor)
                        )
                    }
                }

                // All filled hint
                AnimatedVisibility(visible = allFilled) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier              = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(OOk.copy(alpha = 0.10f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, null,
                            tint = OOk, modifier = Modifier.size(15.dp))
                        Text(
                            "Code complete — tap Verify!",
                            color = OOk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Error
                AnimatedVisibility(visible = authState is AuthState.Error) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(OErr.copy(alpha = 0.10f))
                            .padding(10.dp)
                    ) {
                        Icon(Icons.Default.ErrorOutline, null,
                            tint = OErr, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            (authState as? AuthState.Error)?.message ?: "",
                            color = OErr, fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            PremiumButton(
                text      = if (allFilled) "Verify Code  ✓" else "Verify Code",
                isLoading = isLoading,
                onClick   = {
                    val otp = digits.joinToString("")
                    if (otp.length == 6) authViewModel.verifyOtp(otp)
                }
            )

            Spacer(Modifier.height(24.dp))

            // Resend section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Didn't receive the code?", color = OGray5, fontSize = 14.sp)

                if (resendTimer > 0) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier              = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(OCard)
                            .border(1.dp, OBorder, RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Timer, null, tint = OBlue, modifier = Modifier.size(14.dp))
                        Text(
                            "Resend in ${resendTimer}s",
                            color = OGray3, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(OBlue.copy(alpha = 0.12f))
                            .border(1.dp, OBlue.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication        = null
                            ) { resendTimer = 60; authViewModel.sendOtp(displayContact) }
                            .padding(horizontal = 22.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Resend Code", color = OBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

private fun maskContact(c: String, isEmail: Boolean): String {
    if (isEmail) {
        val at = c.indexOf('@')
        return if (at <= 2) c else c.take(2) + "***" + c.substring(at)
    }
    return if (c.length <= 4) c else c.take(3) + "***" + c.takeLast(3)
}

@Composable
private fun OtpDigitBox(
    modifier       : Modifier      = Modifier,
    value          : String,
    onValueChange  : (String) -> Unit,
    focusRequester : FocusRequester,
    isSuccess      : Boolean       = false
) {
    var focused by remember { mutableStateOf(false) }
    val border by animateColorAsState(
        when {
            isSuccess                       -> OOk
            value.isNotEmpty() || focused   -> OBlue
            else                            -> OFBrd
        }, tween(200), label = "b"
    )
    val bg by animateColorAsState(
        when {
            isSuccess                       -> OOk.copy(alpha = 0.08f)
            focused || value.isNotEmpty()   -> OBlue.copy(alpha = 0.10f)
            else                            -> OFBg
        }, tween(200), label = "bg"
    )
    val scale by animateFloatAsState(
        if (value.isNotEmpty()) 1.06f else 1f,
        spring(stiffness = Spring.StiffnessMedium), label = "sc"
    )

    BasicTextField(
        value           = value,
        onValueChange   = onValueChange,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        singleLine      = true,
        modifier        = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.5.dp, border, RoundedCornerShape(14.dp))
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused },
        decorationBox = { inner ->
            Box(
                modifier         = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (value.isEmpty()) {
                    Text(
                        if (focused) "|" else "—",
                        color      = if (focused) OBlue else OGray6,
                        fontSize   = if (focused) 18.sp else 14.sp,
                        textAlign  = TextAlign.Center
                    )
                }
                inner()
            }
        },
        textStyle = androidx.compose.ui.text.TextStyle(
            color      = OWhite,
            fontSize   = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign  = TextAlign.Center
        )
    )
}