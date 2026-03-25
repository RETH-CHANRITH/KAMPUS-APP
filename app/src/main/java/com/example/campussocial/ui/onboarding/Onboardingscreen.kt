package com.example.campussocial.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip

import androidx.compose.ui.geometry.Offset

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// ── Palette — matches splash green theme ──────────────────────────────────────
private val BgColor     = Color(0xFF000000)   // pure black like splash
private val GreenLight  = Color(0xFF3DDC84)   // same as splash NeonPink
private val GreenDark   = Color(0xFF2ABF6A)   // same as splash NeonOrange
private val ActiveDot   = Color(0xFF3DDC84)   // green active dot
private val InactiveDot = Color(0xFF1A3A2E)   // dark green inactive
private val TextWhite   = Color(0xFFFFFFFF)
private val TextGray    = Color(0xFF8A9A8E)   // greenish gray like splash
private val SkipBg      = Color(0xFF0F1F15)
private val BtnBg       = Color(0xFF111827)
private val NeonPink    = Color(0xFFFF4D8D)
private val NeonOrange  = Color(0xFFFFA94D)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinished : () -> Unit = {},
    viewModel  : OnboardingViewModel = viewModel()
) {
    val pagerState = rememberPagerState { onboardingPages.size }
    val scope      = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        viewModel.onPageChanged(pagerState.currentPage)
    }

    val isLast by remember {
        derivedStateOf { pagerState.currentPage == onboardingPages.size - 1 }
    }

    val goNext: () -> Unit = {
        if (isLast) {
            onFinished()
        } else {
            scope.launch {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
        }
    }

    // Sparkle animations
    val inf2 = rememberInfiniteTransition(label = "stars")
    val ss1 by inf2.animateFloat(0.2f, 1f,   infiniteRepeatable(tween(900),  RepeatMode.Reverse), "ss1")
    val ss2 by inf2.animateFloat(1f,   0.3f, infiniteRepeatable(tween(700),  RepeatMode.Reverse), "ss2")
    val ss3 by inf2.animateFloat(0.5f, 1f,   infiniteRepeatable(tween(1100), RepeatMode.Reverse), "ss3")
    val ss4 by inf2.animateFloat(1f,   0.2f, infiniteRepeatable(tween(800),  RepeatMode.Reverse), "ss4")
    val ss5 by inf2.animateFloat(0.3f, 0.9f, infiniteRepeatable(tween(1300), RepeatMode.Reverse), "ss5")
    val ss6 by inf2.animateFloat(0.8f, 0.1f, infiniteRepeatable(tween(600),  RepeatMode.Reverse), "ss6")
    val ss7 by inf2.animateFloat(0.4f, 1f,   infiniteRepeatable(tween(1500), RepeatMode.Reverse), "ss7")
    val ss8  by inf2.animateFloat(0.7f, 0.2f, infiniteRepeatable(tween(850),  RepeatMode.Reverse), "ss8")
    val ss9  by inf2.animateFloat(0.1f, 0.8f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), "ss9")
    val ss10 by inf2.animateFloat(0.9f, 0.3f, infiniteRepeatable(tween(950),  RepeatMode.Reverse), "ss10")
    val ss11 by inf2.animateFloat(0.4f, 1f,   infiniteRepeatable(tween(750),  RepeatMode.Reverse), "ss11")
    val ss12 by inf2.animateFloat(0.6f, 0.1f, infiniteRepeatable(tween(1250), RepeatMode.Reverse), "ss12")
    val ss13 by inf2.animateFloat(0.2f, 0.9f, infiniteRepeatable(tween(680),  RepeatMode.Reverse), "ss13")
    val ss14 by inf2.animateFloat(1f,   0.4f, infiniteRepeatable(tween(1050), RepeatMode.Reverse), "ss14")
    val ss15 by inf2.animateFloat(0.3f, 0.8f, infiniteRepeatable(tween(820),  RepeatMode.Reverse), "ss15")
    val ss16 by inf2.animateFloat(0.5f, 1f,   infiniteRepeatable(tween(720),  RepeatMode.Reverse), "ss16")
    val ss17 by inf2.animateFloat(0.1f, 0.7f, infiniteRepeatable(tween(990),  RepeatMode.Reverse), "ss17")
    val ss18 by inf2.animateFloat(0.8f, 0.2f, infiniteRepeatable(tween(870),  RepeatMode.Reverse), "ss18")
    val ss19 by inf2.animateFloat(0.3f, 1f,   infiniteRepeatable(tween(640),  RepeatMode.Reverse), "ss19")
    val ss20 by inf2.animateFloat(0.6f, 0.1f, infiniteRepeatable(tween(1150), RepeatMode.Reverse), "ss20")
    val ss21 by inf2.animateFloat(0.2f, 0.9f, infiniteRepeatable(tween(780),  RepeatMode.Reverse), "ss21")
    val ss22 by inf2.animateFloat(0.7f, 0.1f, infiniteRepeatable(tween(1050), RepeatMode.Reverse), "ss22")
    val ss23 by inf2.animateFloat(0.4f, 0.9f, infiniteRepeatable(tween(930),  RepeatMode.Reverse), "ss23")
    val ss24 by inf2.animateFloat(0.1f, 0.8f, infiniteRepeatable(tween(660),  RepeatMode.Reverse), "ss24")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
    ) {
        // Twinkling stars — 48 stars all over the screen!
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // ── Row 1: very top ───────────────────────────────────────────
            drawOnboardingStar(Offset(w*0.04f, h*0.02f), 16f, 4f,   Color.White.copy(alpha=ss1))
            drawOnboardingStar(Offset(w*0.18f, h*0.01f), 7f,  1.8f, GreenLight.copy(alpha=ss2*0.7f))
            drawOnboardingStar(Offset(w*0.33f, h*0.03f), 10f, 2.5f, Color.White.copy(alpha=ss3))
            drawOnboardingStar(Offset(w*0.50f, h*0.01f), 13f, 3.2f, GreenLight.copy(alpha=ss4*0.85f))
            drawOnboardingStar(Offset(w*0.67f, h*0.03f), 8f,  2f,   Color.White.copy(alpha=ss5))
            drawOnboardingStar(Offset(w*0.82f, h*0.02f), 11f, 2.8f, GreenDark.copy(alpha=ss6*0.7f))
            drawOnboardingStar(Offset(w*0.95f, h*0.01f), 18f, 4.5f, Color.White.copy(alpha=ss7))
            // ── Row 2 ─────────────────────────────────────────────────────
            drawOnboardingStar(Offset(w*0.02f, h*0.10f), 9f,  2.2f, GreenLight.copy(alpha=ss8*0.75f))
            drawOnboardingStar(Offset(w*0.12f, h*0.12f), 13f, 3.2f, Color.White.copy(alpha=ss9))
            drawOnboardingStar(Offset(w*0.26f, h*0.09f), 7f,  1.8f, GreenDark.copy(alpha=ss10*0.6f))
            drawOnboardingStar(Offset(w*0.44f, h*0.11f), 11f, 2.8f, Color.White.copy(alpha=ss11*0.9f))
            drawOnboardingStar(Offset(w*0.60f, h*0.08f), 8f,  2f,   GreenLight.copy(alpha=ss12*0.65f))
            drawOnboardingStar(Offset(w*0.76f, h*0.13f), 14f, 3.5f, Color.White.copy(alpha=ss13))
            drawOnboardingStar(Offset(w*0.90f, h*0.10f), 7f,  1.8f, GreenLight.copy(alpha=ss14*0.8f))
            // ── Row 3 ─────────────────────────────────────────────────────
            drawOnboardingStar(Offset(w*0.03f, h*0.23f), 12f, 3f,   Color.White.copy(alpha=ss15))
            drawOnboardingStar(Offset(w*0.16f, h*0.26f), 7f,  1.8f, GreenDark.copy(alpha=ss16*0.55f))
            drawOnboardingStar(Offset(w*0.88f, h*0.24f), 9f,  2.2f, Color.White.copy(alpha=ss17))
            drawOnboardingStar(Offset(w*0.97f, h*0.27f), 15f, 3.8f, GreenLight.copy(alpha=ss18*0.8f))
            // ── Row 4 — mid ───────────────────────────────────────────────
            drawOnboardingStar(Offset(w*0.02f, h*0.38f), 8f,  2f,   Color.White.copy(alpha=ss19*0.7f))
            drawOnboardingStar(Offset(w*0.10f, h*0.42f), 11f, 2.8f, GreenLight.copy(alpha=ss20))
            drawOnboardingStar(Offset(w*0.91f, h*0.36f), 13f, 3.2f, Color.White.copy(alpha=ss21))
            drawOnboardingStar(Offset(w*0.98f, h*0.44f), 7f,  1.8f, GreenDark.copy(alpha=ss22*0.6f))
            // ── Row 5 ─────────────────────────────────────────────────────
            drawOnboardingStar(Offset(w*0.04f, h*0.54f), 10f, 2.5f, Color.White.copy(alpha=ss23*0.8f))
            drawOnboardingStar(Offset(w*0.94f, h*0.52f), 12f, 3f,   GreenLight.copy(alpha=ss24*0.7f))
            drawOnboardingStar(Offset(w*0.07f, h*0.60f), 7f,  1.8f, Color.White.copy(alpha=ss1*0.6f))
            drawOnboardingStar(Offset(w*0.96f, h*0.62f), 9f,  2.2f, GreenDark.copy(alpha=ss2*0.75f))
            // ── Row 6 ─────────────────────────────────────────────────────
            drawOnboardingStar(Offset(w*0.03f, h*0.70f), 14f, 3.5f, Color.White.copy(alpha=ss3*0.85f))
            drawOnboardingStar(Offset(w*0.13f, h*0.73f), 8f,  2f,   GreenLight.copy(alpha=ss4*0.6f))
            drawOnboardingStar(Offset(w*0.88f, h*0.70f), 11f, 2.8f, Color.White.copy(alpha=ss5*0.7f))
            drawOnboardingStar(Offset(w*0.97f, h*0.74f), 7f,  1.8f, GreenLight.copy(alpha=ss6*0.55f))
            // ── Row 7 ─────────────────────────────────────────────────────
            drawOnboardingStar(Offset(w*0.05f, h*0.82f), 12f, 3f,   Color.White.copy(alpha=ss7*0.8f))
            drawOnboardingStar(Offset(w*0.17f, h*0.85f), 7f,  1.8f, GreenDark.copy(alpha=ss8*0.6f))
            drawOnboardingStar(Offset(w*0.85f, h*0.82f), 9f,  2.2f, Color.White.copy(alpha=ss9*0.75f))
            drawOnboardingStar(Offset(w*0.96f, h*0.86f), 16f, 4f,   GreenLight.copy(alpha=ss10*0.8f))
            // ── Bottom row ────────────────────────────────────────────────
            drawOnboardingStar(Offset(w*0.08f, h*0.92f), 11f, 2.8f, Color.White.copy(alpha=ss11*0.7f))
            drawOnboardingStar(Offset(w*0.22f, h*0.94f), 8f,  2f,   GreenLight.copy(alpha=ss12*0.55f))
            drawOnboardingStar(Offset(w*0.38f, h*0.91f), 13f, 3.2f, Color.White.copy(alpha=ss13*0.8f))
            drawOnboardingStar(Offset(w*0.52f, h*0.95f), 7f,  1.8f, GreenDark.copy(alpha=ss14*0.65f))
            drawOnboardingStar(Offset(w*0.66f, h*0.92f), 10f, 2.5f, Color.White.copy(alpha=ss15*0.7f))
            drawOnboardingStar(Offset(w*0.80f, h*0.94f), 8f,  2f,   GreenLight.copy(alpha=ss16*0.6f))
            drawOnboardingStar(Offset(w*0.93f, h*0.91f), 14f, 3.5f, Color.White.copy(alpha=ss17*0.85f))
            // ── Extra center scattered ────────────────────────────────────
            drawOnboardingStar(Offset(w*0.35f, h*0.18f), 9f,  2.2f, Color.White.copy(alpha=ss18*0.5f))
            drawOnboardingStar(Offset(w*0.58f, h*0.22f), 7f,  1.8f, GreenLight.copy(alpha=ss19*0.45f))
            drawOnboardingStar(Offset(w*0.42f, h*0.32f), 8f,  2f,   Color.White.copy(alpha=ss20*0.4f))
            drawOnboardingStar(Offset(w*0.72f, h*0.35f), 6f,  1.5f, GreenDark.copy(alpha=ss21*0.5f))
            drawOnboardingStar(Offset(w*0.28f, h*0.48f), 7f,  1.8f, Color.White.copy(alpha=ss22*0.4f))
            drawOnboardingStar(Offset(w*0.65f, h*0.58f), 8f,  2f,   GreenLight.copy(alpha=ss23*0.45f))
            drawOnboardingStar(Offset(w*0.35f, h*0.65f), 6f,  1.5f, Color.White.copy(alpha=ss24*0.5f))
        }

        Column(
            modifier            = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Skip ──────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 8.dp, end = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (!isLast) {
                    Text(
                        text       = "Skip",
                        color      = Color.White,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier   = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(GreenLight, GreenDark),
                                    start  = androidx.compose.ui.geometry.Offset(0f, 0f),
                                    end    = androidx.compose.ui.geometry.Offset(200f, 0f)
                                )
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication        = null
                            ) {
                                scope.launch {
                                    pagerState.animateScrollToPage(onboardingPages.size - 1)
                                }
                                viewModel.skipToLast(onboardingPages.size)
                            }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                }
            }

            // ── Pager ─────────────────────────────────────────────────────────
            HorizontalPager(
                state          = pagerState,
                modifier       = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                pageSpacing    = 0.dp,
                contentPadding = PaddingValues(0.dp)
            ) { page ->
                val pageOffset = (
                        (pagerState.currentPage - page) +
                                pagerState.currentPageOffsetFraction
                        ).coerceIn(-1f, 1f)

                PageContent(
                    page       = onboardingPages[page],
                    pageOffset = pageOffset
                )
            }

            // ── Dots ──────────────────────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically,
                modifier              = Modifier.padding(top = 20.dp, bottom = 20.dp)
            ) {
                repeat(onboardingPages.size) { i ->
                    AnimatedDot(isActive = i == pagerState.currentPage)
                }
            }

            // ── Swipe Button ──────────────────────────────────────────────────
            SwipeButton(
                isLast   = isLast,
                onSwiped = goNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 40.dp)
            )
        }
    }
}

// ── Swipe-to-unlock button ─────────────────────────────────────────────────────
@Composable
fun SwipeButton(
    onSwiped : () -> Unit,
    modifier : Modifier = Modifier,
    isLast   : Boolean  = false
) {
    var buttonWidthPx by remember { mutableStateOf(0f) }
    val thumbSizeDp   = 58.dp
    val thumbSizePx   = 58f * 3f

    var dragOffsetPx by remember { mutableStateOf(0f) }
    var isSwiped     by remember { mutableStateOf(false) }

    val animatedOffset by animateFloatAsState(
        targetValue   = if (isSwiped) buttonWidthPx - thumbSizePx - 16f else dragOffsetPx,
        animationSpec = if (isSwiped) tween(300, easing = FastOutSlowInEasing) else tween(0),
        finishedListener = {
            if (isSwiped) {
                onSwiped()
                isSwiped     = false
                dragOffsetPx = 0f
            }
        },
        label = "thumbOffset"
    )

    val maxDrag  = (buttonWidthPx - thumbSizePx - 24f).coerceAtLeast(1f)
    val progress = (animatedOffset / maxDrag).coerceIn(0f, 1f)

    val inf = rememberInfiniteTransition(label = "chev")
    val chevPhase by inf.animateFloat(
        initialValue  = 0f,
        targetValue   = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart),
        label         = "chevPhase"
    )
    val heartPulse by inf.animateFloat(
        initialValue  = 1f,
        targetValue   = 1.10f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "heartPulse"
    )

    Box(
        modifier = modifier
            .height(70.dp)
            .clip(RoundedCornerShape(50.dp))
            .background(BtnBg)
            .onSizeChanged { buttonWidthPx = it.width.toFloat() }
    ) {
        // Trail fill — green
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceAtLeast(0.01f))
                .clip(RoundedCornerShape(50.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            GreenLight.copy(alpha = 0.25f),
                            GreenDark.copy(alpha  = 0.10f)
                        )
                    )
                )
        )

        // Label + chevrons
        Row(
            modifier              = Modifier
                .fillMaxSize()
                .padding(start = 70.dp, end = 22.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text          = "Get Started",
                color         = TextWhite.copy(alpha = 1f - progress),
                fontSize      = 17.sp,
                fontWeight    = FontWeight.Bold,
                textAlign     = TextAlign.Center,
                letterSpacing = 0.3.sp,
                modifier      = Modifier.weight(1f)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                repeat(3) { i ->
                    val a = (0.20f + 0.75f * (
                            (kotlin.math.sin((chevPhase - i * 1.2f).toDouble()) * 0.5 + 0.5).toFloat()
                            )) * (1f - progress)
                    Text(
                        text       = ">",
                        color      = GreenLight.copy(alpha = a),
                        fontSize   = 19.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // Draggable heart thumb — green gradient
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(6.dp)
                .size(thumbSizeDp)
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .graphicsLayer {
                    val s = heartPulse * (1f + progress * 0.06f)
                    scaleX             = s
                    scaleY             = s
                    shadowElevation    = 24f
                    shape              = CircleShape
                    ambientShadowColor = GreenLight.copy(alpha = 0.8f)
                    spotShadowColor    = GreenDark.copy(alpha  = 0.6f)
                }
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(GreenLight, GreenDark),
                        start  = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end    = androidx.compose.ui.geometry.Offset(140f, 140f)
                    )
                )
                .pointerInput(buttonWidthPx) {
                    detectHorizontalDragGestures(
                        onDragEnd    = {
                            if (dragOffsetPx >= maxDrag * 0.65f) isSwiped = true
                            else dragOffsetPx = 0f
                        },
                        onDragCancel = { dragOffsetPx = 0f }
                    ) { _, dragAmount ->
                        dragOffsetPx = (dragOffsetPx + dragAmount).coerceIn(0f, maxDrag)
                    }
                }
        ) {
            Icon(
                imageVector        = Icons.Filled.Favorite,
                contentDescription = "Swipe to continue",
                tint               = Color.White,
                modifier           = Modifier.size(26.dp)
            )
        }
    }
}

// ── Page content ───────────────────────────────────────────────────────────────
@Composable
private fun PageContent(page: OnboardingPage, pageOffset: Float) {
    val alpha by animateFloatAsState(
        targetValue   = if (kotlin.math.abs(pageOffset) < 0.5f) 1f else 0.4f,
        animationSpec = tween(250),
        label         = "alpha"
    )

    Column(
        modifier            = Modifier
            .fillMaxSize()
            .graphicsLayer {
                this.alpha   = alpha
                translationX = pageOffset * -40f
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        OnboardingIllustration(
            imageRes = page.imageRes,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.50f)
                .padding(top = 16.dp)
        )

        Spacer(Modifier.height(32.dp))

        Text(
            text          = page.title,
            color         = TextWhite,
            fontSize      = 30.sp,
            fontWeight    = FontWeight.ExtraBold,
            textAlign     = TextAlign.Center,
            lineHeight    = 42.sp,
            letterSpacing = (-0.8).sp,
            modifier      = Modifier.padding(horizontal = 28.dp)
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text       = page.description,
            color      = TextGray,
            fontSize   = 15.sp,
            fontWeight = FontWeight.Normal,
            textAlign  = TextAlign.Center,
            lineHeight = 24.sp,
            modifier   = Modifier.padding(horizontal = 36.dp)
        )
    }
}

// ── Draw 4-point onboarding star ──────────────────────────────────────────────
private fun DrawScope.drawOnboardingStar(
    center : androidx.compose.ui.geometry.Offset,
    outerR : Float,
    innerR : Float,
    color  : Color
) {
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

// ── Animated dot ───────────────────────────────────────────────────────────────
@Composable
private fun AnimatedDot(isActive: Boolean) {
    val width by animateDpAsState(
        targetValue   = if (isActive) 36.dp else 10.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label         = "dot_w"
    )
    val color by animateColorAsState(
        targetValue   = if (isActive) ActiveDot else InactiveDot,
        animationSpec = tween(300),
        label         = "dot_c"
    )
    Box(
        modifier = Modifier
            .height(10.dp)
            .width(width)
            .graphicsLayer {
                shadowElevation    = if (isActive) 12f else 0f
                shape              = CircleShape
                ambientShadowColor = GreenLight.copy(alpha = 0.6f)
                spotShadowColor    = GreenLight.copy(alpha = 0.6f)
            }
            .clip(CircleShape)
            .background(color)
    )
}