package com.example.kampus.ui.splash
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.LaunchedEffect
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kampus.ui.theme.NeonPink
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.math.abs

// ── Colors ─────────────────────────────────────────────────────────────────────
val BgColor    = Color(0xFF000000)
private val NeonPink   = Color(0xFF3DDC84)
val NeonOrange = Color(0xFF2ABF6A)
val TextWhite  = Color(0xFFFFFFFF)
val TextGray   = Color(0xFF8A9A8E)
val DinoColor  = Color(0xFF3DDC84)
private val GroundColor= Color(0xFF2A3A2E)

@Composable
fun SplashScreen(
    onNavigateToOnboarding: () -> Unit,
    onNavigateToHome: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    onNavigateToAuthenticated: () -> Unit = {},
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("kampus_prefs", android.content.Context.MODE_PRIVATE)
    }
    val hasSeenOnboarding = remember { prefs.getBoolean("has_seen_onboarding", false) }

    // ── Entry animations ───────────────────────────────────────────────────────
    val iconScale    = remember { Animatable(0f) }
    val iconAlpha    = remember { Animatable(0f) }
    val taglineAlpha = remember { Animatable(0f) }
    val dinoAlpha    = remember { Animatable(0f) }

    // Letter by letter
    var visibleCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        iconAlpha.animateTo(1f, tween(300))
        iconScale.animateTo(
            targetValue   = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness    = Spring.StiffnessMediumLow
            )
        )
        delay(200)
        // Start typing letters
        val appName = "KAMPUS"
        for (i in appName.indices) {
            delay(160L)
            visibleCount = i + 1
        }
        delay(200)
        taglineAlpha.animateTo(1f, tween(500))
        delay(100)
        dinoAlpha.animateTo(1f, tween(400))
        delay(2800)

        if (!hasSeenOnboarding) {
            onNavigateToOnboarding()
        } else if (FirebaseAuth.getInstance().currentUser != null) {
            onNavigateToAuthenticated()
        } else {
            onNavigateToLogin()
        }
    }

    // ── Infinite animations ────────────────────────────────────────────────────
    val inf = rememberInfiniteTransition(label = "splash")

    // Glow
    val glowAlpha by inf.animateFloat(
        initialValue  = 0.3f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "glow"
    )
    val glowScale by inf.animateFloat(
        initialValue  = 0.85f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "glowScale"
    )

    // Sparkles
    val sp1 by inf.animateFloat(0.3f, 1f,   infiniteRepeatable(tween(800),  RepeatMode.Reverse), "sp1")
    val sp2 by inf.animateFloat(1f,   0.3f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), "sp2")
    val sp3 by inf.animateFloat(0.5f, 1f,   infiniteRepeatable(tween(900),  RepeatMode.Reverse), "sp3")
    val sp4 by inf.animateFloat(1f,   0.4f, infiniteRepeatable(tween(700),  RepeatMode.Reverse), "sp4")
    val sp5 by inf.animateFloat(0.2f, 0.9f, infiniteRepeatable(tween(1300), RepeatMode.Reverse), "sp5")
    val sp6 by inf.animateFloat(0.8f, 0.2f, infiniteRepeatable(tween(600),  RepeatMode.Reverse), "sp6")
    val sp7 by inf.animateFloat(0.3f, 1f,   infiniteRepeatable(tween(1500), RepeatMode.Reverse), "sp7")
    val sp8 by inf.animateFloat(0.6f, 0.1f, infiniteRepeatable(tween(850),  RepeatMode.Reverse), "sp8")
    val sp9 by inf.animateFloat(0.1f, 0.85f,infiniteRepeatable(tween(1200), RepeatMode.Reverse), "sp9")

    // Icon float
    val floatY by inf.animateFloat(
        initialValue  = 0f, targetValue = -12f,
        animationSpec = infiniteRepeatable(tween(2200), RepeatMode.Reverse),
        label = "floatY"
    )

    // ── Dino animation ─────────────────────────────────────────────────────────
    // Master time for dino — drives everything
    val dinoTime by inf.animateFloat(
        initialValue  = 0f,
        targetValue   = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Restart),
        label         = "dino_time"
    )

    // Dino x position — runs from left to right and loops
    val dinoX by inf.animateFloat(
        initialValue  = -0.15f,
        targetValue   = 1.15f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label         = "dino_x"
    )

    // Jump — dino jumps periodically
    val jumpTime by inf.animateFloat(
        initialValue  = 0f,
        targetValue   = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label         = "jump"
    )
    // Jump height: parabolic — only goes up (positive = up on screen = negative y)
    val jumpHeight = maxOf(0f, -sin(jumpTime) * 80f)

    // Leg alternation
    val legPhase = sin(dinoTime * 2f)

    // Cactus 1
    val cactusX by inf.animateFloat(
        initialValue  = 1.1f,
        targetValue   = -0.1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label         = "cactus"
    )
    // Cactus 2 — offset timing so they don't overlap
    val cactusX2 by inf.animateFloat(
        initialValue  = 1.6f,
        targetValue   = -0.1f,
        animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing), RepeatMode.Restart),
        label         = "cactus2"
    )
    // Cactus 3 — small, fast
    val cactusX3 by inf.animateFloat(
        initialValue  = 1.3f,
        targetValue   = -0.1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label         = "cactus3"
    )

    Box(
        modifier         = Modifier.fillMaxSize().background(BgColor),
        contentAlignment = Alignment.Center
    ) {

        // Background glow
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NeonPink.copy(alpha = 0.18f * glowAlpha),
                        NeonOrange.copy(alpha = 0.08f * glowAlpha),
                        Color.Transparent
                    ),
                    center = Offset(size.width / 2f, size.height * 0.35f),
                    radius = size.minDimension * glowScale * 0.70f
                )
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier            = Modifier.padding(horizontal = 32.dp)
        ) {

            // ── Icon + sparkles ────────────────────────────────────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier
                    .alpha(iconAlpha.value)
                    .scale(iconScale.value)
                    .size(220.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    drawProStar(Offset(cx - size.width*0.37f, cy - size.height*0.36f), 22f, 5f, TextWhite.copy(alpha=sp1))
                    drawProStar(Offset(cx - size.width*0.12f, cy - size.height*0.48f), 9f,  2f, NeonPink.copy(alpha=sp2*0.8f))
                    drawProStar(Offset(cx + size.width*0.06f, cy - size.height*0.49f), 14f, 3.5f, TextWhite.copy(alpha=sp3*0.9f))
                    drawProStar(Offset(cx + size.width*0.39f, cy - size.height*0.33f), 28f, 7f, TextWhite.copy(alpha=sp4))
                    drawProStar(Offset(cx + size.width*0.46f, cy - size.height*0.15f), 8f,  2f, NeonPink.copy(alpha=sp5*0.7f))
                    drawProStar(Offset(cx + size.width*0.45f, cy + size.height*0.20f), 16f, 4f, NeonOrange.copy(alpha=sp6))
                    drawProStar(Offset(cx + size.width*0.36f, cy + size.height*0.38f), 10f, 2.5f, TextWhite.copy(alpha=sp7*0.6f))
                    drawProStar(Offset(cx - size.width*0.44f, cy - size.height*0.05f), 8f,  2f, NeonPink.copy(alpha=sp3*0.65f))
                    drawProStar(Offset(cx - size.width*0.20f, cy - size.height*0.46f), 6f,  1.5f, TextWhite.copy(alpha=sp6*0.5f))
                    drawProStar(Offset(cx - size.width*0.40f, cy + size.height*0.32f), 14f, 3.5f, NeonPink.copy(alpha=sp8))
                    drawProStar(Offset(cx - size.width*0.46f, cy - size.height*0.24f), 20f, 5f, TextWhite.copy(alpha=sp9))
                    drawProStar(Offset(cx + size.width*0.42f, cy - size.height*0.44f), 7f,  1.8f, NeonOrange.copy(alpha=sp1*0.8f))
                    drawProStar(Offset(cx - size.width*0.05f, cy + size.height*0.45f), 7f,  1.8f, TextWhite.copy(alpha=sp8*0.55f))
                }

                // Glow disc behind icon
                Canvas(modifier = Modifier.size(140.dp).scale(glowScale * 0.95f)) {
                    drawCircle(
                        brush = Brush.radialGradient(listOf(
                            NeonPink.copy(alpha = glowAlpha * 0.55f),
                            NeonOrange.copy(alpha = glowAlpha * 0.25f),
                            Color.Transparent
                        ))
                    )
                }

                // Icon
                Box(
                    contentAlignment = Alignment.Center,
                    modifier         = Modifier
                        .size(120.dp)
                        .offset(y = floatY.dp)
                        .graphicsLayer {
                            shadowElevation    = 48f
                            shape              = RoundedCornerShape(30.dp)
                            ambientShadowColor = NeonPink.copy(alpha = 0.6f)
                            spotShadowColor    = NeonOrange.copy(alpha = 0.5f)
                        }
                        .background(
                            Brush.linearGradient(listOf(NeonPink, NeonOrange),
                                Offset(0f, 0f), Offset(340f, 340f)),
                            RoundedCornerShape(30.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier.size(120.dp).background(
                            Brush.radialGradient(listOf(Color.White.copy(alpha=0.22f), Color.Transparent), radius=120f),
                            RoundedCornerShape(30.dp)
                        )
                    )
                    Text("🎓", fontSize = 54.sp)
                }
            }

            Spacer(Modifier.height(36.dp))

            // ── Letter by letter app name ──────────────────────────────────────
            LetterByLetterText(visibleCount = visibleCount)

            Spacer(Modifier.height(14.dp))

            // ── Tagline ────────────────────────────────────────────────────────
            val strings = com.example.kampus.ui.localization.rememberUiStrings()
            Text(
                text       = strings.appTagline,
                color      = TextGray,
                fontSize   = 16.sp,
                fontWeight = FontWeight.Normal,
                textAlign  = TextAlign.Center,
                lineHeight = 26.sp,
                modifier   = Modifier.alpha(taglineAlpha.value)
            )


        }

        // ── Dinosaur runner — pinned to very bottom of screen ─────────────────
        Canvas(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .alpha(dinoAlpha.value)
                .fillMaxWidth()
                .height(120.dp)
                .padding(bottom = 110.dp)
        ) {
            val groundY = size.height - 20f

            // Ground line
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, NeonPink.copy(alpha=0.5f),
                        NeonPink.copy(alpha=0.5f), Color.Transparent)
                ),
                start       = Offset(0f, groundY),
                end         = Offset(size.width, groundY),
                strokeWidth = 2.5f
            )

            val dx = dinoX * size.width
            val dy = groundY - jumpHeight
            drawDino(x=dx, y=dy, legPhase=legPhase, color=DinoColor, isAir=jumpHeight > 5f)

            drawCactus(x=cactusX  * size.width, y=groundY, color=NeonOrange.copy(alpha=0.85f), scale=1.0f)
            drawCactus(x=cactusX2 * size.width, y=groundY, color=NeonPink.copy(alpha=0.75f),   scale=1.4f)
            drawCactus(x=cactusX3 * size.width, y=groundY, color=NeonOrange.copy(alpha=0.65f), scale=0.7f)

            if (jumpHeight < 5f) {
                val dustAlpha = abs(sin(dinoTime)) * 0.4f
                drawCircle(NeonPink.copy(alpha=dustAlpha*0.5f), 3f, Offset(dx-20f, groundY+2f))
                drawCircle(NeonPink.copy(alpha=dustAlpha*0.3f), 2f, Offset(dx-32f, groundY-2f))
            }
        }
    }
}

// ── Draw Dinosaur ─────────────────────────────────────────────────────────────
fun DrawScope.drawDino(
    x        : Float,
    y        : Float,
    legPhase : Float,
    color    : Color,
    isAir    : Boolean
) {
    val s = 1.8f  // scale factor — bigger dino

    // Body
    drawRoundRect(
        color        = color,
        topLeft      = Offset(x - 14*s, y - 36*s),
        size         = Size(28*s, 24*s),
        cornerRadius = CornerRadius(6*s)
    )

    // Head
    drawRoundRect(
        color        = color,
        topLeft      = Offset(x + 2*s, y - 52*s),
        size         = Size(22*s, 20*s),
        cornerRadius = CornerRadius(5*s)
    )

    // Mouth (small rectangle)
    drawRoundRect(
        color        = color,
        topLeft      = Offset(x + 18*s, y - 44*s),
        size         = Size(8*s, 5*s),
        cornerRadius = CornerRadius(2*s)
    )

    // Eye (white dot)
    drawCircle(
        color  = Color.Black,
        radius = 3*s,
        center = Offset(x + 16*s, y - 46*s)
    )
    drawCircle(
        color  = Color.White,
        radius = 1.5f*s,
        center = Offset(x + 17*s, y - 47*s)
    )

    // Tail
    drawRoundRect(
        color        = color,
        topLeft      = Offset(x - 26*s, y - 28*s),
        size         = Size(14*s, 8*s),
        cornerRadius = CornerRadius(4*s)
    )

    // Small arm
    drawRoundRect(
        color        = color,
        topLeft      = Offset(x + 8*s, y - 26*s),
        size         = Size(10*s, 6*s),
        cornerRadius = CornerRadius(3*s)
    )

    // Legs — alternate when running, tucked when jumping
    if (isAir) {
        // Both legs tucked
        drawRoundRect(color=color, topLeft=Offset(x-10*s, y-14*s), size=Size(8*s, 14*s), cornerRadius=CornerRadius(3*s))
        drawRoundRect(color=color, topLeft=Offset(x+2*s,  y-14*s), size=Size(8*s, 14*s), cornerRadius=CornerRadius(3*s))
    } else {
        // Alternating legs
        val leg1H = 14*s + legPhase * 5*s
        val leg2H = 14*s - legPhase * 5*s
        drawRoundRect(color=color, topLeft=Offset(x-10*s, y-14*s), size=Size(8*s, leg1H), cornerRadius=CornerRadius(3*s))
        drawRoundRect(color=color, topLeft=Offset(x+2*s,  y-14*s), size=Size(8*s, leg2H), cornerRadius=CornerRadius(3*s))
    }
}

// ── Draw Cactus ───────────────────────────────────────────────────────────────
fun DrawScope.drawCactus(x: Float, y: Float, color: Color, scale: Float = 1f) {
    val s = scale
    // Main trunk
    drawRoundRect(
        color        = color,
        topLeft      = Offset(x - 6*s, y - 50*s),
        size         = Size(12*s, 50*s),
        cornerRadius = CornerRadius(5*s)
    )
    // Left arm
    drawRoundRect(color=color, topLeft=Offset(x-20*s, y-38*s), size=Size(16*s, 8*s), cornerRadius=CornerRadius(4*s))
    drawRoundRect(color=color, topLeft=Offset(x-22*s, y-54*s), size=Size(9*s,  20*s), cornerRadius=CornerRadius(4*s))
    // Right arm
    drawRoundRect(color=color, topLeft=Offset(x+6*s,  y-30*s), size=Size(16*s, 8*s),  cornerRadius=CornerRadius(4*s))
    drawRoundRect(color=color, topLeft=Offset(x+14*s, y-44*s), size=Size(9*s,  22*s), cornerRadius=CornerRadius(4*s))
}

// ── Letter-by-letter text ──────────────────────────────────────────────────────
@Composable
fun LetterByLetterText(
    visibleCount : Int,
    modifier     : Modifier = Modifier
) {
    val fullText  = "KAMPUS"

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment     = Alignment.CenterVertically,
        modifier              = modifier
    ) {
        fullText.forEachIndexed { i, char ->
            val alpha by animateFloatAsState(
                targetValue   = if (i < visibleCount) 1f else 0f,
                animationSpec = tween(280, easing = FastOutSlowInEasing),
                label         = "la$i"
            )
            val offsetY by animateFloatAsState(
                targetValue   = if (i < visibleCount) 0f else 14f,
                animationSpec = tween(320, easing = FastOutSlowInEasing),
                label         = "lo$i"
            )
            Text(
                text          = char.toString(),
                color         = TextWhite,
                fontSize      = 42.sp,
                fontWeight    = FontWeight.ExtraBold,
                letterSpacing = (-1.2).sp,
                modifier      = Modifier.alpha(alpha).offset(y = offsetY.dp)
            )
        }
    }
}

// ── 4-point star ──────────────────────────────────────────────────────────────
fun DrawScope.drawProStar(center: Offset, outerR: Float, innerR: Float, color: Color) {
    val path = Path()
    for (i in 0 until 8) {
        val angle  = Math.PI / 4 * i - Math.PI / 2
        val radius = if (i % 2 == 0) outerR else innerR
        val px     = center.x + (radius * Math.cos(angle)).toFloat()
        val py     = center.y + (radius * Math.sin(angle)).toFloat()
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    drawPath(path, color)
}