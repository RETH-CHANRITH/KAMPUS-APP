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
import androidx.compose.foundation.layout.height
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
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import kotlinx.coroutines.launch

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
    val commentPickers = rememberMediaPickers(onPhotoSelected = { commentImageUri = it })
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var commentToDelete by remember { mutableStateOf<PostComment?>(null) }

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

    if (commentToDelete != null) {
        AlertDialog(
            onDismissRequest = { commentToDelete = null },
            title = { Text("Delete Comment") },
            text = { Text("Are you sure you want to delete this comment? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val comment = commentToDelete!!
                        commentToDelete = null
                        scope.launch {
                            viewModel.deleteComment(postId, comment.id)
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { commentToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
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
                                    )
                                    Column {
                                        Text(text = post.author, color = colors.onSurface, fontWeight = FontWeight.SemiBold)
                                        Text(text = formatPostTime(post), color = colors.onSurfaceVariant, fontSize = 12.sp)
                                    }
                                }

                                Text(text = post.content, color = colors.onSurface, fontSize = 15.sp, lineHeight = 22.sp)

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

                        item {
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
                                    postAuthorId = state.post?.authorId.orEmpty(),
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
                                        commentToDelete = targetComment
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .border(1.dp, colors.outline.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = if (replyingToName == null) "Write a comment" else "Write a reply",
                color = colors.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Text, photo, and replies stay in sync instantly.",
                color = colors.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }

        if (replyingToName != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(colors.primary.copy(alpha = 0.10f))
                    .border(1.dp, colors.primary.copy(alpha = 0.22f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("↩", color = colors.primary)
                Text("Replying to $replyingToName", color = colors.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(4.dp))
                Text(
                    "Cancel",
                    color = colors.onSurfaceVariant,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable(onClick = onCancelReply),
                )
            }
        }

        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            placeholder = { Text(if (replyingToName == null) "Write a comment..." else "Write a reply...") },
            minLines = 2,
            shape = RoundedCornerShape(14.dp),
        )

        if (imageUri != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surfaceVariant.copy(alpha = 0.35f))
                    .border(1.dp, colors.primary.copy(alpha = 0.20f), RoundedCornerShape(16.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Selected comment image",
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text("Photo attached", color = colors.onSurface, fontWeight = FontWeight.SemiBold)
                    Text("Will upload with your comment", color = colors.onSurfaceVariant, fontSize = 12.sp)
                }
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Remove photo",
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable(onClick = onRemovePhoto),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .height(42.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(colors.surfaceVariant.copy(alpha = 0.35f))
                    .border(1.dp, colors.outline.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
                    .clickable(onClick = onPickPhoto)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.Image, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                    Text("Photo", color = colors.onSurface, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Button(
                onClick = onSubmit,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = Color.White),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(if (replyingToName == null) "Post Comment" else "Post Reply", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CommentThread(
    comment: PostComment,
    postAuthorId: String,
    onReply: (PostComment) -> Unit,
    onLike: (PostComment) -> Unit,
    onDelete: (PostComment) -> Unit,
    depth: Int = 0,
) {
    val colors = MaterialTheme.colorScheme
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val canDelete = comment.userId == currentUserId || (postAuthorId.isNotEmpty() && postAuthorId == currentUserId)

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
                if (canDelete) {
                    ActionChip(label = "Delete", active = false, onClick = { onDelete(comment) })
                }
            }
        }

        comment.replies.forEach { reply ->
            CommentThread(
                comment = reply,
                postAuthorId = postAuthorId,
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
) {
    val colors = MaterialTheme.colorScheme
    if (!imageUrl.isNullOrBlank()) {
        AsyncImage(
            model = imageUrl,
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
