package com.example.kampus.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun DraggableLocalPreview(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val previewWidth = 118.dp
        val previewHeight = 158.dp
        val horizontalPadding = 18.dp
        val bottomPadding = 112.dp

        val maxX = with(density) { (maxWidth - previewWidth).toPx() }.coerceAtLeast(0f)
        val maxY = with(density) { (maxHeight - previewHeight).toPx() }.coerceAtLeast(0f)
        val initialX = with(density) { (maxWidth - previewWidth - horizontalPadding).toPx() }.coerceIn(0f, maxX)
        val initialY = with(density) { (maxHeight - previewHeight - bottomPadding).toPx() }.coerceIn(0f, maxY)

        var offsetX by remember(maxWidth, maxHeight) { mutableFloatStateOf(initialX) }
        var offsetY by remember(maxWidth, maxHeight) { mutableFloatStateOf(initialY) }

        Surface(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .size(width = previewWidth, height = previewHeight)
                .shadow(18.dp, RoundedCornerShape(24.dp), clip = false)
                .pointerInput(maxX, maxY) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX = min(max(offsetX + dragAmount.x, 0f), maxX)
                        offsetY = min(max(offsetY + dragAmount.y, 0f), maxY)
                    }
                },
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 0.dp,
        ) {
            Box(
                modifier = Modifier.background(color = androidx.compose.ui.graphics.Color(0xFF050A14)),
                contentAlignment = Alignment.Center,
            ) {
                content()
            }
        }
    }
}
