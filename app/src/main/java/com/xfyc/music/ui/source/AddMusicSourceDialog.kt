package com.xfyc.music.ui.source

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMusicSourceDialog(
    onDismiss: () -> Unit,
    onAddUrl: (name: String, baseUrl: String, searchPath: String, playPath: String, coverPath: String, lyricPath: String, authType: String, authConfig: String?) -> Unit,
    onAddJson: (name: String, jsonConfig: String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    var name by remember { mutableStateOf("") }
    var baseUrl by remember { mutableStateOf("") }
    var searchPath by remember { mutableStateOf("/api/search?keyword={query}") }
    var playPath by remember { mutableStateOf("/api/song/{songId}/play") }
    var coverPath by remember { mutableStateOf("/api/song/{songId}/cover") }
    var lyricPath by remember { mutableStateOf("/api/song/{songId}/lyric") }
    var authType by remember { mutableStateOf("none") }
    var authConfig by remember { mutableStateOf("") }
    var jsonConfig by remember { mutableStateOf("") }

    val authTypes = listOf("none", "token", "api_key", "basic_auth", "cookie")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Music Source") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) { Text("URL Source") }
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) { Text("JSON Config") }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == 0) {
                    // URL Source
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Source Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        label = { Text("Base URL") },
                        placeholder = { Text("https://example.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = searchPath,
                        onValueChange = { searchPath = it },
                        label = { Text("Search API Path") },
                        placeholder = { Text("/api/search?keyword={query}") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = playPath,
                        onValueChange = { playPath = it },
                        label = { Text("Play URL Path") },
                        placeholder = { Text("/api/song/{songId}/play") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = coverPath,
                        onValueChange = { coverPath = it },
                        label = { Text("Cover URL Path (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = lyricPath,
                        onValueChange = { lyricPath = it },
                        label = { Text("Lyric URL Path (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Auth type
                    Text("Authentication", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        authTypes.forEach { type ->
                            FilterChip(
                                selected = authType == type,
                                onClick = { authType = type },
                                label = { Text(type.replace("_", " ").replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }

                    if (authType != "none") {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = authConfig,
                            onValueChange = { authConfig = it },
                            label = { Text("Auth Config (JSON)") },
                            placeholder = {
                                Text(
                                    when (authType) {
                                        "token" -> "{\"token\": \"your-token\"}"
                                        "api_key" -> "{\"key\": \"your-key\", \"headerName\": \"X-Api-Key\"}"
                                        "basic_auth" -> "{\"username\": \"user\", \"password\": \"pass\"}"
                                        "cookie" -> "{\"cookie\": \"session=xxx\"}"
                                        else -> ""
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    // JSON Config
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Source Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = jsonConfig,
                        onValueChange = { jsonConfig = it },
                        label = { Text("JSON Config") },
                        placeholder = {
                            Text(
                                "{\n" +
                                    "  \"type\": \"custom_api\",\n" +
                                    "  \"baseUrl\": \"https://...\",\n" +
                                    "  \"searchUrl\": \"...\",\n" +
                                    "  \"playUrl\": \"...\"\n" +
                                    "}"
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        maxLines = 10
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedTab == 0 && name.isNotBlank() && baseUrl.isNotBlank()) {
                        onAddUrl(name, baseUrl, searchPath, playPath, coverPath, lyricPath, authType, authConfig.ifBlank { null })
                    } else if (selectedTab == 1 && name.isNotBlank() && jsonConfig.isNotBlank()) {
                        onAddJson(name, jsonConfig)
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
