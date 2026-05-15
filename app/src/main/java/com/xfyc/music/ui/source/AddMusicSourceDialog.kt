package com.xfyc.music.ui.source

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMusicSourceDialog(
    onDismiss: () -> Unit,
    onAddUrl: (name: String, baseUrl: String, searchPath: String, playPath: String, coverPath: String, lyricPath: String, authType: String, authConfig: String?) -> Unit,
    onAddJson: (name: String, jsonConfig: String) -> Unit,
    onImportUrl: (url: String) -> Unit,
    isImporting: Boolean = false
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
    var importUrl by remember { mutableStateOf("") }

    val authTypes = listOf("none", "token", "api_key", "basic_auth", "cookie")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加音乐源") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) { Text("URL 源") }
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) { Text("JSON 配置") }
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) { Text("导入URL") }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == 0) {
                    // URL Source
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("源名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        label = { Text("基础 URL") },
                        placeholder = { Text("https://example.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = searchPath,
                        onValueChange = { searchPath = it },
                        label = { Text("搜索 API 路径") },
                        placeholder = { Text("/api/search?keyword={query}") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = playPath,
                        onValueChange = { playPath = it },
                        label = { Text("播放 URL 路径") },
                        placeholder = { Text("/api/song/{songId}/play") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = coverPath,
                        onValueChange = { coverPath = it },
                        label = { Text("封面 URL 路径（可选）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = lyricPath,
                        onValueChange = { lyricPath = it },
                        label = { Text("歌词 URL 路径（可选）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Auth type
                    Text("认证方式", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        authTypes.forEach { type ->
                            val label = when (type) {
                                "none" -> "无"
                                "token" -> "Token"
                                "api_key" -> "Api Key"
                                "basic_auth" -> "Basic Auth"
                                "cookie" -> "Cookie"
                                else -> type
                            }
                            FilterChip(
                                selected = authType == type,
                                onClick = { authType = type },
                                label = { Text(label) }
                            )
                        }
                    }

                    if (authType != "none") {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = authConfig,
                            onValueChange = { authConfig = it },
                            label = { Text("认证配置 (JSON)") },
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
                } else if (selectedTab == 1) {
                    // JSON Config
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("源名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = jsonConfig,
                        onValueChange = { jsonConfig = it },
                        label = { Text("JSON 配置") },
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
                } else {
                    // Import URL
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "粘贴洛雪音乐音源 JS 文件的 URL",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = importUrl,
                            onValueChange = { importUrl = it },
                            label = { Text("音源 URL") },
                            placeholder = {
                                Text("https://fastly.jsdelivr.net/gh/.../render_api.js")
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "支持解析 API_URL、API_KEY 和 MUSIC_SOURCE 配置\n自动创建对应音乐源（酷我/酷狗/网易云/咪咕等）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        selectedTab == 0 && name.isNotBlank() && baseUrl.isNotBlank() ->
                            onAddUrl(name, baseUrl, searchPath, playPath, coverPath, lyricPath, authType, authConfig.ifBlank { null })
                        selectedTab == 1 && name.isNotBlank() && jsonConfig.isNotBlank() ->
                            onAddJson(name, jsonConfig)
                        selectedTab == 2 && importUrl.isNotBlank() ->
                            onImportUrl(importUrl)
                    }
                },
                enabled = when (selectedTab) {
                    0 -> !isImporting
                    1 -> !isImporting
                    2 -> importUrl.isNotBlank() && !isImporting
                    else -> true
                }
            ) {
                if (isImporting && selectedTab == 2) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (selectedTab == 2) "导入" else "添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
