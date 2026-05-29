package com.example.kampus.ui.post

import android.net.Uri
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.kampus.ui.theme.ThemeController
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.kampus.ui.events.rememberMediaPickers
import com.example.kampus.ui.feed.PostItem
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.navigationBarsPadding
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.foundation.layout.navigationBarsPadding
import com.google.firebase.auth.FirebaseAuth

@Composable
fun PostDetailScreen(
    postId: Int,
    onBack: () -> Unit,
    openComposer: Boolean = false,
    viewModel: PostViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    var commentText by remember { mutableStateOf("") }
    var commentImageUri by remember { mutableStateOf<Uri?>(null) }
    var replyingToCommentId by remember { mutableStateOf<String?>(null) }
    var replyingToName by remember { mutableStateOf<String?>(null) }
    var commentsState by remember { mutableStateOf<List<PostComment>>(emptyList()) }
    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid.orEmpty() }
    val commentPickers = rememberMediaPickers(onPhotoSelected = { commentImageUri = it })
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(postId) {
        viewModel.observePost(postId)
    }

    LaunchedEffect(state.comments) {
        commentsState = state.comments
    }

    LaunchedEffect(openComposer) {
        if (openComposer) {
            kotlinx.coroutines.delay(400)
            try { focusRequester.requestFocus() } catch (_: Exception) {}
            keyboardController?.show()
        }
    }

    androidx.compose.material3.Scaffold(
        containerColor = colors.background,
        bottomBar = {
            if (state.post != null) {
                Surface(
                    color = colors.surface,
                    tonalElevation = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.outline.copy(alpha = 0.2f))
                ) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        CommentComposer(
                            text = commentText,
                            imageUri = commentImageUri,
                            replyingToName = replyingToName,
                            focusRequester = focusRequester,
                            onTextChange = { commentText = it },
                            onPickPhoto = { commentPickers.pickPhoto() },
                            onCancelReply = {
                                replyingToCommentId = null
                                replyingToName = null
                            },
                            onRemovePhoto = { commentImageUri = null },
                            onSubmit = {
                                val text = commentText.trim()
                                val hasPhoto = commentImageUri != null
                                if (text.isBlank() && !hasPhoto) return@CommentComposer

                                scope.launch {
                                    val result = if (replyingToCommentId == null) {
                                        viewModel.addComment(postId, text, commentImageUri)
                                    } else {
                                        viewModel.addReply(postId, replyingToCommentId.orEmpty(), text, commentImageUri)
                                    }

                                    if (result.isSuccess) {
                                        commentText = ""
                                        commentImageUri = null
                                        replyingToCommentId = null
                                        replyingToName = null
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(paddingValues)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceVariant)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.onSurface,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(text = "Post Detail", color = colors.onBackground, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colors.primary)
                    }
                }

                state.post == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = state.error ?: "Post not found",
                            color = colors.onSurfaceVariant,
                            fontSize = 14.sp,
                        )
                    }
                }

                else -> {
                    val post = state.post!!
                    val replyingToComment = replyingToCommentId?.let { commentId ->
                        commentsState.firstNotNullOfOrNull { comment -> comment.findById(commentId) }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(colors.surface)
                                    .border(1.dp, colors.outline.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    AvatarChip(
                                        emoji = post.avatar,
                                        imageUrl = post.profileImageUrl.takeIf { it.isNotBlank() },
                                        size = 42.dp,
                                        userId = post.authorId,
                                        userId = post.authorId,
                                    )
                                    Column {
                                        Text(text = post.author, color = colors.onSurface, fontWeight = FontWeight.SemiBold)
                                        Text(text = formatPostTime(post), color = colors.onSurfaceVariant, fontSize = 12.sp)
                                    }
                                }

                                val displayContent = if (post.sharedOriginalPostId != null) {
                                    val sharedPrefix = "Shared from ${post.sharedOriginalAuthor}"
                                    val stripped = post.content
                                        .substringBefore("\n\n$sharedPrefix")
                                        .substringBefore("\n$sharedPrefix")
                                        .trim()
                                    stripped
                                } else {
                                    post.content
                                }

                                if (displayContent.isNotBlank()) {
                                    Text(text = displayContent, color = colors.onSurface, fontSize = 15.sp, lineHeight = 22.sp)
                                }

                                if (post.sharedOriginalPostId != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    SharedOriginalCard(
                                        author = post.sharedOriginalAuthor ?: "Unknown",
                                        authorId = post.sharedOriginalAuthorId.orEmpty(),
                                        avatar = post.sharedOriginalAvatar ?: post.avatar,
                                        profileImageUrl = post.sharedOriginalProfileImageUrl.orEmpty(),
                                        time = post.sharedOriginalTime ?: "now",
                                        content = post.sharedOriginalContent.orEmpty(),
                                        mediaUris = post.sharedOriginalMediaUris,
                                        mediaTypes = post.sharedOriginalMediaTypes,
                                        mediaEmojis = post.sharedOriginalMediaEmojis,
                                        likes = post.sharedOriginalLikes ?: 0,
                                        comments = post.sharedOriginalComments ?: 0,
                                        shares = post.sharedOriginalShares ?: 0,
                                        isVerified = post.sharedOriginalIsVerified ?: false,
                                        onClick = {
                                            viewModel.observePost(post.sharedOriginalPostId)
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }

                                val metadataLines = postMetadataLines(post)
                                if (metadataLines.isNotEmpty()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        metadataLines.forEach { line ->
                                            Text(text = line, color = colors.onSurfaceVariant, fontSize = 12.sp)
                                        }
                                    }
                                }

                                Text(
                                    text = "${post.likes} likes • ${commentsState.sumOf { 1 + it.replies.size }} comments • ${post.shares} shares",
                                    color = colors.onSurfaceVariant,
                                    fontSize = 12.sp,
                                )
                            }
                        }

                        if (commentsState.isEmpty()) {
                            item {
                                Text(
                                    text = "No comments yet. Be the first to reply.",
                                    color = colors.onSurfaceVariant,
                                    fontSize = 13.sp,
                                )
                            }
                        } else {
                            items(commentsState, key = { it.id }) { comment ->
                                CommentThread(
                                    comment = comment,
                                    onReply = { targetComment ->
                                        replyingToCommentId = targetComment.id
                                        replyingToName = targetComment.username
                                    },
                                    onLike = { targetComment ->
                                        val previousComments = commentsState
                                        commentsState = commentsState.map { current ->
                                            if (current.id == targetComment.id) {
                                                current.copy(
                                                    likedByCurrentUser = !current.likedByCurrentUser,
                                                    likesCount = if (current.likedByCurrentUser) current.likesCount - 1 else current.likesCount + 1,
                                                )
                                            } else {
                                                current.copy(
                                                    replies = current.replies.map { reply ->
                                                        if (reply.id == targetComment.id) {
                                                            reply.copy(
                                                                likedByCurrentUser = !reply.likedByCurrentUser,
                                                                likesCount = if (reply.likedByCurrentUser) reply.likesCount - 1 else reply.likesCount + 1,
                                                            )
                                                        } else {
                                                            reply
                                                        }
                                                    }
                                                )
                                            }
                                        }

                                        scope.launch {
                                            val result = viewModel.toggleCommentLike(postId, targetComment.id)
                                            if (result.isFailure) {
                                                commentsState = previousComments
                                            }
                                        }
                                    },
                                    onDelete = { targetComment ->
                                        val previousComments = commentsState
                                        commentsState = commentsState.mapNotNull { current ->
                                            when {
                                                current.id == targetComment.id -> null
                                                current.replies.any { reply -> reply.id == targetComment.id } -> {
                                                    current.copy(replies = current.replies.filterNot { reply -> reply.id == targetComment.id })
                                                }
                                                else -> current
                                            }
                                        }

                                        scope.launch {
                                            val result = viewModel.deleteComment(postId, targetComment.id)
                                            if (result.isFailure) {
                                                commentsState = previousComments
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentComposer(
    text: String,
    imageUri: Uri?,
    replyingToName: String?,
    focusRequester: FocusRequester = remember { FocusRequester() },
    onTextChange: (String) -> Unit,
    onPickPhoto: () -> Unit,
    onCancelReply: () -> Unit,
    onRemovePhoto: () -> Unit,
    onSubmit: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid.orEmpty() }
    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid.orEmpty() }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Replying to label if replying
        // Replying to label if replying
        if (replyingToName != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.primary.copy(alpha = 0.10f))
                    .border(1.dp, colors.primary.copy(alpha = 0.20f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                    .border(1.dp, colors.primary.copy(alpha = 0.20f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("Replying to @$replyingToName", color = colors.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Cancel",
                    tint = colors.primary,
                    modifier = Modifier
                        .size(14.dp)
                        .clickable(onClick = onCancelReply)
                )
            }
        }

        // Image preview if any
                Text("Replying to @$replyingToName", color = colors.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Cancel",
                    tint = colors.primary,
                    modifier = Modifier
                        .size(14.dp)
                        .clickable(onClick = onCancelReply)
                )
            }
        }

        // Image preview if any
        if (imageUri != null) {
            Box(
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, colors.outline.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, colors.outline.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
            ) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Photo preview",
                    modifier = Modifier.fillMaxSize(),
                    contentDescription = "Photo preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                Box(
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable(onClick = onRemovePhoto),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Remove",
                        tint = Color.White,
                        modifier = Modifier.size(10.dp),
                    )
                }
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Remove",
                        tint = Color.White,
                        modifier = Modifier.size(10.dp),
                    )
                }
            }
        }

        // Main input row
        // Main input row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Current User Avatar in real-time
            AvatarChip(
                emoji = "👤",
                imageUrl = null,
                size = 36.dp,
                userId = currentUserId,
            )

            // Text field and photo picker inside a search-bar style container
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surfaceVariant.copy(alpha = 0.5f))
                    .border(1.dp, colors.outline.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                androidx.compose.foundation.text.BasicTextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = colors.onSurface,
                        fontSize = 14.sp
                    ),
                    maxLines = 4,
                    decorationBox = { inner ->
                        if (text.isEmpty()) {
                            Text(
                                text = if (replyingToName == null) "Write a comment..." else "Reply...",
                                color = colors.onSurfaceVariant.copy(alpha = 0.6f),
                                fontSize = 14.sp
                            )
                        }
                        inner()
                    }
                )

                // Pick photo icon inside the text input box
                Icon(
                    imageVector = Icons.Outlined.Image,
                    contentDescription = "Add photo",
                    tint = colors.primary,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(onClick = onPickPhoto)
                )
            }

            // Send icon button
            val enabled = text.isNotBlank() || imageUri != null
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (enabled) colors.primary else colors.surfaceVariant)
                    .clickable(enabled = enabled, onClick = onSubmit),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Send,
                    contentDescription = "Send",
                    tint = if (enabled) Color.White else colors.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun CommentThread(
    comment: PostComment,
    onReply: (PostComment) -> Unit,
    onLike: (PostComment) -> Unit,
    onDelete: (PostComment) -> Unit = {},
    depth: Int = 0,
) {
    val colors = MaterialTheme.colorScheme
    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid.orEmpty() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 14).dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(colors.surface)
                .border(1.dp, colors.outline.copy(alpha = 0.28f), RoundedCornerShape(18.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AvatarChip(
                    emoji = comment.userAvatar,
                    imageUrl = comment.userProfileImageUrl.takeIf { it.isNotBlank() },
                    size = 34.dp,
                    userId = comment.userId,
                    userId = comment.userId,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(comment.username, color = colors.onSurface, fontWeight = FontWeight.SemiBold)
                    Text(formatPostTime(comment.createdAt), color = colors.onSurfaceVariant, fontSize = 11.sp)
                }
            }

            Text(comment.text, color = colors.onSurface, fontSize = 14.sp, lineHeight = 20.sp)

            if (!comment.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = comment.imageUrl,
                    contentDescription = "Comment image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                ActionChip(
                    label = if (comment.likedByCurrentUser) "❤️ ${comment.likesCount}" else "🤍 ${comment.likesCount}",
                    active = comment.likedByCurrentUser,
                    onClick = { onLike(comment) },
                )
                ActionChip(label = "Reply", active = false, onClick = { onReply(comment) })
                if (comment.userId == currentUserId) {
                    ActionChip(label = "Delete", active = false, onClick = { onDelete(comment) })
                }
            }
        }

        comment.replies.forEach { reply ->
            CommentThread(
                comment = reply,
                onReply = onReply,
                onLike = onLike,
                onDelete = onDelete,
                depth = depth + 1,
            )
        }
    }
}

@Composable
private fun ActionChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (active) colors.primary.copy(alpha = 0.12f) else colors.surfaceVariant.copy(alpha = 0.35f))
            .border(1.dp, if (active) colors.primary.copy(alpha = 0.24f) else colors.outline.copy(alpha = 0.28f), RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label, color = if (active) colors.primary else colors.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AvatarChip(
    emoji: String,
    imageUrl: String?,
    size: androidx.compose.ui.unit.Dp,
    userId: String? = null,
    userId: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    var liveImageUrl by remember(imageUrl, userId) { mutableStateOf(imageUrl) }

    androidx.compose.runtime.DisposableEffect(userId) {
        var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
        if (!userId.isNullOrBlank()) {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            listenerRegistration = db.collection("users").document(userId)
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null && snapshot.exists()) {
                        val dbUrl = snapshot.getString("profileImageUrl")
                        if (!dbUrl.isNullOrBlank()) {
                            liveImageUrl = dbUrl
                        }
                    }
                }
        }
        onDispose {
            listenerRegistration?.remove()
        }
    }

    if (!liveImageUrl.isNullOrBlank()) {
    var liveImageUrl by remember(imageUrl, userId) { mutableStateOf(imageUrl) }

    androidx.compose.runtime.DisposableEffect(userId) {
        var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
        if (!userId.isNullOrBlank()) {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            listenerRegistration = db.collection("users").document(userId)
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null && snapshot.exists()) {
                        val dbUrl = snapshot.getString("profileImageUrl")
                        if (!dbUrl.isNullOrBlank()) {
                            liveImageUrl = dbUrl
                        }
                    }
                }
        }
        onDispose {
            listenerRegistration?.remove()
        }
    }

    if (!liveImageUrl.isNullOrBlank()) {
        AsyncImage(
            model = liveImageUrl,
            model = liveImageUrl,
            contentDescription = null,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(colors.surfaceVariant),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(colors.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = emoji.ifBlank { "👤" }, fontSize = (size.value * 0.42f).sp)
        }
    }
}

private fun formatPostTime(post: PostItem): String {
    val timestamp = post.timestamp
    return if (timestamp > 0L) {
        DateUtils.getRelativeTimeSpanString(timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
    } else {
        post.time
    }
}

private fun formatPostTime(timestamp: Long): String {
    return if (timestamp > 0L) {
        DateUtils.getRelativeTimeSpanString(timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
    } else {
        "now"
    }
}

private fun postMetadataLines(post: PostItem): List<String> = buildList {
    post.feelingEmoji?.takeIf { it.isNotBlank() }?.let { add("Feeling $it") }
    post.feeling?.takeIf { it.isNotBlank() }?.let { feeling ->
        if (post.feelingEmoji.isNullOrBlank()) add("Feeling $feeling")
    }
    post.location?.takeIf { it.isNotBlank() }?.let { add("📍 $it") }
    if (post.tags.isNotEmpty()) add(post.tags.take(4).joinToString(" "))
    if (post.taggedPeople.isNotEmpty()) add("With ${post.taggedPeople.take(3).joinToString(", ")}")
    if (post.visibility != PostItem.PostVisibility.PUBLIC) {
        add("Visible to ${post.visibility.name.lowercase().replaceFirstChar { it.uppercaseChar() }}")
    }
}

private fun PostComment.findById(targetId: String): PostComment? {
    if (id == targetId) return this
    return replies.firstNotNullOfOrNull { it.findById(targetId) }
}

@Composable
fun SharedOriginalCard(
    author: String,
    authorId: String,
    avatar: String,
    profileImageUrl: String,
    time: String,
    content: String,
    mediaUris: List<android.net.Uri>,
    mediaTypes: List<PostItem.MediaType>,
    mediaEmojis: List<String>,
    likes: Int,
    comments: Int,
    shares: Int,
    isVerified: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val isDark = ThemeController.isDark

    // Card press animation
    var isPressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.988f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "fb_card_scale",
    )

    // Match Facebook's dark embedded card color
    val cardBg = if (isDark) Color(0xFF1C1E22) else Color(0xFFF2F3F5)
    val cardBorder = if (isDark) Color(0xFF3A3B3C) else Color(0xFFCDD0D5)
    val textColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF111827)
    val grayText = if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val lightGrayText = if (isDark) Color(0xFFE5E7EB) else Color(0xFF374151)
    val blueColor = ThemeController.accent.color

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = cardScale; scaleY = cardScale }
            .clip(RoundedCornerShape(12.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        // Header
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 10.dp, end = 12.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AvatarChip(emoji = avatar, imageUrl = profileImageUrl.takeIf { it.isNotBlank() }, size = 36.dp, userId = authorId)
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        author,
                        color = textColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (isVerified) {
                        Icon(Icons.Default.Verified, "Verified", tint = blueColor, modifier = Modifier.size(13.dp))
                    }
                }
                Text(time, color = grayText, fontSize = 11.sp)
            }
        }

        // Caption
        if (content.isNotBlank()) {
            Text(
                content,
                color = lightGrayText,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }

        // Media
        when {
            mediaUris.isNotEmpty() -> {
                val firstUri = mediaUris.first()
                val firstType = mediaTypes.getOrNull(0) ?: PostItem.MediaType.IMAGE
                Spacer(Modifier.height(6.dp))
                if (firstType == PostItem.MediaType.VIDEO) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Color(0xFF0D0D0D)),
                        contentAlignment = Alignment.Center,
                    ) {
                        AsyncImage(
                            model = firstUri,
                            contentDescription = "Video thumbnail",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.35f)),
                        )
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.65f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = "Play video",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp),
                            )
                        }
                        if (mediaUris.size > 1) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(10.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                            ) {
                                Text("+${mediaUris.size - 1} more", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                } else {
                    if (mediaUris.size == 1) {
                        AsyncImage(
                            model = firstUri,
                            contentDescription = "Shared original media",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 180.dp, max = 300.dp),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Row(modifier = Modifier.fillMaxWidth().height(200.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            AsyncImage(
                                model = firstUri,
                                contentDescription = "Shared media 1",
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                contentScale = ContentScale.Crop,
                            )
                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                AsyncImage(
                                    model = mediaUris[1],
                                    contentDescription = "Shared media 2",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                                if (mediaUris.size > 2) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.5f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            "+${mediaUris.size - 2}",
                                            color = Color.White,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            mediaEmojis.isNotEmpty() -> {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(cardBorder.copy(0.2f), Color(0xFF050810).copy(0.6f))
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(mediaEmojis.joinToString(" "), fontSize = 48.sp)
                }
            }
        }

        // Stats summary row at bottom of nested card
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("👍", fontSize = 12.sp)
                Text("❤️", fontSize = 12.sp)
                Text(
                    text = "$likes likes",
                    color = grayText,
                    fontSize = 12.sp,
                )
            }
            Text(
                text = "$comments comments • $shares shares",
                color = grayText,
                fontSize = 12.sp,
            )
        }
    }
}
