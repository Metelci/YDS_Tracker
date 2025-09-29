package com.mtlc.studyplan.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class LeaderboardEntry(
    val rank: Int,
    val username: String,
    val xp: Int,
    val streak: Int,
    val isCurrentUser: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    currentUser: User,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val friendsRepo = remember { FriendsRepository(context) }
    val friends by friendsRepo.friends.collectAsState(initial = emptyList())

    // Create leaderboard from current user + friends
    val leaderboard = remember(currentUser, friends) {
        val entries = mutableListOf(
            LeaderboardEntry(
                rank = 0,
                username = currentUser.username,
                xp = currentUser.xp,
                streak = currentUser.streak,
                isCurrentUser = true
            )
        )
        entries.addAll(
            friends.map {
                LeaderboardEntry(
                    rank = 0,
                    username = it.friendUsername,
                    xp = it.friendXp,
                    streak = it.friendStreak,
                    isCurrentUser = false
                )
            }
        )

        // Sort by XP descending
        entries.sortedByDescending { it.xp }
            .mapIndexed { index, entry -> entry.copy(rank = index + 1) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Leaderboard") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.EmojiEvents,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "Your Rank: #${leaderboard.find { it.isCurrentUser }?.rank ?: 1}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${currentUser.xp} XP • ${currentUser.streak} day streak",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Rankings",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            itemsIndexed(leaderboard) { index, entry ->
                LeaderboardItemCard(entry = entry)
            }

            if (leaderboard.size <= 1) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Add friends to see rankings",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Invite friends via email to compare your progress",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaderboardItemCard(entry: LeaderboardEntry) {
    val backgroundColor = when (entry.rank) {
        1 -> Brush.horizontalGradient(
            colors = listOf(
                Color(0xFFFFD700).copy(alpha = 0.3f),
                Color(0xFFFFD700).copy(alpha = 0.1f)
            )
        )
        2 -> Brush.horizontalGradient(
            colors = listOf(
                Color(0xFFC0C0C0).copy(alpha = 0.3f),
                Color(0xFFC0C0C0).copy(alpha = 0.1f)
            )
        )
        3 -> Brush.horizontalGradient(
            colors = listOf(
                Color(0xFFCD7F32).copy(alpha = 0.3f),
                Color(0xFFCD7F32).copy(alpha = 0.1f)
            )
        )
        else -> null
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (entry.isCurrentUser) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Box {
            if (backgroundColor != null) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(backgroundColor)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rank badge
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            when (entry.rank) {
                                1 -> Color(0xFFFFD700)
                                2 -> Color(0xFFC0C0C0)
                                3 -> Color(0xFFCD7F32)
                                else -> MaterialTheme.colorScheme.primaryContainer
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${entry.rank}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (entry.rank <= 3) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            entry.username,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (entry.isCurrentUser) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    "YOU",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            "${entry.xp} XP",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "${entry.streak} days",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}