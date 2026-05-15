package com.xfyc.music.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xfyc.music.domain.model.PlayMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToMusicSources: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val defaultPlayMode by viewModel.defaultPlayMode.collectAsState()
    val scanOnStartup by viewModel.scanOnStartup.collectAsState()
    val autoPauseHeadset by viewModel.autoPauseHeadset.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    var showPlayModeMenu by remember { mutableStateOf(false) }
    var showThemeMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("设置") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Playback section
            item {
                SectionTitle("播放")
            }

            item {
                ListItem(
                    headlineContent = { Text("默认播放模式") },
                    supportingContent = { Text(defaultPlayMode.name) },
                    leadingContent = {
                        Icon(Icons.Default.Repeat, contentDescription = null)
                    },
                    modifier = Modifier.clickable { showPlayModeMenu = true }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text("耳机断开时自动暂停") },
                    leadingContent = {
                        Icon(Icons.Default.HeadsetOff, contentDescription = null)
                    },
                    trailingContent = {
                        Switch(
                            checked = autoPauseHeadset,
                            onCheckedChange = { viewModel.setAutoPauseHeadset(it) }
                        )
                    }
                )
            }

            // Scanning section
            item {
                SectionTitle("扫描")
            }

            item {
                ListItem(
                    headlineContent = { Text("启动时自动扫描") },
                    supportingContent = { Text("应用启动时扫描媒体库") },
                    leadingContent = {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                    },
                    trailingContent = {
                        Switch(
                            checked = scanOnStartup,
                            onCheckedChange = { viewModel.setScanOnStartup(it) }
                        )
                    }
                )
            }

            // Online music section
            item {
                SectionTitle("在线音乐")
            }

            item {
                ListItem(
                    headlineContent = { Text("音乐源管理") },
                    supportingContent = { Text("添加和管理在线音乐源") },
                    leadingContent = {
                        Icon(Icons.Default.Cloud, contentDescription = null)
                    },
                    modifier = Modifier.clickable { onNavigateToMusicSources() }
                )
            }

            // Appearance section
            item {
                SectionTitle("外观")
            }

            item {
                ListItem(
                    headlineContent = { Text("主题") },
                    supportingContent = { Text(themeMode.replaceFirstChar { it.uppercase() }) },
                    leadingContent = {
                        Icon(Icons.Default.DarkMode, contentDescription = null)
                    },
                    modifier = Modifier.clickable { showThemeMenu = true }
                )
            }

            // About section
            item {
                SectionTitle("关于")
            }

            item {
                ListItem(
                    headlineContent = { Text("Xfyc Music") },
                    supportingContent = { Text("版本 1.0.0") },
                    leadingContent = {
                        Icon(Icons.Default.Info, contentDescription = null)
                    }
                )
            }

            // Play mode dropdown
            if (showPlayModeMenu) {
                item {
                    AlertDialog(
                        onDismissRequest = { showPlayModeMenu = false },
                        title = { Text("默认播放模式") },
                        text = {
                            Column {
                                PlayMode.entries.forEach { mode ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable {
                                                viewModel.setDefaultPlayMode(mode)
                                                showPlayModeMenu = false
                                            }
                                    ) {
                                        RadioButton(
                                            selected = defaultPlayMode == mode,
                                            onClick = {
                                                viewModel.setDefaultPlayMode(mode)
                                                showPlayModeMenu = false
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = when (mode) {
                                                PlayMode.SEQUENTIAL -> "顺序播放"
                                                PlayMode.SINGLE_LOOP -> "单曲循环"
                                                PlayMode.LIST_LOOP -> "列表循环"
                                                PlayMode.SHUFFLE -> "随机播放"
                                            },
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showPlayModeMenu = false }) {
                                Text("确定")
                            }
                        }
                    )
                }
            }

            // Theme dropdown
            if (showThemeMenu) {
                item {
                    val themes = listOf("system", "light", "dark")
                    AlertDialog(
                        onDismissRequest = { showThemeMenu = false },
                        title = { Text("主题") },
                        text = {
                            Column {
                                themes.forEach { theme ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable {
                                                viewModel.setThemeMode(theme)
                                                showThemeMenu = false
                                            }
                                    ) {
                                        RadioButton(
                                            selected = themeMode == theme,
                                            onClick = {
                                                viewModel.setThemeMode(theme)
                                                showThemeMenu = false
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = theme.replaceFirstChar { it.uppercase() },
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showThemeMenu = false }) {
                                Text("确定")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}
