package com.example.kampus.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.layout.ContentScale
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kampus.ui.localization.rememberUiStrings
import coil.compose.AsyncImage

private val BBg get() = ProfileColors.Bg
private val BCard get() = ProfileColors.Card
private val BBorder get() = ProfileColors.Border
private val BWhite get() = ProfileColors.White
private val BSubtle get() = ProfileColors.Subtle
private val BBlue get() = ProfileColors.Blue

@Composable
fun BlockedUsersScreen(
    onBack: () -> Unit,
    profileViewModel: ProfileViewModel = viewModel()
) {
    val strings = rememberUiStrings()
    val uiState by profileViewModel.uiState.collectAsStateWithLifecycle()
    val blockedUsers = uiState.blockedUsers

    Surface(color = BBg, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(BCard)
                        .border(1.dp, BBorder, CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = strings.back, tint = BWhite)
                }
                Text(strings.blockedUsersTitle, color = BWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Box(Modifier.size(40.dp))
            }

            if (blockedUsers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(BCard)
                        .border(1.dp, BBorder, RoundedCornerShape(14.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(strings.noBlockedUsers, color = BSubtle, fontSize = 15.sp)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(blockedUsers, key = { it.userId }) { user ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(BCard)
                                .border(1.dp, BBorder, RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                if (user.profileImageUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = user.profileImageUrl,
                                        contentDescription = user.displayName,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop,
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(BBorder),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = user.avatarEmoji,
                                            color = BWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 24.sp,
                                        )
                                    }
                                }
                                Column {
                                    Text(user.displayName, color = BWhite, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                    Text(user.handle, color = BSubtle, fontSize = 14.sp)
                                }
                            }

                            Button(
                                onClick = { profileViewModel.unblockUser(user.userId) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BBlue,
                                    contentColor = BWhite,
                                ),
                                shape = RoundedCornerShape(999.dp),
                            ) {
                                Text(strings.unblock, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}