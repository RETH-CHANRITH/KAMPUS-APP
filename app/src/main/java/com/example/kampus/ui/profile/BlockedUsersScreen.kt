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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BBg = Color(0xFF1A1D2E)
private val BCard = Color(0xFF252A41)
private val BBorder = Color(0xFF364153)
private val BWhite = Color(0xFFFFFFFF)
private val BSubtle = Color(0xFF99A1AF)
private val BBlue = Color(0xFF0D7FFF)

private data class BlockedUser(
    val id: Int,
    val name: String,
    val handle: String,
)

@Composable
fun BlockedUsersScreen(onBack: () -> Unit) {
    var blockedUsers by remember {
        mutableStateOf(
            listOf(
                BlockedUser(1, "John Smith", "@johnsmith"),
                BlockedUser(2, "Emma Davis", "@emmadavis"),
            )
        )
    }

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
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = BWhite)
                }
                Text("Blocked Users", color = BWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
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
                    Text("No blocked users", color = BSubtle, fontSize = 15.sp)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(blockedUsers, key = { it.id }) { user ->
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
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4A5565)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = user.name.first().toString(),
                                        color = BWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                    )
                                }
                                Column {
                                    Text(user.name, color = BWhite, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                    Text(user.handle, color = BSubtle, fontSize = 14.sp)
                                }
                            }

                            Button(
                                onClick = { blockedUsers = blockedUsers.filterNot { it.id == user.id } },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BBlue,
                                    contentColor = BWhite,
                                ),
                                shape = RoundedCornerShape(999.dp),
                            ) {
                                Text("Unblock", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}