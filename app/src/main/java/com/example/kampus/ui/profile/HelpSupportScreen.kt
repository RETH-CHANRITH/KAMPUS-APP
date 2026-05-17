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
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kampus.ui.localization.rememberUiStrings

private val HBg get() = ProfileColors.Bg
private val HCard get() = ProfileColors.Card
private val HBorder get() = ProfileColors.Border
private val HWhite get() = ProfileColors.White
private val HSubtle get() = ProfileColors.Subtle
private val HBlue get() = ProfileColors.Blue
private val HDanger = Color(0xFFFB2C36)
private val HDangerText = Color(0xFFFF6467)

private data class ContactOption(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconKey: String,
    val actionValue: String,
)

@Composable
fun HelpSupportScreen(
    onBack: () -> Unit,
    supportViewModel: SupportContentViewModel = viewModel(),
) {
    val strings = rememberUiStrings()
    val context = LocalContext.current
    val supportContent by supportViewModel.uiState.collectAsStateWithLifecycle()
    val contactOptions = supportContent.contactOptions.map { option ->
        ContactOption(
            title = option.title,
            subtitle = option.subtitle,
            icon = SupportContentViewModel.iconForContact(option.iconKey),
            iconKey = option.iconKey,
            actionValue = option.actionValue,
        )
    }

    val faqTopics = supportContent.faqTopics

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
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = strings.back, tint = HWhite)
                    }
                    Text(strings.helpAndSupport, color = HWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Box(Modifier.size(40.dp))
                }
            }

            item {
                Section(title = strings.contactUs) {
                    contactOptions.forEachIndexed { index, option ->
                        ContactRow(
                            option = option,
                            showDivider = index != contactOptions.lastIndex,
                            onClick = {
                                when (option.iconKey.lowercase()) {
                                    "email" -> openEmail(
                                        context = context,
                                        email = option.actionValue.ifBlank { "support@kampus.app" },
                                        subject = "KAMPUS support request",
                                        body = "Hi KAMPUS support,\n\n",
                                    )
                                    "phone" -> openDialer(context, option.actionValue.ifBlank { "18005550199" })
                                    else -> openWebPage(context, option.actionValue.ifBlank { "https://kampus.app/help" })
                                }
                            },
                        )
                    }
                }
            }

            item {
                Section(title = strings.frequentlyAskedQuestions) {
                    faqTopics.forEachIndexed { index, topic ->
                        FaqRow(
                            topic = topic,
                            showDivider = index != faqTopics.lastIndex,
                            strings = strings,
                            onClick = {
                                openWebPage(context, "https://kampus.app/help/faq?topic=${topic.lowercase().replace(" ", "-")}")
                            },
                        )
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(strings.reportAProblem, color = HWhite, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(HDanger.copy(alpha = 0.1f))
                            .border(1.dp, HDanger.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                            .clickable {
                                openEmail(
                                    context = context,
                                    email = "support@kampus.app",
                                    subject = "KAMPUS technical issue",
                                    body = "Hi KAMPUS support,\n\nI need help with:\n",
                                )
                            }
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
                            Text(supportContent.reportTechnicalIssueTitle, color = HDanger, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Text(supportContent.reportTechnicalIssueHelp, color = HDangerText, fontSize = 14.sp)
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
                        Text(strings.appVersion, color = HSubtle, fontSize = 14.sp)
                        Text(
                            supportContent.appVersionText,
                            color = HWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Text(
                            supportContent.checkForUpdatesText.ifBlank { strings.checkForUpdates },
                            color = HBlue,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 10.dp).clickable {
                                openWebPage(context, "https://kampus.app/download")
                            },
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
private fun ContactRow(option: ContactOption, showDivider: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
private fun FaqRow(topic: String, showDivider: Boolean, strings: com.example.kampus.ui.localization.UiStrings, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(topic, color = HSubtle, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text(strings.tapToViewQuestions, color = HWhite, fontSize = 15.sp, fontWeight = FontWeight.Medium)
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