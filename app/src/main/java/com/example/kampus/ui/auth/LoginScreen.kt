@file:Suppress("DEPRECATION")
package com.example.kampus.ui.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kampus.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions


// ── Design tokens ──────────────────────────────────────────────────────────────
private val Bg          = Color(0xFF0A0D14)
private val Card        = Color(0xFF111827)
private val CardBorder  = Color(0xFF1E2A3A)
private val Blue        = Color(0xFF3B82F6)
private val BlueGlow    = Color(0xFF2563EB)
private val FieldBg     = Color(0xFF0F1623)
private val FieldBorder = Color(0xFF1E2A3A)
private val FieldFocus  = Color(0xFF3B82F6)
private val ErrorRed    = Color(0xFFFF4D6A)
private val White       = Color(0xFFFFFFFF)
private val Gray300     = Color(0xFFD1D5DB)
private val Gray500     = Color(0xFF6B7280)
private val Gray600     = Color(0xFF4B5563)

@Composable
fun LoginScreen(
    onLoginSuccess   : () -> Unit = {},
    onRegisterClick  : () -> Unit = {},
    onForgotPassword : () -> Unit = {},
    onGoogleClick    : () -> Unit = {},
    onAppleClick     : () -> Unit = {},
    onBackClick      : () -> Unit = {},
    authViewModel    : AuthViewModel = viewModel()
) {
    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var saveMe          by remember { mutableStateOf(true) }
    var emailError      by remember { mutableStateOf("") }
    var passwordError   by remember { mutableStateOf("") }

    val authState  by authViewModel.authState.collectAsState()
    val context = LocalContext.current

    var topErrorMessage by remember { mutableStateOf<String?>(null) }

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        @Suppress("DEPRECATION")
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(Exception::class.java)
            val idToken: String? = account.idToken
            if (!idToken.isNullOrBlank()) {
                authViewModel.loginWithGoogle(idToken)
            }
        } catch (_: Exception) {
            // AuthViewModel will show errors on next state update; keep UI silent here.
        }
    }

    fun startGoogleSignIn() {
        @Suppress("DEPRECATION")
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        @Suppress("DEPRECATION")
        val client = GoogleSignIn.getClient(context, gso)
        googleLauncher.launch(client.signInIntent)
    }
    val isLoading   = authState is AuthState.Loading
    val focusManager = LocalFocusManager.current

    // Ambient glow animation
    val inf = rememberInfiniteTransition(label = "login_glow")
    val glowAlpha by inf.animateFloat(
        initialValue  = 0.06f, targetValue = 0.14f,
        animationSpec = infiniteRepeatable(tween(2400), RepeatMode.Reverse),
        label = "glow"
    )

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) onLoginSuccess()
        val error = authState as? AuthState.Error
        if (error != null) {
            topErrorMessage = error.message
        }
    }

    fun validate(): Boolean {
        var ok = true
        emailError = when {
            email.isBlank()      -> { ok = false; "Email is required" }
            !email.contains("@") -> { ok = false; "Enter a valid email" }
            else -> ""
        }
        passwordError = when {
            password.isBlank()  -> { ok = false; "Password is required" }
            password.length < 6 -> { ok = false; "At least 6 characters" }
            else -> ""
        }
        return ok
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
    ) {
        // ── Ambient top glow ───────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Blue.copy(alpha = glowAlpha), Color.Transparent),
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

            // ── Top row: back + logo ───────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                AuthBackButton(onClick = onBackClick)

                // App badge
                Box(
                    contentAlignment = Alignment.Center,
                    modifier         = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Blue, BlueGlow),
                                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                end   = androidx.compose.ui.geometry.Offset(80f, 80f)
                            )
                        )
                ) {
                    Text("🎓", fontSize = 20.sp)
                }
            }

            Spacer(Modifier.height(40.dp))

            AnimatedVisibility(
                visible = topErrorMessage != null,
                enter = fadeIn(tween(160)) + expandVertically(tween(200)),
                exit = fadeOut(tween(120)) + shrinkVertically(tween(160))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ErrorRed.copy(alpha = 0.12f))
                        .border(1.dp, ErrorRed.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.ErrorOutline, null, tint = ErrorRed, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        topErrorMessage.orEmpty(),
                        modifier = Modifier.weight(1f),
                        color = Gray300,
                        fontSize = 13.sp,
                        lineHeight = 16.sp
                    )
                    TextButton(
                        onClick = {
                            topErrorMessage = null
                            authViewModel.resetState()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)
                    ) { Text("Dismiss") }
                }
            }

            // ── Header ────────────────────────────────────────────────────────
            Column(
                modifier            = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    "Welcome back 👋",
                    color      = Gray500,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Login to your\nAccount",
                    color         = White,
                    fontSize      = 32.sp,
                    fontWeight    = FontWeight.ExtraBold,
                    lineHeight    = 42.sp,
                    letterSpacing = (-0.5).sp
                )
            }

            Spacer(Modifier.height(36.dp))

            // ── Form card ─────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Card)
                    .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PremiumInputField(
                    value         = email,
                    onValueChange = { email = it; emailError = ""; authViewModel.resetState() },
                    label         = "Email Address",
                    placeholder   = "you@example.com",
                    icon          = Icons.Outlined.Email,
                    error         = emailError,
                    keyboardType  = KeyboardType.Email,
                    imeAction     = ImeAction.Next
                )

                PremiumInputField(
                    value            = password,
                    onValueChange    = { password = it; passwordError = ""; authViewModel.resetState() },
                    label            = "Password",
                    placeholder      = "Enter your password",
                    icon             = Icons.Outlined.Lock,
                    error            = passwordError,
                    isPassword       = true,
                    passwordVisible  = passwordVisible,
                    onTogglePassword = { passwordVisible = !passwordVisible },
                    imeAction        = ImeAction.Done,
                    onDone           = { focusManager.clearFocus() }
                )

                // Remember + Forgot
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Custom checkbox
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(if (saveMe) Blue else FieldBg)
                                .border(
                                    1.dp,
                                    if (saveMe) Blue else FieldBorder,
                                    RoundedCornerShape(5.dp)
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication        = null
                                ) { saveMe = !saveMe }
                        ) {
                            if (saveMe) {
                                Icon(
                                    Icons.Default.Check, null,
                                    tint     = White,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                        Text("Remember me", color = Gray500, fontSize = 13.sp)
                    }
                    Text(
                        "Forgot Password?",
                        color      = Blue,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier   = Modifier.clickable(onClick = onForgotPassword)
                    )
                }

                // Server error
                // Error is shown as a top banner to keep the form clean.
            }

            Spacer(Modifier.height(24.dp))

            // ── Login button ──────────────────────────────────────────────────
            PremiumButton(
                text      = "Login",
                isLoading = isLoading,
                onClick   = {
                    if (validate()) {
                        focusManager.clearFocus()
                        authViewModel.login(email, password)
                    }
                }
            )

            Spacer(Modifier.height(20.dp))

            // ── Sign up link ───────────────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("Don't have an account? ", color = Gray500, fontSize = 14.sp)
                Text(
                    "Sign Up",
                    color      = Blue,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.clickable(onClick = onRegisterClick)
                )
            }

            Spacer(Modifier.height(32.dp))

            // ── Divider ────────────────────────────────────────────────────────
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier              = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider(Modifier.weight(1f), color = CardBorder)
                Text("or continue with", color = Gray600, fontSize = 12.sp)
                HorizontalDivider(Modifier.weight(1f), color = CardBorder)
            }

            Spacer(Modifier.height(20.dp))

            // ── Social buttons ─────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PremiumSocialButton(
                    name     = "Google",
                    logoRes  = R.drawable.logo_google,
                    onClick  = {
                        onGoogleClick()
                        startGoogleSignIn()
                    },
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

// ── Premium Input Field ────────────────────────────────────────────────────────
@Composable
fun PremiumInputField(
    value            : String,
    onValueChange    : (String) -> Unit,
    label            : String,
    placeholder      : String,
    icon             : ImageVector,
    error            : String       = "",
    isPassword       : Boolean      = false,
    passwordVisible  : Boolean      = false,
    onTogglePassword : () -> Unit   = {},
    keyboardType     : KeyboardType = KeyboardType.Text,
    comment          : String       = "",
    imeAction        : ImeAction    = ImeAction.Next,
    onDone           : () -> Unit   = {}
) {
    var focused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        targetValue = when {
            error.isNotEmpty() -> ErrorRed
            focused            -> FieldFocus
            else               -> FieldBorder
        },
        animationSpec = tween(200), label = "border"
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            label,
            color      = if (focused) Blue else Gray500,
            fontSize   = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.3.sp
        )
        OutlinedTextField(
            value         = value,
            onValueChange = onValueChange,
            placeholder   = { Text(placeholder, color = Gray600, fontSize = 14.sp) },
            leadingIcon   = {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier         = Modifier
                        .padding(start = 4.dp)
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (focused) Blue.copy(alpha = 0.12f) else FieldBorder.copy(alpha = 0.5f))
                ) {
                    Icon(icon, null,
                        tint     = if (focused) Blue else Gray500,
                        modifier = Modifier.size(18.dp))
                }
            },
            trailingIcon = if (isPassword) {{
                IconButton(onClick = onTogglePassword) {
                    Icon(
                        if (passwordVisible) Icons.Outlined.Visibility
                        else Icons.Outlined.VisibilityOff,
                        null,
                        tint     = Gray500,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }} else null,
            visualTransformation = if (isPassword && !passwordVisible)
                PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            singleLine      = true,
            isError         = error.isNotEmpty(),
            shape           = RoundedCornerShape(14.dp),
            colors          = OutlinedTextFieldDefaults.colors(
                focusedBorderColor      = borderColor,
                unfocusedBorderColor    = borderColor,
                errorBorderColor        = ErrorRed,
                focusedContainerColor   = FieldBg,
                unfocusedContainerColor = FieldBg,
                errorContainerColor     = ErrorRed.copy(alpha = 0.05f),
                cursorColor             = Blue,
                focusedTextColor        = White,
                unfocusedTextColor      = White,
                errorTextColor          = White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused }
        )
        AnimatedVisibility(
            visible = error.isNotEmpty(),
            enter   = fadeIn() + expandVertically(),
            exit    = fadeOut() + shrinkVertically()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ErrorOutline, null,
                    tint = ErrorRed, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text(error, color = ErrorRed, fontSize = 11.sp)
            }
        }
    }
}

// ── Premium Button ─────────────────────────────────────────────────────────────
@Composable
fun PremiumButton(
    text      : String,
    onClick   : () -> Unit,
    isLoading : Boolean  = false,
    modifier  : Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue   = if (pressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label         = "btn_scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(Blue, BlueGlow),
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end   = androidx.compose.ui.geometry.Offset(500f, 0f)
                )
            )
            .clickable(
                interactionSource = interactionSource,
                indication        = null
            ) { if (!isLoading) onClick() }
    ) {
        // Shine overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(White.copy(alpha = 0.10f), Color.Transparent),
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end   = androidx.compose.ui.geometry.Offset(0f, 100f)
                    )
                )
        )
        if (isLoading) {
            CircularProgressIndicator(
                color       = White,
                modifier    = Modifier.size(24.dp),
                strokeWidth = 2.5.dp
            )
        } else {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text          = text,
                    color         = White,
                    fontSize      = 16.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward, null,
                    tint     = White.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ── Premium Social Button ──────────────────────────────────────────────────────
@Composable
fun PremiumSocialButton(
    name     : String,
    logoRes  : Int,
    onClick  : () -> Unit,
    modifier : Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue   = if (pressed) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label         = "social_scale"
    )

    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .height(54.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(14.dp))
            .background(Card)
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick
            )
    ) {
        Image(
            painter            = painterResource(id = logoRes),
            contentDescription = name,
            contentScale       = ContentScale.Fit,
            modifier           = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            name,
            color      = Gray300,
            fontSize   = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ── Back Button ────────────────────────────────────────────────────────────────
@Composable
fun AuthBackButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue   = if (pressed) 0.92f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label         = "back_scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(13.dp))
            .background(Card)
            .border(1.dp, CardBorder, RoundedCornerShape(13.dp))
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick
            )
    ) {
        Icon(
            Icons.Default.ArrowBackIosNew, null,
            tint     = White,
            modifier = Modifier.size(16.dp)
        )
    }
}

// ── Keep old names as aliases so other screens still compile ───────────────────
@Composable
fun AuthInputField(
    modifier         : Modifier = Modifier,
    value            : String,
    onValueChange    : (String) -> Unit,
    label            : String,
    placeholder      : String,
    icon             : ImageVector,
    error            : String       = "",
    isPassword       : Boolean      = false,
    passwordVisible  : Boolean      = false,
    onTogglePassword : () -> Unit   = {},
    keyboardType     : KeyboardType = KeyboardType.Text,
    imeAction        : ImeAction    = ImeAction.Next,
    onDone           : () -> Unit   = {}
) = PremiumInputField(
    value,
    onValueChange,
    label,
    placeholder,
    icon,
    error,
    isPassword,
    passwordVisible,
    onTogglePassword,
    keyboardType,
    "",
    imeAction,
    onDone,
)

@Composable
fun AuthPrimaryButton(
    modifier  : Modifier = Modifier,
    text      : String,
    onClick   : () -> Unit,
    isLoading : Boolean  = false,
) = PremiumButton(text, onClick, isLoading, modifier)

@Composable
fun AuthSocialButton(
    modifier : Modifier = Modifier,
    name     : String,
    logoRes  : Int,
    onClick  : () -> Unit,
) = PremiumSocialButton(name, logoRes, onClick, modifier)

@Composable
fun AuthSocialDivider() {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier              = Modifier.fillMaxWidth()
    ) {
        HorizontalDivider(Modifier.weight(1f), color = CardBorder)
        Text("or continue with", color = Gray600, fontSize = 12.sp)
        HorizontalDivider(Modifier.weight(1f), color = CardBorder)
    }
}
