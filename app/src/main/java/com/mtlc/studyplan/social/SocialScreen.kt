package com.mtlc.studyplan.social

// import androidx.compose.material3.FloatingActionButton
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.mtlc.studyplan.R
import com.mtlc.studyplan.data.social.Friend
import com.mtlc.studyplan.data.social.PersistentSocialRepository
import com.mtlc.studyplan.data.social.SocialProfile
import com.mtlc.studyplan.data.social.SocialRepository
import com.mtlc.studyplan.database.StudyPlanDatabase
import com.mtlc.studyplan.social.components.SocialSegmentedTabs
import com.mtlc.studyplan.social.tabs.AwardsTab
import com.mtlc.studyplan.social.tabs.FriendsTab
import com.mtlc.studyplan.social.tabs.ProfileTab
import com.mtlc.studyplan.social.tabs.RanksTab
import com.mtlc.studyplan.ui.theme.DesignTokens
import com.mtlc.studyplan.ui.theme.LocalSpacing
import com.mtlc.studyplan.ui.theme.StudyPlanTheme
import com.mtlc.studyplan.utils.AvatarPreview
import com.mtlc.studyplan.utils.ImageProcessingUtils
import com.mtlc.studyplan.utils.socialDataStore
import kotlinx.coroutines.launch
import com.mtlc.studyplan.auth.AuthRepository
import com.mtlc.studyplan.auth.FriendsRepository
import android.content.Intent

// import com.mtlc.studyplan.social.components.AwardNotification

private const val AVATAR_HISTORY_LIMIT = 5

private data class AvatarSnapshot(
    val selectedAvatarId: String,
    val customUri: String?
)

private data class PendingAvatarPreview(
    val uri: Uri,
    val preview: AvatarPreview
)

/**
 * Sends a friend invitation email using the device's email app
 */
private fun sendFriendInviteEmail(
    context: android.content.Context,
    friendEmail: String,
    senderUsername: String,
    senderEmail: String
) {
    val appName = context.getString(R.string.app_name)
    val subject = "$senderUsername invited you to join $appName!"
    val body = """
        Hi there!

        $senderUsername ($senderEmail) has invited you to join them on $appName - a study planning app to help you achieve your academic goals!

        Download the app and start studying together:

        📱 Get Started:
        1. Download $appName from the Play Store
        2. Create your account with this email: $friendEmail
        3. Connect with $senderUsername to share progress and compete on leaderboards!

        Features you'll love:
        • Personalized study plans
        • Progress tracking
        • Friend leaderboards
        • Achievement system
        • Study reminders

        See you in the app!

        ---
        This invitation was sent through $appName
    """.trimIndent()

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "message/rfc822" // Email-only apps
        putExtra(Intent.EXTRA_EMAIL, arrayOf(friendEmail))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }

    try {
        val chooserIntent = Intent.createChooser(intent, "Send friend invite via email")
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooserIntent)
    } catch (e: Exception) {
        // If no email app is available, fallback to generic share
        val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Send this invite to $friendEmail:\n\n$body")
        }
        val fallbackChooser = Intent.createChooser(fallbackIntent, "Share invite")
        fallbackChooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(fallbackChooser)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialScreen(
    repository: SocialRepository? = null
) {
    val spacing = LocalSpacing.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }


    // Use persistent repository if none provided
    val socialRepository = repository ?: remember {
        PersistentSocialRepository(
            context = context,
            dataStore = context.socialDataStore,
            database = StudyPlanDatabase.getDatabase(context)
        )
    }

    // Initialize auth repositories for friend invite
    val authRepository = remember { AuthRepository(context) }
    val friendsRepository = remember { FriendsRepository(context) }

    val profile by socialRepository.profile.collectAsState()
    val ranks by socialRepository.ranks.collectAsState()
    val friends by socialRepository.friends.collectAsState()
    val awards by socialRepository.awards.collectAsState()
    val currentAuthUser by authRepository.currentUser.collectAsState(initial = null)
    val canInviteFriends = currentAuthUser != null

    var selectedTab by rememberSaveable { mutableStateOf(SocialTab.Profile) }
    var showUsernameDialog by rememberSaveable { mutableStateOf(false) }
    var pendingUsername by rememberSaveable { mutableStateOf("") }
    var usernameError by remember { mutableStateOf<String?>(null) }

    // Friend invite dialog state
    var showInviteFriendDialog by remember { mutableStateOf(false) }
    var friendEmail by remember { mutableStateOf("") }
    var friendEmailError by remember { mutableStateOf<String?>(null) }
    var isInviting by remember { mutableStateOf(false) }

        // Avatar upload + preview state
    var showAvatarUploadDialog by remember { mutableStateOf(false) }
    val undoStack = remember { mutableStateListOf<AvatarSnapshot>() }
    val redoStack = remember { mutableStateListOf<AvatarSnapshot>() }
    var pendingAvatarPreview by remember { mutableStateOf<PendingAvatarPreview?>(null) }
    var showAvatarPreviewDialog by remember { mutableStateOf(false) }
    var isAvatarOperationRunning by remember { mutableStateOf(false) }
    var isGeneratingPreview by remember { mutableStateOf(false) }
    var previewErrorMessage by remember { mutableStateOf<String?>(null) }
    var latestPreviewRequest by remember { mutableStateOf<String?>(null) }

    val defaultAvatarId = profile.availableAvatars.firstOrNull()?.id ?: "target"

    fun recycleCurrentPreview() {
        pendingAvatarPreview?.preview?.bitmap?.let { bitmap ->
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
        pendingAvatarPreview = null
    }

    fun handlePreviewDismiss(clearError: Boolean = true) {
        showAvatarPreviewDialog = false
        latestPreviewRequest = null
        if (clearError) {
            previewErrorMessage = null
        }
        isGeneratingPreview = false
        recycleCurrentPreview()
    }

    fun mapAvatarError(error: Throwable): String {
        val message = error.message?.lowercase() ?: ""
        return when {
            message.contains("2mb") || message.contains("too large") -> context.getString(R.string.social_avatar_error_size)
            message.contains("format") || message.contains("mime") || message.contains("type") -> context.getString(R.string.social_avatar_error_format)
            message.contains("dimension") -> context.getString(R.string.social_avatar_error_dimensions)
            message.contains("access") || message.contains("permission") -> context.getString(R.string.social_avatar_error_access)
            else -> context.getString(R.string.social_avatar_error_generic, error.message ?: context.getString(R.string.social_avatar_error_generic_fallback))
        }
    }

    fun pushUndoSnapshot(snapshot: AvatarSnapshot) {
        if (undoStack.firstOrNull() != snapshot) {
            undoStack.add(0, snapshot)
            if (undoStack.size > AVATAR_HISTORY_LIMIT) {
                undoStack.removeAt(undoStack.lastIndex)
            }
        }
    }

    fun pushRedoSnapshot(snapshot: AvatarSnapshot) {
        if (redoStack.firstOrNull() != snapshot) {
            redoStack.add(0, snapshot)
            if (redoStack.size > AVATAR_HISTORY_LIMIT) {
                redoStack.removeAt(redoStack.lastIndex)
            }
        }
    }

    suspend fun applySnapshotInternal(snapshot: AvatarSnapshot) {
        if (snapshot.selectedAvatarId == "custom" && !snapshot.customUri.isNullOrBlank()) {
            val path = Uri.parse(snapshot.customUri).path
            if (!path.isNullOrBlank()) {
                socialRepository.updateCustomAvatar(path)
                return
            }
        }
        val targetId = snapshot.selectedAvatarId.takeIf { it.isNotBlank() } ?: defaultAvatarId
        socialRepository.selectAvatar(targetId)
    }

    fun handleUndo() {
        if (undoStack.isEmpty() || isAvatarOperationRunning) return
        val target = undoStack.removeAt(0)
        pushRedoSnapshot(AvatarSnapshot(profile.selectedAvatarId, profile.customAvatarUri))
        isAvatarOperationRunning = true
        scope.launch {
            try {
                applySnapshotInternal(target)
                val message = context.getString(R.string.social_avatar_undo_success)
                snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
            } catch (error: Exception) {
                val message = mapAvatarError(error)
                snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Long)
            } finally {
                isAvatarOperationRunning = false
            }
        }
    }

    fun handleRedo() {
        if (redoStack.isEmpty() || isAvatarOperationRunning) return
        val target = redoStack.removeAt(0)
        pushUndoSnapshot(AvatarSnapshot(profile.selectedAvatarId, profile.customAvatarUri))
        isAvatarOperationRunning = true
        scope.launch {
            try {
                applySnapshotInternal(target)
                val message = context.getString(R.string.social_avatar_redo_success)
                snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
            } catch (error: Exception) {
                val message = mapAvatarError(error)
                snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Long)
            } finally {
                isAvatarOperationRunning = false
            }
        }
    }

    fun fallbackToDefaultAvatar() {
        scope.launch {
            socialRepository.selectAvatar(defaultAvatarId)
        }
    }

    fun handlePreviewApply() {
        val pending = pendingAvatarPreview ?: return
        val snapshotBefore = AvatarSnapshot(profile.selectedAvatarId, profile.customAvatarUri)
        isAvatarOperationRunning = true
        scope.launch {
            try {
                socialRepository.uploadCustomAvatar(pending.uri.toString())
                pushUndoSnapshot(snapshotBefore)
                redoStack.clear()
                val message = context.getString(R.string.social_avatar_upload_success)
                snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
                handlePreviewDismiss()
            } catch (error: Exception) {
                val message = mapAvatarError(error)
                snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Long)
                handlePreviewDismiss()
                fallbackToDefaultAvatar()
            } finally {
                isAvatarOperationRunning = false
            }
        }
    }

    fun startPreview(uri: Uri) {
        recycleCurrentPreview()
        isGeneratingPreview = true
        previewErrorMessage = null
        showAvatarPreviewDialog = true
        val requestKey = uri.toString()
        latestPreviewRequest = requestKey
        scope.launch {
            val result = ImageProcessingUtils.generatePreviewImage(context, uri)
            if (latestPreviewRequest != requestKey) {
                result.onSuccess { preview: AvatarPreview ->
                    if (!preview.bitmap.isRecycled) {
                        preview.bitmap.recycle()
                    }
                }
                return@launch
            }
            result.fold(
                onSuccess = { preview ->
                    pendingAvatarPreview = PendingAvatarPreview(uri = uri, preview = preview)
                    isGeneratingPreview = false
                },
                onFailure = { error ->
                    previewErrorMessage = mapAvatarError(error)
                    isGeneratingPreview = false
                    recycleCurrentPreview()
                }
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            latestPreviewRequest = null
            recycleCurrentPreview()
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { picked ->
        showAvatarUploadDialog = false
        picked?.let { startPreview(it) } ?: run {
            if (!showAvatarPreviewDialog) {
                previewErrorMessage = null
            }
        }
    }
    // var notificationAward by remember { mutableStateOf<com.mtlc.studyplan.data.social.Award?>(null) }

    // Deep link handling simplified - NavSocialTab removed
    // If deep links are needed, they can be handled via string parameters
    // and mapped to SocialTab enum directly

    // Enforce username selection if missing
    LaunchedEffect(currentAuthUser?.username, profile.username) {
        val authUsername = currentAuthUser?.username?.trim().orEmpty()

        if (currentAuthUser == null) {
            if (profile.username.isNotBlank()) {
                socialRepository.updateUsername("")
            }
            showUsernameDialog = false
            pendingUsername = ""
            usernameError = null
        } else if (authUsername.isBlank()) {
            showUsernameDialog = true
            pendingUsername = ""
            usernameError = null
        } else {
            if (profile.username != authUsername) {
                socialRepository.updateUsername(authUsername)
            }
            showUsernameDialog = false
            pendingUsername = authUsername
            usernameError = null
        }
    }


    Column(modifier = Modifier.fillMaxSize()) {

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Snackbar host
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            // Username required dialog overlay
            if (showUsernameDialog) {
                UsernameRequiredDialog(
                    value = pendingUsername,
                    onValueChange = { input ->
                        pendingUsername = input
                        usernameError = validateUsername(input)
                    },
                    error = usernameError,
                    onConfirm = {
                        val err = validateUsername(pendingUsername)
                        usernameError = err
                        if (err == null) {
                            val activeUser = currentAuthUser
                            if (activeUser == null) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = context.getString(R.string.social_login_required),
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            } else {
                                val sanitized = pendingUsername.trim()
                                scope.launch {
                                    val updateResult = authRepository.updateUsername(sanitized)
                                    if (updateResult.isSuccess) {
                                        socialRepository.updateUsername(sanitized)
                                        showUsernameDialog = false
                                        val usernameSetMessage = context.getString(R.string.social_username_set_success)
                                            .replace("{username}", sanitized)
                                        snackbarHostState.showSnackbar(
                                            message = usernameSetMessage,
                                            duration = SnackbarDuration.Short
                                        )
                                    } else {
                                        snackbarHostState.showSnackbar(
                                            message = context.getString(R.string.social_username_save_error),
                                            duration = SnackbarDuration.Long
                                        )
                                    }
                                }
                            }
                        }
                    },
                    onDismiss = { /* keep open until a valid username saved */ },
                    confirmEnabled = usernameError == null && pendingUsername.isNotBlank()
                )
            }

            // Avatar upload dialog
            if (showAvatarUploadDialog) {
                AvatarUploadDialog(
                    onDismiss = { showAvatarUploadDialog = false },
                    onGallerySelected = {
                        imagePickerLauncher.launch("image/*")
                    },
                    onCameraSelected = {
                        // For now, just show a message that camera feature is coming soon
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Camera feature coming soon! Please use gallery for now.",
                                duration = SnackbarDuration.Short
                            )
                        }
                        showAvatarUploadDialog = false
                    }
                )
            }
            if (showAvatarPreviewDialog) {
                AvatarPreviewDialog(
                    preview = pendingAvatarPreview,
                    isLoading = isGeneratingPreview,
                    isApplying = isAvatarOperationRunning,
                    errorMessage = previewErrorMessage,
                    canUndo = undoStack.isNotEmpty(),
                    canRedo = redoStack.isNotEmpty(),
                    onDismiss = { handlePreviewDismiss() },
                    onApply = { handlePreviewApply() },
                    onRetry = {
                        handlePreviewDismiss(clearError = false)
                        imagePickerLauncher.launch("image/*")
                    },
                    onUseDefault = {
                        handlePreviewDismiss()
                        fallbackToDefaultAvatar()
                    },
                    onUndo = { handleUndo() },
                    onRedo = { handleRedo() }
                )
            }

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Fixed Social Hub Top Bar
                SocialHubTopBar(
                    canInvite = canInviteFriends,
                    onInviteClick = {
                        if (canInviteFriends) {
                            showInviteFriendDialog = true
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = context.getString(R.string.social_login_required),
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    }
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = spacing.md),
                    verticalArrangement = Arrangement.spacedBy(spacing.md)
                ) {
                    SocialSegmentedTabs(
                selected = selectedTab,
                onTabSelected = { selectedTab = it }
            )

            when (selectedTab) {
                SocialTab.Profile -> {
                    ProfileTab(
                        profile = profile,
                        onAvatarSelected = { id ->
                            if (id != profile.selectedAvatarId) {
                                val snapshotBefore = AvatarSnapshot(profile.selectedAvatarId, profile.customAvatarUri)
                                isAvatarOperationRunning = true
                                scope.launch {
                                    try {
                                        socialRepository.selectAvatar(id)
                                        pushUndoSnapshot(snapshotBefore)
                                        redoStack.clear()
                                    } catch (error: Exception) {
                                        val message = mapAvatarError(error)
                                        snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Long)
                                    } finally {
                                        isAvatarOperationRunning = false
                                    }
                                }
                            }
                        },
                        onUploadAvatarClick = {
                            if (!isAvatarOperationRunning && !isGeneratingPreview) {
                                showAvatarUploadDialog = true
                            }
                        },
                        onUndoAvatar = { handleUndo() },
                        onRedoAvatar = { handleRedo() },
                        canUndo = undoStack.isNotEmpty(),
                        canRedo = redoStack.isNotEmpty(),
                        isAvatarBusy = isAvatarOperationRunning || isGeneratingPreview
                    )

                }
                SocialTab.Ranks -> RanksTab(ranks = ranks)
                SocialTab.Friends -> FriendsTab(
                    friends = friends,
                    canInviteFriends = canInviteFriends,
                    onFriendSelected = { /* friend -> showFriendProfileSnackbar(friend, snackbarHostState, scope, context) */ },
                    onAddFriend = {
                        if (canInviteFriends) {
                            showInviteFriendDialog = true
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = context.getString(R.string.social_login_required),
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    }
                )
                SocialTab.Awards -> AwardsTab(awards = awards)
            }

            // Section removed per product requirements
                }
            }

        }
    }

    // Invite Friend Dialog
    if (showInviteFriendDialog) {
        AlertDialog(
            onDismissRequest = {
                showInviteFriendDialog = false
                friendEmail = ""
                friendEmailError = null
            },
            title = { Text("Invite Friend") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Enter your friend's email address to send them a friend request.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = friendEmail,
                        onValueChange = {
                            friendEmail = it
                            friendEmailError = null
                        },
                        label = { Text("Friend's Email") },
                        isError = friendEmailError != null,
                        supportingText = friendEmailError?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isInviting
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Validate email
                        if (friendEmail.isBlank()) {
                            friendEmailError = "Email is required"
                            return@TextButton
                        }
                        if (!AuthRepository.isValidEmail(friendEmail)) {
                            friendEmailError = "Please enter a valid email address"
                            return@TextButton
                        }

                        // Check if user is logged in
                        val user = currentAuthUser
                        if (user == null) {
                            friendEmailError = context.getString(R.string.social_login_required)
                            return@TextButton
                        }

                        isInviting = true
                        scope.launch {
                            try {
                                // Save friend request locally
                                val result = friendsRepository.sendFriendRequest(
                                    currentUserId = user.id,
                                    currentUserEmail = user.email,
                                    currentUsername = user.username,
                                    friendEmail = friendEmail
                                )

                                if (result.isSuccess) {
                                    // Open email app to send the actual invite
                                    sendFriendInviteEmail(
                                        context = context,
                                        friendEmail = friendEmail,
                                        senderUsername = user.username,
                                        senderEmail = user.email
                                    )

                                    snackbarHostState.showSnackbar(
                                        message = "Opening email app to send invite to $friendEmail",
                                        duration = SnackbarDuration.Short
                                    )
                                    showInviteFriendDialog = false
                                    friendEmail = ""
                                } else {
                                    snackbarHostState.showSnackbar(
                                        message = "Failed to create friend request: ${result.exceptionOrNull()?.message}",
                                        duration = SnackbarDuration.Long
                                    )
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar(
                                    message = "Error: ${e.message}",
                                    duration = SnackbarDuration.Long
                                )
                            } finally {
                                isInviting = false
                            }
                        }
                    },
                    enabled = !isInviting
                ) {
                    if (isInviting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Send Invite")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showInviteFriendDialog = false
                        friendEmail = ""
                        friendEmailError = null
                    },
                    enabled = !isInviting
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SocialHubTopBar(
    canInvite: Boolean,
    onInviteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val title = stringResource(R.string.social_hub_title)
    val subtitle = stringResource(R.string.social_hub_subtitle)
    val inviteLabel = stringResource(R.string.topbar_invite_action)
    val loginPrompt = stringResource(R.string.social_login_required)

    val disabledContainer = MaterialTheme.colorScheme.surfaceVariant
    val disabledContent = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFE9F5E9),
                            Color(0xFFD1ECF1)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(end = 12.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF4A6741),
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        lineHeight = 18.sp,
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onInviteClick,
                        enabled = canInvite,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF2E7D32),
                            disabledContainerColor = disabledContainer,
                            disabledContentColor = disabledContent
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (canInvite) Color(0xFF2E7D32) else disabledContent
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = inviteLabel,
                            fontWeight = FontWeight.Medium,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    if (!canInvite) {
                        Text(
                            text = loginPrompt,
                            style = MaterialTheme.typography.labelSmall,
                            color = disabledContent,
                            textAlign = TextAlign.End,
                            modifier = Modifier.padding(top = spacing.xs)
                        )
                    }
                }
            }
        }
    }
}

fun showFriendProfileSnackbar(
    friend: Friend,
    snackbarHostState: SnackbarHostState,
    scope: kotlinx.coroutines.CoroutineScope,
    context: android.content.Context
) {
    scope.launch {
        val message = context.getString(R.string.social_friend_profile_stub, friend.name)
        snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
    }
}

@Composable
fun HighlightChip(label: String, value: String, modifier: Modifier = Modifier) {
    val prussianBlue = Color(0xFF003153)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DesignTokens.SurfaceContainer,
        tonalElevation = 1.dp,
        modifier = modifier
            .border(BorderStroke(1.dp, prussianBlue), RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun ProfileSection(
    profile: SocialProfile,
    onAvatarSelected: (String) -> Unit,
    onUsernameSave: (String) -> Unit,
    onUploadAvatarClick: () -> Unit
) {
    val spacing = LocalSpacing.current
    val prussianBlue = Color(0xFF003153)

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .border(BorderStroke(1.dp, prussianBlue), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            // Profile header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = stringResource(id = R.string.social_profile_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Username section (editable)
            var editing by rememberSaveable(profile.username) { mutableStateOf(false) }
            var input by rememberSaveable(profile.username) { mutableStateOf(profile.username) }
            var error by remember { mutableStateOf<String?>(null) }
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Username",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (!editing) {
                        TextButton(onClick = { editing = true; input = profile.username; error = null }) {
                            Icon(imageVector = Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(text = "Edit", modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                }
                if (editing) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = {
                            input = it.lowercase()
                            error = null // validateUsername(input)
                        },
                        singleLine = true,
                        isError = error != null,
                        supportingText = {
                            when (error) {
                                "invalid" -> Text(text = stringResource(id = R.string.social_username_error))
                                else -> Text(text = stringResource(id = R.string.social_username_hint))
                            }
                        }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        TextButton(onClick = {
                            // Username validation disabled for now
                            error = null
                            // Always proceed since validation is disabled
                            onUsernameSave(input.trim())
                            editing = false
                        }, enabled = error == null && input.isNotBlank()) {
                            Text(text = stringResource(id = R.string.save))
                        }
                        TextButton(onClick = { editing = false; input = profile.username; error = null }) {
                            Text(text = stringResource(id = R.string.cancel))
                        }
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .border(BorderStroke(1.dp, Color(0xFF003153)), RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = profile.username,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = stringResource(id = R.string.social_username_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            // Avatar section
            Text(
                text = stringResource(id = R.string.social_choose_avatar),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            // Avatar grid (2x4 layout)
            val avatarOptions = profile.availableAvatars
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                avatarOptions.chunked(4).forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        row.forEach { option ->
                            val isSelected = option.id == profile.selectedAvatarId
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clickable { onAvatarSelected(option.id) },
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else
                                    MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(12.dp),
                                border = if (isSelected)
                                    androidx.compose.foundation.BorderStroke(
                                        2.dp,
                                        MaterialTheme.colorScheme.primary
                                    )
                                else null
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "🎯", // avatarEmoji(option.id),
                                        style = MaterialTheme.typography.headlineSmall
                                    )
                                    if (isSelected) {
                                        Surface(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(4.dp)
                                                .size(18.dp),
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "✓",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        // Add empty spaces if row doesn't have 4 items
                        repeat(4 - row.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // Upload custom avatar button
            FilledTonalButton(
                onClick = onUploadAvatarClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.Image,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(id = R.string.social_upload_avatar),
                    modifier = Modifier.padding(start = spacing.xs)
                )
            }

            // Preview section
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .border(BorderStroke(1.dp, Color(0xFF003153)), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    // Avatar preview
                    AvatarDisplay(
                        profile = profile,
                        size = 40.dp
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile.username,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Study Level: Intermediate",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                        )
                    }
                }
            }
        }
    }
}

// Deprecated helper removed

@Composable
fun AvatarDisplay(
    profile: SocialProfile,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 40.dp
) {
    if (profile.selectedAvatarId == "custom" && !profile.customAvatarUri.isNullOrEmpty()) {
        // Display custom uploaded image
        Image(
            painter = rememberAsyncImagePainter(
                model = Uri.parse(profile.customAvatarUri)
            ),
            contentDescription = "Custom avatar",
            modifier = modifier
                .size(size)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        // Display emoji avatar
        Surface(
            modifier = modifier.size(size),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = avatarEmoji(profile.selectedAvatarId),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

fun avatarEmoji(id: String): String = when (id) {
    "target" -> "🎯"
    "rocket" -> "🚀"
    "star" -> "⭐"
    "flame" -> "🔥"
    "diamond" -> "💎"
    "trophy" -> "🏆"
    "puzzle" -> "🧩"
    "sun" -> "🌞"
    "custom" -> "📷"
    else -> "🙂"
}

fun validateUsername(input: String): String? {
    val trimmed = input.trim()
    val regex = Regex("^[a-z0-9_]{3,20}$")
    return when {
        trimmed.isEmpty() -> ""
        !regex.matches(trimmed) -> "invalid"
        else -> null
    }
}

@Composable
fun UsernameRequiredDialog(
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmEnabled: Boolean
) {
    val spacing = LocalSpacing.current
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = confirmEnabled) {
                Text(text = stringResource(id = R.string.save))
            }
        },
        title = { Text(text = stringResource(id = R.string.social_choose_username_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { onValueChange(it.lowercase()) },
                    singleLine = true,
                    label = { Text(stringResource(id = R.string.social_username_label)) },
                    supportingText = {
                        when (error) {
                            "invalid" -> Text(stringResource(id = R.string.social_username_error))
                            else -> Text(stringResource(id = R.string.social_username_hint))
                        }
                    }
                )
            }
        }
    )
}

@Composable
private fun AvatarPreviewDialog(
    preview: PendingAvatarPreview?,
    isLoading: Boolean,
    isApplying: Boolean,
    errorMessage: String?,
    canUndo: Boolean,
    canRedo: Boolean,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
    onRetry: () -> Unit,
    onUseDefault: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit
) {
    val spacing = LocalSpacing.current
    val imageBitmap = remember(preview?.preview?.bitmap) {
        preview?.preview?.bitmap?.takeIf { !it.isRecycled }?.asImageBitmap()
    }
    val hasPreview = preview != null && imageBitmap != null

    val sizeText = if (preview != null) {
        val size = preview.preview.estimatedFileSize
        when {
            size >= 1_048_576L -> stringResource(
                id = R.string.social_avatar_size_mb,
                size.toDouble() / (1024.0 * 1024.0)
            )
            size >= 1024L -> stringResource(
                id = R.string.social_avatar_size_kb,
                size / 1024
            )
            size > 0L -> stringResource(
                id = R.string.social_avatar_size_kb,
                1
            )
            else -> stringResource(id = R.string.social_avatar_size_unknown)
        }
    } else {
        stringResource(id = R.string.social_avatar_size_unknown)
    }

    val resolutionText = if (preview != null) {
        stringResource(
            id = R.string.social_avatar_resolution,
            preview.preview.width,
            preview.preview.height
        )
    } else {
        stringResource(id = R.string.social_avatar_resolution_unknown)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.social_avatar_preview_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onUndo,
                        enabled = canUndo && !isLoading && !isApplying
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Undo,
                            contentDescription = stringResource(id = R.string.social_avatar_undo_cd)
                        )
                    }
                    IconButton(
                        onClick = onRedo,
                        enabled = canRedo && !isLoading && !isApplying
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Redo,
                            contentDescription = stringResource(id = R.string.social_avatar_redo_cd)
                        )
                    }
                }

                when {
                    isLoading -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Text(text = stringResource(id = R.string.social_avatar_preview_loading))
                        }
                    }
                    errorMessage != null -> {
                        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                            Text(
                                text = errorMessage,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            FilledTonalButton(
                                onClick = onRetry,
                                enabled = !isApplying,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = stringResource(id = R.string.social_avatar_retry))
                            }
                        }
                    }
                    hasPreview -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(spacing.sm)
                        ) {
                            Surface(
                                shape = CircleShape,
                                tonalElevation = 2.dp
                            ) {
                                Image(
                                    bitmap = imageBitmap,
                                    contentDescription = stringResource(id = R.string.social_avatar_preview_cd),
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Text(
                                text = stringResource(id = R.string.social_avatar_preview_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = resolutionText,
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = sizeText,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                    else -> {
                        Text(
                            text = stringResource(id = R.string.social_avatar_no_preview),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onApply,
                enabled = !isLoading && !isApplying && errorMessage == null && hasPreview
            ) {
                if (isApplying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(text = stringResource(id = R.string.social_avatar_apply))
                }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                TextButton(
                    onClick = onUseDefault,
                    enabled = !isLoading && !isApplying
                ) {
                    Text(text = stringResource(id = R.string.social_avatar_use_default))
                }
                TextButton(
                    onClick = onDismiss,
                    enabled = !isApplying
                ) {
                    Text(text = stringResource(id = R.string.social_avatar_cancel))
                }
            }
        }
    )
}
@Composable
fun AvatarUploadDialog(
    onDismiss: () -> Unit,
    onGallerySelected: () -> Unit,
    onCameraSelected: () -> Unit
) {
    val spacing = LocalSpacing.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.social_upload_avatar),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                Text(
                    text = "Choose how you'd like to add your avatar:",
                    style = MaterialTheme.typography.bodyMedium
                )

                // Gallery option
                FilledTonalButton(
                    onClick = onGallerySelected,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Image,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Choose from Gallery")
                }

                // Camera option
                FilledTonalButton(
                    onClick = onCameraSelected,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person, // Using Person as placeholder for camera
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Take Photo")
                }

                Text(
                    text = "• Image must be smaller than 2MB\n• Recommended size: 512x512 pixels\n• Supported formats: JPG, PNG",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


@Preview(showBackground = true)
@Composable
private fun SocialScreenPreview() {
    StudyPlanTheme {
        SocialScreen()
    }
}







