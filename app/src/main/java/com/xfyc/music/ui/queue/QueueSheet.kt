package com.xfyc.music.ui.queue

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    onDismiss: () -> Unit,
    onSongClick: (Int) -> Unit,
    viewModel: QueueViewModel = hiltViewModel()
) {
    val queueSongs by viewModel.queueSongs.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "播放队列 (${queueSongs.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                if (queueSongs.isNotEmpty()) {
                    TextButton(onClick = { viewModel.clearQueue() }) {
                        Text("清空")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (queueSongs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "队列为空",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    itemsIndexed(queueSongs) { index, song ->
                        val isCurrentSong = index == currentIndex

                        ListItem(
                            headlineContent = {
                                Text(
                                    text = song.title,
                                    fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCurrentSong)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                            },
                            supportingContent = {
                                Text(song.artist)
                            },
                            leadingContent = {
                                if (isCurrentSong) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = "正在播放",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Text(
                                        text = "${index + 1}",
                                        modifier = Modifier.padding(8.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            trailingContent = {
                                IconButton(onClick = {
                                    viewModel.removeFromQueue(index)
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "移除")
                                }
                            },
                            modifier = Modifier.clickable { onSongClick(index) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
