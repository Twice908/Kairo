package com.kairo.player.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kairo.player.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.CompositionLocalProvider
import com.kairo.player.domain.model.Album
import com.kairo.player.domain.model.Artist
import com.kairo.player.domain.model.Track
import com.kairo.player.playback.PlaybackState
import com.kairo.player.ui.components.EmptyState
import com.kairo.player.ui.components.MiniPlayer
import com.kairo.player.ui.components.PlaylistVisual
import com.kairo.player.ui.LocalKairoMotionEnabled
import com.kairo.player.ui.rememberSystemAnimationsEnabled
import com.kairo.player.ui.screens.AudioQualityDetails
import com.kairo.player.ui.screens.DiagnosticsScreen
import com.kairo.player.ui.screens.HomeScreen
import com.kairo.player.ui.screens.LibraryScreen
import com.kairo.player.ui.screens.NowPlayingScreen
import com.kairo.player.ui.screens.QueueScreen
import com.kairo.player.ui.screens.SearchScreen
import com.kairo.player.ui.screens.SettingsScreen
import com.kairo.player.ui.theme.KairoSpacing

private object KairoRoute {
    const val Home = "home"
    const val Search = "search"
    const val NowPlaying = "now_playing"
    const val Queue = "queue"
    const val Library = "library"
    const val Diagnostics = "diagnostics"
    const val Settings = "settings"
}

private data class MainDestination(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun KairoApp(viewModel: KairoViewModel) {
    val motionEnabled = rememberSystemAnimationsEnabled()
    CompositionLocalProvider(LocalKairoMotionEnabled provides motionEnabled) {
        SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
            KairoAppContent(viewModel, this, motionEnabled)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun KairoAppContent(
    viewModel: KairoViewModel,
    sharedTransitionScope: SharedTransitionScope,
    motionEnabled: Boolean,
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val versionName = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty() }
            .getOrDefault("")
    }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val isNowPlaying = currentDestination?.route == KairoRoute.NowPlaying
    val snackbarHostState = remember { SnackbarHostState() }
    val message by viewModel.message.collectAsStateWithLifecycle()
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        uri?.let { viewModel.addLocalFolder(it.toString()) }
    }

    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message!!)
            viewModel.dismissMessage()
        }
    }

    val mainDestinations = remember {
        listOf(
            MainDestination(KairoRoute.Home, "Home", Icons.Outlined.Home),
            MainDestination(KairoRoute.Search, "Search", Icons.Outlined.Search),
            MainDestination(KairoRoute.Library, "Library", Icons.Outlined.LibraryMusic),
            MainDestination(KairoRoute.Queue, "Queue", Icons.AutoMirrored.Outlined.QueueMusic),
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Image(
                        painter = painterResource(R.drawable.kairo_logo),
                        contentDescription = "Kairo",
                        modifier = Modifier.width(96.dp).height(32.dp),
                    )
                },
                actions = {
                    IconButton(onClick = { navController.navigate(KairoRoute.NowPlaying) }) {
                        Icon(Icons.Outlined.MusicNote, contentDescription = "Now playing")
                    }
                    IconButton(onClick = { navController.navigate(KairoRoute.Diagnostics) }) {
                        Icon(Icons.Outlined.GraphicEq, contentDescription = "Audio diagnostics")
                    }
                    IconButton(onClick = { navController.navigate(KairoRoute.Settings) }) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        bottomBar = {
            Column {
                PlaybackMiniPlayer(
                    viewModel = viewModel,
                    sharedTransitionScope = sharedTransitionScope,
                    sharedElementVisible = !isNowPlaying,
                    motionEnabled = motionEnabled,
                ) {
                    navController.navigate(KairoRoute.NowPlaying)
                }
                NavigationBar {
                    mainDestinations.forEach { destination ->
                        val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { contentPadding ->
        NavHost(
            navController = navController,
            startDestination = KairoRoute.Home,
            modifier = Modifier.fillMaxSize().padding(contentPadding),
            enterTransition = {
                if (motionEnabled) {
                    fadeIn(tween(180, easing = FastOutSlowInEasing)) +
                        slideInHorizontally(tween(180, easing = FastOutSlowInEasing)) { it / 28 }
                } else EnterTransition.None
            },
            exitTransition = {
                if (motionEnabled) {
                    fadeOut(tween(180, easing = FastOutSlowInEasing)) +
                        slideOutHorizontally(tween(180, easing = FastOutSlowInEasing)) { -it / 28 }
                } else ExitTransition.None
            },
            popEnterTransition = {
                if (motionEnabled) {
                    fadeIn(tween(180, easing = FastOutSlowInEasing)) +
                        slideInHorizontally(tween(180, easing = FastOutSlowInEasing)) { -it / 28 }
                } else EnterTransition.None
            },
            popExitTransition = {
                if (motionEnabled) {
                    fadeOut(tween(180, easing = FastOutSlowInEasing)) +
                        slideOutHorizontally(tween(180, easing = FastOutSlowInEasing)) { it / 28 }
                } else ExitTransition.None
            },
        ) {
            composable(KairoRoute.Home) {
                HomeDestination(viewModel) { route -> navController.navigate(route) }
            }
            composable(KairoRoute.Search) {
                SearchDestination(viewModel) { route -> navController.navigate(route) }
            }
            composable(KairoRoute.NowPlaying) {
                NowPlayingDestination(
                    viewModel,
                    sharedTransitionScope,
                    sharedElementVisible = isNowPlaying && motionEnabled,
                ) { navController.navigate(KairoRoute.Queue) }
            }
            composable(KairoRoute.Queue) {
                QueueDestination(viewModel) { navController.navigate(KairoRoute.NowPlaying) }
            }
            composable(KairoRoute.Library) {
                LibraryDestination(viewModel) { route -> navController.navigate(route) }
            }
            composable(KairoRoute.Diagnostics) { DiagnosticsDestination(viewModel) }
            composable(KairoRoute.Settings) {
                SettingsScreen(
                    versionName = versionName,
                    onAddMusicFolder = { folderPicker.launch(null) },
                    onOpenDiagnostics = { navController.navigate(KairoRoute.Diagnostics) },
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun PlaybackMiniPlayer(
    viewModel: KairoViewModel,
    sharedTransitionScope: SharedTransitionScope,
    sharedElementVisible: Boolean,
    motionEnabled: Boolean,
    onOpen: () -> Unit,
) {
    val playback by viewModel.playbackState.collectAsStateWithLifecycle()
    val track = playback.currentTrack?.toPresentation(playback.durationMs)
    val duration = playback.durationMs
    val progress = duration?.takeIf { it > 0L }?.let { playback.currentPositionMs.toFloat() / it }
    AnimatedVisibility(
        visible = sharedElementVisible,
        enter = if (motionEnabled) fadeIn(tween(130)) else EnterTransition.None,
        exit = if (motionEnabled) fadeOut(tween(110)) else ExitTransition.None,
    ) {
        MiniPlayer(
            track = track,
            isPlaying = playback is PlaybackState.Playing || playback is PlaybackState.Buffering,
            progress = progress,
            onPlayPause = viewModel::togglePlayPause,
            onOpenNowPlaying = onOpen,
            artworkIndex = track?.id?.hashCode() ?: 0,
            sharedTransitionScope = sharedTransitionScope,
            sharedElementVisible = sharedElementVisible && motionEnabled,
            modifier = Modifier.padding(horizontal = KairoSpacing.medium, vertical = KairoSpacing.small),
        )
    }
}

@Composable
private fun HomeDestination(viewModel: KairoViewModel, navigate: (String) -> Unit) {
    val library by viewModel.libraryState.collectAsStateWithLifecycle()
    HomeScreen(
        recentlyPlayed = library.recentlyPlayed,
        albums = library.albums,
        artists = library.artists,
        playlists = library.playlists.map { PlaylistVisual(it.id, it.name, it.trackKeys.size) },
        isLoading = library.isLoading,
        errorMessage = library.error,
        onTrackClick = { track -> viewModel.playTrack(track); navigate(KairoRoute.NowPlaying) },
        onAlbumClick = { album -> viewModel.playAlbum(album); navigate(KairoRoute.NowPlaying) },
        onArtistClick = { artist -> viewModel.playArtist(artist); navigate(KairoRoute.NowPlaying) },
        onPlaylistClick = { playlistId -> viewModel.playPlaylist(playlistId); navigate(KairoRoute.NowPlaying) },
    )
}

@Composable
private fun SearchDestination(viewModel: KairoViewModel, navigate: (String) -> Unit) {
    val search by viewModel.searchState.collectAsStateWithLifecycle()
    val albums = remember(search.tracks) { search.tracks.mapNotNull { it.album }.distinctBy { "${it.sourceId}:${it.id}" } }
    val artists = remember(search.tracks) { search.tracks.flatMap { it.artists }.distinctBy { "${it.sourceId}:${it.id}" } }
    SearchScreen(
        query = search.query,
        onQueryChange = viewModel::setSearchQuery,
        state = search.status,
        tracks = search.tracks,
        albums = albums,
        artists = artists,
        playlists = search.playlists,
        errorMessage = search.error,
        onSearch = viewModel::search,
        onTrackClick = { track -> viewModel.playTrack(track); navigate(KairoRoute.NowPlaying) },
        onAlbumClick = { album ->
            viewModel.playTracks(search.tracks.filter { it.album?.let { item -> item.id == album.id && item.sourceId == album.sourceId } == true }, 0)
            navigate(KairoRoute.NowPlaying)
        },
        onArtistClick = { artist ->
            viewModel.playTracks(search.tracks.filter { track -> track.artists.any { it.id == artist.id && it.sourceId == artist.sourceId } }, 0)
            navigate(KairoRoute.NowPlaying)
        },
        onPlaylistClick = { playlistId -> viewModel.playPlaylist(playlistId); navigate(KairoRoute.NowPlaying) },
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun NowPlayingDestination(
    viewModel: KairoViewModel,
    sharedTransitionScope: SharedTransitionScope,
    sharedElementVisible: Boolean,
    openQueue: () -> Unit,
) {
    val playback by viewModel.playbackState.collectAsStateWithLifecycle()
    val queue by viewModel.queueState.collectAsStateWithLifecycle()
    val diagnostics by viewModel.diagnostics.collectAsStateWithLifecycle()
    val track = playback.currentTrack
    if (track == null) {
        EmptyState("Nothing playing", "Choose a song from Search or Library to begin.")
        return
    }
    val format = diagnostics.currentFormat
    NowPlayingScreen(
        track = track.toPresentation(playback.durationMs),
        isPlaying = playback is PlaybackState.Playing || playback is PlaybackState.Buffering,
        isBuffering = playback is PlaybackState.Loading || playback is PlaybackState.Buffering,
        positionMs = playback.currentPositionMs,
        durationMs = playback.durationMs,
        quality = AudioQualityDetails(
            lossless = format?.lossless,
            bitDepth = format?.bitDepth,
            sampleRateHz = format?.sampleRateHz,
            codec = format?.codec,
            channels = format?.channelCount?.let { "$it channels" },
        ),
        shuffleEnabled = queue.shuffleEnabled,
        repeatMode = queue.repeatMode,
        deviceVolume = queue.deviceVolume,
        maxDeviceVolume = queue.maxDeviceVolume,
        deviceMuted = queue.deviceMuted,
        sharedTransitionScope = sharedTransitionScope,
        sharedElementVisible = sharedElementVisible,
        onSeek = viewModel::seekTo,
        onPrevious = viewModel::previous,
        onPlayPause = viewModel::togglePlayPause,
        onNext = viewModel::next,
        onShuffle = viewModel::setShuffleEnabled,
        onRepeat = viewModel::cycleRepeatMode,
        onQueue = openQueue,
        onDeviceVolume = viewModel::setDeviceVolume,
        onDeviceMuted = viewModel::setDeviceMuted,
    )
}

@Composable
private fun QueueDestination(viewModel: KairoViewModel, openNowPlaying: () -> Unit) {
    val queue by viewModel.queueState.collectAsStateWithLifecycle()
    val tracks = remember(queue.tracks) {
        queue.tracks.map { track -> track.toPresentation() }
    }
    QueueScreen(
        tracks = tracks,
        currentIndex = queue.currentIndex,
        onClear = viewModel::clearQueue,
        onSelect = { index -> viewModel.selectQueueItem(index); openNowPlaying() },
        onRemove = { index -> viewModel.removeQueueItem(index) },
        onMove = viewModel::moveQueueItem,
    )
}

@Composable
private fun LibraryDestination(viewModel: KairoViewModel, navigate: (String) -> Unit) {
    val library by viewModel.libraryState.collectAsStateWithLifecycle()
    LibraryScreen(
        tracks = library.tracks,
        albums = library.albums,
        artists = library.artists,
        playlists = library.playlists.map { PlaylistVisual(it.id, it.name, it.trackKeys.size) },
        recentlyPlayed = library.recentlyPlayed,
        isLoading = library.isLoading,
        errorMessage = library.error,
        onTrackClick = { track -> viewModel.playTrack(track); navigate(KairoRoute.NowPlaying) },
        onAlbumClick = { album -> viewModel.playAlbum(album); navigate(KairoRoute.NowPlaying) },
        onArtistClick = { artist -> viewModel.playArtist(artist); navigate(KairoRoute.NowPlaying) },
        onPlaylistClick = { id -> viewModel.playPlaylist(id); navigate(KairoRoute.NowPlaying) },
    )
}

@Composable
private fun DiagnosticsDestination(viewModel: KairoViewModel) {
    val diagnostics by viewModel.diagnostics.collectAsStateWithLifecycle()
    val format = diagnostics.currentFormat
    val playbackStatus = when (val state = diagnostics.playbackState) {
        is PlaybackState.Idle -> "Idle"
        is PlaybackState.Loading -> "Loading"
        is PlaybackState.Playing -> "Playing"
        is PlaybackState.Paused -> "Paused"
        is PlaybackState.Buffering -> "Buffering"
        is PlaybackState.Completed -> "Completed"
        is PlaybackState.Error -> state.message?.let { "Error: $it" } ?: "Error"
    }
    DiagnosticsScreen(
        playbackStatus = playbackStatus,
        currentTrackTitle = diagnostics.playbackState.currentTrack?.title,
        playbackPositionMs = diagnostics.playbackPositionMs,
        bufferedPositionMs = diagnostics.bufferedPositionMs,
        quality = format?.let {
            AudioQualityDetails(
                lossless = it.lossless,
                bitDepth = it.bitDepth,
                sampleRateHz = it.sampleRateHz,
                codec = it.codec,
                channels = it.channelCount?.let { count -> "$count channels" },
            )
        },
    )
}
