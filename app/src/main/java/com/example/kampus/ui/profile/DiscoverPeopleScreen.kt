package com.example.kampus.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.StarOutline
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class DiscoverTab { Suggested, New, All }

private data class DiscoverPerson(
    val name: String,
    val handle: String,
    val city: String,
    val followers: String,
    val mutual: String,
    val isNew: Boolean,
    val tab: DiscoverTab,
)

private val DpBg = Color(0xFF1A1F2E)
private val DpCard = Color(0xFF252A41)
private val DpBlue = Color(0xFF0D7FFF)
private val DpMuted = Color(0xFF99A1AF)
private val DpWhite = Color.White

@Composable
fun DiscoverPeopleScreen(onBack: () -> Unit) {
    var selected by remember { mutableStateOf(DiscoverTab.New) }
    val people = remember {
        listOf(
            DiscoverPerson("Sophie Anderson", "@sophieanderson", "Leeds, UK", "124", "3 mutual", true, DiscoverTab.New),
            DiscoverPerson("Daniel Kim", "@danielkim", "Bristol, UK", "892", "5 mutual", true, DiscoverTab.New),
            DiscoverPerson("Isabella Martinez", "@isabellamartinez", "Liverpool, UK", "456", "2 mutual", true, DiscoverTab.New),
            DiscoverPerson("Oliver Johnson", "@oliverjohnson", "Edinburgh, UK", "678", "4 mutual", true, DiscoverTab.New),
            DiscoverPerson("Ariana Cole", "@arianacole", "Manchester, UK", "1.1K", "8 mutual", false, DiscoverTab.Suggested),
            DiscoverPerson("Noah Tran", "@noahtran", "London, UK", "2.3K", "11 mutual", false, DiscoverTab.Suggested),
        )
    }

    val visible = when (selected) {
        DiscoverTab.All -> people
        else -> people.filter { it.tab == selected }
    }

    Surface(color = DpBg, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Header(onBack = onBack)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                DiscoverTabChip(
                    title = "Suggested",
                    icon = Icons.Outlined.StarOutline,
                    selected = selected == DiscoverTab.Suggested,
                    modifier = Modifier.weight(1f),
                    onClick = { selected = DiscoverTab.Suggested },
                )
                DiscoverTabChip(
                    title = "New",
                    icon = Icons.Outlined.PersonAdd,
                    selected = selected == DiscoverTab.New,
                    modifier = Modifier.weight(1f),
                    onClick = { selected = DiscoverTab.New },
                )
                DiscoverTabChip(
                    title = "All",
                    icon = Icons.Outlined.GridView,
                    selected = selected == DiscoverTab.All,
                    modifier = Modifier.weight(1f),
                    onClick = { selected = DiscoverTab.All },
                )
            }

            Text(
                text = "New members who recently joined the community",
                color = DpMuted,
                fontSize = 14.sp,
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxSize()) {
                items(visible.chunked(2)) { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        DiscoverCard(person = rowItems[0], modifier = Modifier.weight(1f))
                        if (rowItems.size > 1) {
                            DiscoverCard(person = rowItems[1], modifier = Modifier.weight(1f))
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(12.dp)) }
            }
        }
    }
}

@Composable
private fun Header(onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = DpWhite)
        }
        Text(text = "Discover People", color = DpWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DiscoverTabChip(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) DpBlue else DpCard)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) DpWhite else DpMuted, modifier = Modifier.size(18.dp))
        Text(text = title, color = if (selected) DpWhite else DpMuted, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DiscoverCard(person: DiscoverPerson, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(DpCard),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFFAD46FF), Color(0xFF2B7FFF), Color(0xFF9810FA)),
                    ),
                ),
        ) {
            Box(
                modifier = Modifier
                    .padding(start = 16.dp, top = 56.dp)
                    .size(80.dp)
                    .clip(CircleShape)
                    .border(4.dp, DpCard, CircleShape)
                    .background(Color(0xFF1F6FEB)),
                contentAlignment = Alignment.Center,
            ) {
                Text(person.name.take(1), color = DpWhite, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            }
        }

        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = person.name,
                    color = DpWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (person.isNew) {
                    Box(
                        modifier = Modifier
                            .height(19.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF4ADE80))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "NEW",
                            color = DpBg,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            Text(text = person.handle, color = DpMuted, fontSize = 12.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = DpMuted, modifier = Modifier.size(14.dp))
                Text(text = person.city, color = DpMuted, fontSize = 12.sp)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column {
                    Text(text = person.followers, color = DpWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Followers", color = DpMuted, fontSize = 10.sp)
                }
                Column {
                    Text(text = person.mutual, color = DpBlue, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(text = "friends", color = DpMuted, fontSize = 10.sp)
                }
            }

            Button(
                onClick = {},
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DpBlue, contentColor = DpWhite),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "Follow", fontWeight = FontWeight.Medium)
            }
        }
    }
}