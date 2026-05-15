package com.xfyc.music.ui.library

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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xfyc.music.ui.components.SongItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onSongClick: (Long) -> Unit,
    onArtistClick: (String) -> Unit,
    onAlbumClick: (String) -> Unit,
    onPlaylistClick: (Long) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val allSongs by viewModel.allSongs.collectAsState()
    val artists by viewModel.artists.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scanResult by viewModel.scanResult.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()

    val tabs = listOf("Songs", "Artists", "Albums", "Playlists")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Library") },
                actions = {
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 8.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    IconButton(onClick = { viewModel.scanMusic() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Scan")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Tab row
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { viewModel.setTab(index) },
                        text = { Text(title) }
                    )
                }
            }

            // Scan result snackbar
            scanResult?.let { message ->
                Snackbar(
                    modifier = Modifier.padding(8.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearScanResult() }) {
                            Text("OK")
                        }
                    }
                ) {
                    Text(message)
                }
            }

            // Content
            when (selectedTab) {
                0 -> SongList(
                    songs = allSongs,
                    onSongClick = onSongClick,
                    onToggleFavorite = { id, fav -> viewModel.toggleFavorite(id, fav) }
                )
                1 -> ArtistList(
                    artists = artists,
                    onArtistClick = onArtistClick
                )
                2 -> AlbumList(
                    albums = albums,
                    onAlbumClick = onAlbumClick
                )
                3 -> PlaylistTab(
                    playlists = playlists,
                    onPlaylistClick = onPlaylistClick
                )
            }
        }
    }
}

@Composable
private fun SongList(
    songs: List<com.xfyc.music.domain.model.Song>,
    onSongClick: (Long) -> Unit,
    onToggleFavorite: (Long, Boolean) -> Unit
) {
    if (songs.isEmpty()) {
        EmptyState("No songs found. Tap the refresh icon to scan.")
    } else {
        LazyColumn {
            item {
                Text(
                    text = "${songs.size} songs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            items(songs) { song ->
                SongItem(
                    song = song,
                    onClick = { onSongClick(song.id) },
                    onMoreClick = { onToggleFavorite(song.id, song.isFavorite) }
                )
            }
        }
    }
}

@Composable
private fun ArtistList(
    artists: List<String>,
    onArtistClick: (String) -> Unit
) {
    if (artists.isEmpty()) {
        EmptyState("No artists found")
    } else {
        LazyColumn {
            items(artists) { artist ->
                ListItem(
                    headlineContent = { Text(artist) },
                    leadingContent = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    },
                    modifier = Modifier.clickable { onArtistClick(artist) }
                )
            }
        }
    }
}

@Composable
private fun AlbumList(
    albums: List<String>,
    onAlbumClick: (String) -> Unit
) {
    if (albums.isEmpty()) {
        EmptyState("No albums found")
    } else {
        LazyColumn {
            items(albums) { album ->
                ListItem(
                    headlineContent = { Text(album) },
                    leadingContent = {
                        Icon(Icons.Default.Album, contentDescription = null)
                    },
                    modifier = Modifier.clickable { onAlbumClick(album) }
                )
            }
        }
    }
}

@Composable
private fun PlaylistTab(
    playlists: List<com.xfyc.music.domain.model.Playlist>,
    onPlaylistClick: (Long) -> Unit
) {
    if (playlists.isEmpty()) {
        EmptyState("No playlists yet. Create one from the Playlist tab.")
    } else {
        LazyColumn {
            items(playlists) { playlist ->
                ListItem(
                    headlineContent = { Text(playlist.name) },
                    supportingContent = {
                        Text("${playlist.songCount} songs")
                    },
                    leadingContent = {
                        Icon(Icons.Default.PlaylistPlay, contentDescription = null)
                    },
                    modifier = Modifier.clickable { onPlaylistClick(playlist.id) }
                )
            }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
