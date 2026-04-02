package com.example.kampus.ui.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@Composable
fun CreatePostScreen(
	onClose: () -> Unit,
	onPost: (
		text: String,
		mediaUri: String?,
		mediaType: PostItem.MediaType,
		visibility: String,
		allowComments: Boolean,
		taggedPeople: List<String>,
		feelingEmoji: String?,
		location: String?,
	) -> Unit,
) {
	var text by remember { mutableStateOf("") }
	var mediaUri by remember { mutableStateOf<String?>(null) }
	var mediaType by remember { mutableStateOf(PostItem.MediaType.NONE) }
	var visibility by remember { mutableStateOf("Public") }
	var allowComments by remember { mutableStateOf(true) }
	var taggedPeopleRaw by remember { mutableStateOf("") }
	var feelingEmoji by remember { mutableStateOf<String?>(null) }
	var location by remember { mutableStateOf<String?>(null) }

	Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(16.dp),
			verticalArrangement = Arrangement.spacedBy(12.dp),
		) {
			Text("Create Post", style = MaterialTheme.typography.headlineSmall)

			OutlinedTextField(
				value = text,
				onValueChange = { text = it },
				label = { Text("What's on your mind?") },
				modifier = Modifier.fillMaxWidth(),
				minLines = 4,
				keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
			)

			OutlinedTextField(
				value = mediaUri.orEmpty(),
				onValueChange = {
					mediaUri = it.ifBlank { null }
					mediaType = if (mediaUri == null) PostItem.MediaType.NONE else PostItem.MediaType.IMAGE
				},
				label = { Text("Media URL (optional)") },
				modifier = Modifier.fillMaxWidth(),
				keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
			)

			OutlinedTextField(
				value = visibility,
				onValueChange = { visibility = it },
				label = { Text("Visibility") },
				modifier = Modifier.fillMaxWidth(),
			)

			OutlinedTextField(
				value = taggedPeopleRaw,
				onValueChange = { taggedPeopleRaw = it },
				label = { Text("Tagged people (comma separated)") },
				modifier = Modifier.fillMaxWidth(),
			)

			OutlinedTextField(
				value = feelingEmoji.orEmpty(),
				onValueChange = { feelingEmoji = it.ifBlank { null } },
				label = { Text("Feeling emoji (optional)") },
				modifier = Modifier.fillMaxWidth(),
			)

			OutlinedTextField(
				value = location.orEmpty(),
				onValueChange = { location = it.ifBlank { null } },
				label = { Text("Location (optional)") },
				modifier = Modifier.fillMaxWidth(),
			)

			Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
				Text("Allow comments")
				Switch(checked = allowComments, onCheckedChange = { allowComments = it })
			}

			Spacer(Modifier.height(8.dp))

			Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
				Button(onClick = onClose, modifier = Modifier.weight(1f)) {
					Text("Cancel")
				}
				Button(
					onClick = {
						val taggedPeople = taggedPeopleRaw
							.split(',')
							.map { it.trim() }
							.filter { it.isNotEmpty() }
						onPost(
							text.trim(),
							mediaUri,
							mediaType,
							visibility,
							allowComments,
							taggedPeople,
							feelingEmoji,
							location,
						)
					},
					modifier = Modifier.weight(1f),
				) {
					Text("Post")
				}
			}
		}
	}
}
