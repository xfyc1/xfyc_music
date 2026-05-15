package com.xfyc.music.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xfyc.music.ui.components.SongItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSongClick: (Long) -> Unit,
    onViewAllSongs: () -> Unit,
    onViewAllFavorites: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val recentSongs by viewModel.recentSongs.collectAsState()
    val favoriteSongs by viewModel.favoriteSongs.collectAsState()
    val songCount by viewModel.songCount.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Xfyc Music") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // Stats card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(
                            icon = Icons.Default.LibraryMusic,
                            label = "歌曲",
                            value = songCount.toString()
                        )
                        StatItem(
                            icon = Icons.Default.Favorite,
                            label = "收藏",
                            value = favoriteSongs.size.toString()
                        )
                    }
                }
            }

            // Recent plays
            if (recentSongs.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "最近播放",
                        icon = Icons.Default.History,
                        onViewAll = onViewAllSongs
                    )
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(recentSongs.take(10)) { song ->
                            SongItem(
                                song = song,
                                onClick = { onSongClick(song.id) },
                                modifier = Modifier.width(280.dp)
                            )
                        }
                    }
                }
            }

            // Favorites
            if (favoriteSongs.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "收藏",
                        icon = Icons.Default.Favorite,
                        onViewAll = onViewAllFavorites
                    )
                }
                items(favoriteSongs.take(10)) { song ->
                    SongItem(
                        song = song,
                        onClick = { onSongClick(song.id) }
                    )
                }
            }

            // Empty state
            if (recentSongs.isEmpty() && favoriteSongs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                            Text(
                                text = "暂无音乐",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "前往音乐库扫描您的音乐文件",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onViewAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        TextButton(onClick = onViewAll) {
            Text("查看全部")
        }
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
