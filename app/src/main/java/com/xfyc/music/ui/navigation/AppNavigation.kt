package com.xfyc.music.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.xfyc.music.domain.model.SearchResult
import com.xfyc.music.ui.components.MiniPlayer
import com.xfyc.music.ui.home.HomeScreen
import com.xfyc.music.ui.library.LibraryScreen
import com.xfyc.music.ui.player.PlayerScreen
import com.xfyc.music.ui.player.PlayerViewModel
import com.xfyc.music.ui.playlist.PlaylistDetailScreen
import com.xfyc.music.ui.playlist.PlaylistScreen
import com.xfyc.music.ui.queue.QueueSheet
import com.xfyc.music.ui.search.SearchScreen
import com.xfyc.music.ui.settings.SettingsScreen
import com.xfyc.music.ui.source.MusicSourceScreen

object Routes {
    const val HOME = "home"
    const val LIBRARY = "library"
    const val SEARCH = "search"
    const val PLAYLISTS = "playlists"
    const val SETTINGS = "settings"
    const val PLAYER = "player"
    const val PLAYLIST_DETAIL = "playlist_detail/{playlistId}"
    const val MUSIC_SOURCES = "music_sources"

    fun playlistDetail(id: Long) = "playlist_detail/$id"
}

sealed class BottomNavItem(val route: String, val label: String, val icon: @Composable () -> Unit) {
    object Home : BottomNavItem(Routes.HOME, "首页", { Icon(Icons.Default.Home, contentDescription = "首页") })
    object Library : BottomNavItem(Routes.LIBRARY, "音乐库", { Icon(Icons.Default.LibraryMusic, contentDescription = "音乐库") })
    object Search : BottomNavItem(Routes.SEARCH, "搜索", { Icon(Icons.Default.Search, contentDescription = "搜索") })
    object Settings : BottomNavItem(Routes.SETTINGS, "设置", { Icon(Icons.Default.Settings, contentDescription = "设置") })
}

@Composable
fun AppNavigation(
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val currentSong by playerViewModel.currentSong.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    var showQueue by remember { mutableStateOf(false) }

    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Library,
        BottomNavItem.Search,
        BottomNavItem.Settings
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val mainRoutes = bottomNavItems.map { it.route }
    val showBottomBar = currentRoute in mainRoutes
    val showMiniPlayer = currentSong != null && currentRoute != Routes.PLAYER

    Scaffold(
        bottomBar = {
            Column {
                // Mini player above bottom nav
                if (showMiniPlayer) {
                    MiniPlayer(
                        song = currentSong,
                        isPlaying = isPlaying,
                        onPlayPause = { playerViewModel.togglePlayPause() },
                        onSkipNext = { playerViewModel.skipToNext() },
                        onClick = {
                            navController.navigate(Routes.PLAYER) { launchSingleTop = true }
                        }
                    )
                }

                if (showBottomBar) {
                    NavigationBar {
                        bottomNavItems.forEach { item ->
                            NavigationBarItem(
                                icon = { item.icon() },
                                label = { Text(item.label) },
                                selected = currentRoute == item.route,
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onSongClick = { songId -> playSong(songId, playerViewModel, navController) },
                    onViewAllSongs = {
                        navController.navigate(Routes.LIBRARY) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }
                    },
                    onViewAllFavorites = { }
                )
            }

            composable(Routes.LIBRARY) {
                LibraryScreen(
                    onSongClick = { songId -> playSong(songId, playerViewModel, navController) },
                    onArtistClick = { },
                    onAlbumClick = { },
                    onPlaylistClick = { id ->
                        navController.navigate(Routes.playlistDetail(id))
                    }
                )
            }

            composable(Routes.SEARCH) {
                SearchScreen(
                    onSongClick = { song, result ->
                        val index = result.songs.indexOf(song).coerceAtLeast(0)
                        playerViewModel.playQueue(result.songs, index)
                        navController.navigate(Routes.PLAYER) { launchSingleTop = true }
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.PLAYLISTS) {
                PlaylistScreen(
                    onPlaylistClick = { id ->
                        navController.navigate(Routes.playlistDetail(id))
                    }
                )
            }

            composable(
                route = Routes.PLAYLIST_DETAIL,
                arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
            ) { backStackEntry ->
                val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: return@composable
                PlaylistDetailScreen(
                    playlistId = playlistId,
                    onNavigateBack = { navController.popBackStack() },
                    onSongClick = { songId -> playSong(songId, playerViewModel, navController) }
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onNavigateToMusicSources = {
                        navController.navigate(Routes.MUSIC_SOURCES)
                    }
                )
            }

            composable(Routes.MUSIC_SOURCES) {
                MusicSourceScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.PLAYER) {
                PlayerScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onOpenQueue = { showQueue = true },
                    onAddToPlaylist = { songId ->
                        navController.navigate(Routes.PLAYLISTS) { launchSingleTop = true }
                    }
                )
            }
        }
    }

    if (showQueue) {
        QueueSheet(
            onDismiss = { showQueue = false },
            onSongClick = { index ->
                playerViewModel.playQueue.skipTo(index)
                showQueue = false
            }
        )
    }
}

private fun playSong(
    songId: Long,
    playerViewModel: PlayerViewModel,
    navController: androidx.navigation.NavController
) {
    playerViewModel.playSongIds(listOf(songId), 0)
    navController.navigate(Routes.PLAYER) { launchSingleTop = true }
}
