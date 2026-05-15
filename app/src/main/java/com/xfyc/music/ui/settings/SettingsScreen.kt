package com.xfyc.music.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xfyc.music.domain.model.PlayMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
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
            TopAppBar(title = { Text("Settings") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Playback section
            item {
                SectionTitle("Playback")
            }

            item {
                ListItem(
                    headlineContent = { Text("Default play mode") },
                    supportingContent = { Text(defaultPlayMode.name) },
                    leadingContent = {
                        Icon(Icons.Default.Repeat, contentDescription = null)
                    },
                    modifier = Modifier.clickable { showPlayModeMenu = true }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text("Auto-pause on headset disconnect") },
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
                SectionTitle("Scanning")
            }

            item {
                ListItem(
                    headlineContent = { Text("Auto-scan on startup") },
                    supportingContent = { Text("Scan media library when app starts") },
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

            // Appearance section
            item {
                SectionTitle("Appearance")
            }

            item {
                ListItem(
                    headlineContent = { Text("Theme") },
                    supportingContent = { Text(themeMode.replaceFirstChar { it.uppercase() }) },
                    leadingContent = {
                        Icon(Icons.Default.DarkMode, contentDescription = null)
                    },
                    modifier = Modifier.clickable { showThemeMenu = true }
                )
            }

            // About section
            item {
                SectionTitle("About")
            }

            item {
                ListItem(
                    headlineContent = { Text("Xfyc Music") },
                    supportingContent = { Text("Version 1.0.0") },
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
                        title = { Text("Default Play Mode") },
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
                                                PlayMode.SEQUENTIAL -> "Sequential"
                                                PlayMode.SINGLE_LOOP -> "Single Loop"
                                                PlayMode.LIST_LOOP -> "List Loop"
                                                PlayMode.SHUFFLE -> "Shuffle"
                                            },
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showPlayModeMenu = false }) {
                                Text("OK")
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
                        title = { Text("Theme") },
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
                                Text("OK")
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
