package com.xfyc.music.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xfyc.music.ui.components.DefaultCover
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
                title = {
                    Column {
                        Text("Xfyc Music", fontWeight = FontWeight.Bold)
                        Text(
                            text = "$songCount 首本地歌曲",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            item {
                HomeHero(
                    songCount = songCount,
                    favoriteCount = favoriteSongs.size,
                    latestTitle = recentSongs.firstOrNull()?.title,
                    onViewAllSongs = onViewAllSongs
                )
            }

            if (recentSongs.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "最近播放",
                        subtitle = "继续刚才的音乐",
                        icon = Icons.Default.History,
                        onViewAll = onViewAllSongs
                    )
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(recentSongs.take(8)) { song ->
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(tween(280)) + slideInVertically { it / 3 }
                            ) {
                                SongItem(
                                    song = song,
                                    onClick = { onSongClick(song.id) },
                                    modifier = Modifier.width(292.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (favoriteSongs.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "收藏",
                        subtitle = "常听的声音放在前面",
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

            if (recentSongs.isEmpty() && favoriteSongs.isEmpty()) {
                item {
                    EmptyHome(onViewAllSongs = onViewAllSongs)
                }
            }
        }
    }
}

@Composable
private fun HomeHero(
    songCount: Int,
    favoriteCount: Int,
    latestTitle: String?,
    onViewAllSongs: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.32f)
                    )
                )
            )
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "今晚听点什么",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = latestTitle?.let { "上次停在《$it》" } ?: "扫描音乐库后开始播放",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatPill(label = "歌曲", value = songCount.toString())
                    StatPill(label = "收藏", value = favoriteCount.toString())
                }
                Spacer(modifier = Modifier.height(18.dp))
                FilledTonalButton(onClick = onViewAllSongs) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("打开音乐库")
                }
            }
            DefaultCover(
                modifier = Modifier
                    .padding(start = 14.dp)
                    .size(92.dp),
                size = 92.dp
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onViewAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(end = 10.dp)
                    .size(22.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        TextButton(onClick = onViewAll) {
            Text("全部")
        }
    }
}

@Composable
private fun StatPill(
    label: String,
    value: String
) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyHome(onViewAllSongs: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 42.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DefaultCover(size = 96.dp)
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "还没有音乐",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "进入音乐库扫描本地文件，收藏和最近播放会自动出现在这里。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))
        FilledTonalButton(onClick = onViewAllSongs) {
            Icon(Icons.Default.LibraryMusic, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("去扫描")
        }
    }
}
