package com.example.kampus.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun AppUIDesign(modifier: Modifier = Modifier) {
    var textValue by remember { mutableStateOf("") }
    var sliderPosition by remember { mutableStateOf(0f) }

    Column(
        modifier = modifier
            .requiredWidth(width = 440.dp)
            .requiredHeight(height = 956.dp)
            .background(color = Color.White)
            .padding(16.dp),
    ) {
        OutlinedTextField(
            value = textValue,
            onValueChange = { textValue = it },
            label = { Text("Edit text") },
            modifier = Modifier.fillMaxWidth(),
        )

        Slider(
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            onValueChangeFinished = { },
        )
    }
}

@Preview(widthDp = 440, heightDp = 956, showBackground = true)
@Composable
private fun AppUIDesignPreview() {
    AppUIDesign(Modifier)
}
