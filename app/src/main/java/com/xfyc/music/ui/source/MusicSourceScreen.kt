package com.xfyc.music.ui.source

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xfyc.music.domain.model.MusicSource
import com.xfyc.music.domain.model.toDomainModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicSourceScreen(
    onNavigateBack: () -> Unit,
    viewModel: MusicSourceViewModel = hiltViewModel()
) {
    val sources by viewModel.sources.collectAsState()
    val isTesting by viewModel.isTesting.collectAsState()
    val testResult by viewModel.testResult.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    val importResult by viewModel.importResult.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showQrScanner by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Dismiss dialog on successful import
    LaunchedEffect(importResult) {
        if (importResult?.startsWith("成功") == true) {
            showAddDialog = false
        }
    }

    // Show import/test results in snackbar
    LaunchedEffect(importResult, testResult) {
        val msg = importResult ?: testResult?.second
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("音乐源") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showQrScanner = true }) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "扫描二维码")
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "添加源")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (sources.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Cloud,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("未配置音乐源")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "添加 URL 源或导入 JSON 配置",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { showAddDialog = true }) {
                        Text("添加音乐源")
                    }
                }
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(sources) { source ->
                    SourceItem(
                        source = source,
                        isTesting = isTesting[source.id] == true,
                        onToggleEnabled = { viewModel.toggleEnabled(source.id, source.enabled) },
                        onTestConnection = { viewModel.testConnection(source.id) },
                        onDelete = { viewModel.deleteSource(source.id) }
                    )
                }
            }
        }

        // Test result snackbar
        testResult?.let { (id, msg) ->
            LaunchedEffect(testResult) {
                kotlinx.coroutines.delay(2000)
                viewModel.clearTestResult()
            }
        }

        // Import result snackbar
        importResult?.let { msg ->
            LaunchedEffect(importResult) {
                kotlinx.coroutines.delay(3000)
                viewModel.clearImportResult()
            }
        }

        // Add source dialog
        if (showAddDialog) {
            AddMusicSourceDialog(
                onDismiss = { showAddDialog = false },
                onAddUrl = { name, baseUrl, search, play, cover, lyric, authType, authConfig ->
                    viewModel.addUrlSource(name, baseUrl, search, play, cover, lyric, authType, authConfig)
                    showAddDialog = false
                },
                onAddJson = { name, json ->
                    viewModel.addJsonConfigSource(name, json)
                    showAddDialog = false
                },
                onImportUrl = { url ->
                    viewModel.importFromUrl(url)
                    // Don't dismiss immediately - wait for result
                },
                isImporting = isImporting
            )
        }
    }
}

@Composable
private fun SourceItem(
    source: MusicSource,
    isTesting: Boolean,
    onToggleEnabled: () -> Unit,
    onTestConnection: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = source.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val statusColor = when (source.status) {
                            "idle" -> MaterialTheme.colorScheme.primary
                            "error" -> MaterialTheme.colorScheme.error
                            "syncing" -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Surface(
                            color = statusColor.copy(alpha = 0.2f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = source.status,
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${source.type} · ${source.baseUrl.take(40)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    source.errorMessage?.let { error ->
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Switch(
                    checked = source.enabled,
                    onCheckedChange = { onToggleEnabled() }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onTestConnection,
                    enabled = !isTesting
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text("测试")
                }
                TextButton(onClick = { showDeleteConfirm = true }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除音乐源") },
            text = { Text("删除 \"${source.name}\"？缓存内容也将被清除。") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteConfirm = false }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            }
        )
    }
}
