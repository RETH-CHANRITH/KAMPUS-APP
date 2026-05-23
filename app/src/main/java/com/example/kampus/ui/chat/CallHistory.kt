package com.example.kampus.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class CallHistoryItem(val name: String, val timestamp: String, val status: String)

@Composable
fun CallHistoryCard(item: CallHistoryItem, onClick: () -> Unit = {}) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .clickable { onClick() }
        .background(Color.Transparent)
        .padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF0F1A33)), contentAlignment = Alignment.Center) {
            Text(text = item.name.firstOrNull()?.toString() ?: "U", color = Color.White, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(text = item.name, color = Color.White, fontSize = 14.sp)
            Text(text = "${item.status} • ${item.timestamp}", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
        }
    }
}
