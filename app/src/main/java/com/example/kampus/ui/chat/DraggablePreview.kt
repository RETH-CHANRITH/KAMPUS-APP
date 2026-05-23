package com.example.kampus.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun DraggableLocalPreview(content: @Composable () -> Unit, initialOffset: Offset = Offset(0f, 0f)) {
    val offsetX = remember { mutableStateOf(initialOffset.x) }
    val offsetY = remember { mutableStateOf(initialOffset.y) }

    Surface(
        modifier = Modifier
            .size(width = 152.dp, height = 204.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX.value += dragAmount.x
                    offsetY.value += dragAmount.y
                }
            }
            .then(Modifier),
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 12.dp,
    ) {
        Box(modifier = Modifier.background(color = androidx.compose.ui.graphics.Color(0xFF08111F)), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }) {
                content()
            }
        }
    }
}
