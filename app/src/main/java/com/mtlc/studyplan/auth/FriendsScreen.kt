package com.mtlc.studyplan.auth
import androidx.compose.ui.graphics.Brush

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    currentUser: User,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val friendsRepo = remember { FriendsRepository(context) }
    val scope = rememberCoroutineScope()

    val friends by friendsRepo.friends.collectAsState(initial = emptyList())
    val requests by friendsRepo.friendRequests.collectAsState(initial = emptyList())

    var showInviteDialog by remember { mutableStateOf(false) }
    var inviteEmail by remember { mutableStateOf("") }
    var inviteError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Color(0xFF0066FF))
            ) {
                TopAppBar(
                    title = { Text("Friends") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.Close, "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showInviteDialog = true }) {
                            Icon(Icons.Default.Add, "Invite Friend")
                        }
                    }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Pending requests
            val pendingRequests = requests.filter { it.status == FriendRequestStatus.PENDING }
            if (pendingRequests.isNotEmpty()) {
                item {
                    Text(
                        "Pending Invites",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(pendingRequests) { request ->
                    FriendRequestCard(
                        request = request,
                        onAccept = {
                            scope.launch {
                                friendsRepo.acceptFriendRequest(request.id)
                            }
                        },
                        onReject = {
                            scope.launch {
                                friendsRepo.rejectFriendRequest(request.id)
                            }
                        }
                    )
                }

                item { Spacer(Modifier.height(8.dp)) }
            }

            // Friends list
            item {
                Text(
                    "Friends (${friends.size})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (friends.isEmpty()) {
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
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "No friends yet",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Invite friends to compare stats and rankings",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(friends) { friend ->
                    FriendCard(
                        friend = friend,
                        onRemove = {
                            scope.launch {
                                friendsRepo.removeFriend(friend.id)
                            }
                        }
                    )
                }
            }
        }
    }

    // Invite Dialog
    if (showInviteDialog) {
        AlertDialog(
            onDismissRequest = {
                showInviteDialog = false
                inviteEmail = ""
                inviteError = null
            },
            title = { Text("Invite Friend") },
            text = {
                Column {
                    Text("Enter your friend's email address")
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = inviteEmail,
                        onValueChange = {
                            inviteEmail = it
                            inviteError = null
                        },
                        label = { Text("Friend's Email") },
                        isError = inviteError != null,
                        supportingText = inviteError?.let { { Text(it) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (!AuthRepository.isValidEmail(inviteEmail)) {
                            inviteError = "Invalid email address"
                        } else if (inviteEmail == currentUser.email) {
                            inviteError = "You can't invite yourself"
                        } else {
                            scope.launch {
                                friendsRepo.sendFriendRequest(
                                    currentUser.id,
                                    currentUser.email,
                                    currentUser.username,
                                    inviteEmail
                                )
                                showInviteDialog = false
                                inviteEmail = ""
                                inviteError = null
                            }
                        }
                    }
                ) {
                    Text("Send Invite")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showInviteDialog = false
                    inviteEmail = ""
                    inviteError = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun FriendRequestCard(
    request: FriendRequest,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    request.fromUsername,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Text(
                    request.fromEmail,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onAccept,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Default.Check,
                        "Accept",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                IconButton(
                    onClick = onReject,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Icon(
                        Icons.Default.Close,
                        "Reject",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun FriendCard(
    friend: FriendRelation,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    friend.friendUsername,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "XP: ${friend.friendXp}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Streak: ${friend.friendStreak} days",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            TextButton(onClick = onRemove) {
                Text("Remove", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

