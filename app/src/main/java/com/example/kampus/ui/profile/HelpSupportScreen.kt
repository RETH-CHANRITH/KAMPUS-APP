package com.example.kampus.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.ReportGmailerrorred
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val HBg = Color(0xFF1A1D2E)
private val HCard = Color(0xFF252A41)
private val HBorder = Color(0xFF364153)
private val HWhite = Color(0xFFFFFFFF)
private val HSubtle = Color(0xFF99A1AF)
private val HBlue = Color(0xFF0D7FFF)
private val HDanger = Color(0xFFFB2C36)
private val HDangerText = Color(0xFFFF6467)

private data class ContactOption(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
)

@Composable
fun HelpSupportScreen(onBack: () -> Unit) {
    val contactOptions = listOf(
        ContactOption("KAMPUS Help Center", "Browse articles and guides", Icons.Outlined.Description),
        ContactOption("Live Chat", "Chat with our support team", Icons.Outlined.ChatBubbleOutline),
        ContactOption("Email Support", "support@kampus.app", Icons.Outlined.Email),
        ContactOption("Phone Support", "Call us for assistance", Icons.Outlined.Phone),
    )

    val faqTopics = listOf("Account", "Security", "Payments", "Safety", "Privacy")

    Surface(color = HBg, modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(HCard)
                            .border(1.dp, HBorder, CircleShape)
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = HWhite)
                    }
                    Text("Help & Support", color = HWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Box(Modifier.size(40.dp))
                }
            }

            item {
                Section(title = "Contact Us") {
                    contactOptions.forEachIndexed { index, option ->
                        ContactRow(option = option, showDivider = index != contactOptions.lastIndex)
                    }
                }
            }

            item {
                Section(title = "Frequently Asked Questions") {
                    faqTopics.forEachIndexed { index, topic ->
                        FaqRow(topic = topic, showDivider = index != faqTopics.lastIndex)
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Report a Problem", color = HWhite, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(HDanger.copy(alpha = 0.1f))
                            .border(1.dp, HDanger.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                            .clickable { }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ReportGmailerrorred,
                            contentDescription = null,
                            tint = HDanger,
                            modifier = Modifier.size(20.dp),
                        )
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text("Report Technical Issue", color = HDanger, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Text("Let us know if something isn't working", color = HDangerText, fontSize = 14.sp)
                        }
                    }
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(HCard)
                        .border(1.dp, HBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("App Version", color = HSubtle, fontSize = 14.sp)
                        Text(
                            "1.0.0 (Build 2024.03.21)",
                            color = HWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Text(
                            "Check for Updates",
                            color = HBlue,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 10.dp).clickable { },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, color = HWhite, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(HCard)
                .border(1.dp, HBorder, RoundedCornerShape(14.dp)),
        ) {
            content()
        }
    }
}

@Composable
private fun ContactRow(option: ContactOption, showDivider: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(HBlue.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(option.icon, contentDescription = null, tint = HBlue)
                }
                Column {
                    Text(option.title, color = HWhite, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text(option.subtitle, color = HSubtle, fontSize = 14.sp)
                }
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = HSubtle)
        }

        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .background(HBorder)
                    .size(width = 1.dp, height = 1.dp),
            )
        }
    }
}

@Composable
private fun FaqRow(topic: String, showDivider: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(topic, color = HSubtle, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text("Tap to view related questions", color = HWhite, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = HSubtle)
        }

        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .background(HBorder)
                    .size(width = 1.dp, height = 1.dp),
            )
        }
    }
}