package com.kairo.player.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VolumeDown
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.kairo.player.domain.model.Album
import com.kairo.player.domain.model.Artist
import com.kairo.player.domain.model.Track
import com.kairo.player.ui.components.AlbumCard
import com.kairo.player.ui.components.ArtistRow
import com.kairo.player.ui.components.EmptyState
import com.kairo.player.ui.components.ErrorState
import com.kairo.player.ui.components.KairoArtwork
import com.kairo.player.ui.components.LoadingState
import com.kairo.player.ui.components.PlaylistCard
import com.kairo.player.ui.components.PlaylistVisual
import com.kairo.player.ui.components.QualityBadge
import com.kairo.player.ui.components.QueueTrackRow
import com.kairo.player.ui.components.SectionHeader
import com.kairo.player.ui.components.TrackRow
import com.kairo.player.ui.components.kairoSharedElement
import com.kairo.player.ui.TrackPresentation
import com.kairo.player.ui.LocalKairoMotionEnabled
import com.kairo.player.ui.theme.KairoCorners
import com.kairo.player.ui.theme.KairoSizes
import com.kairo.player.ui.theme.KairoSpacing

@Composable
private fun ScreenHeading(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(KairoSpacing.small)) {
        Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionPlaceholder(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.padding(vertical = KairoSpacing.small),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun HomeScreen(
    recentlyPlayed: List<Track> = emptyList(),
    recentlyAdded: List<Track> = emptyList(),
    albums: List<Album> = emptyList(),
    artists: List<Artist> = emptyList(),
    playlists: List<PlaylistVisual> = emptyList(),
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onTrackClick: ((Track) -> Unit)? = null,
    onAlbumClick: ((Album) -> Unit)? = null,
    onArtistClick: ((Artist) -> Unit)? = null,
    onPlaylistClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = KairoSpacing.large, vertical = KairoSpacing.large),
            verticalArrangement = Arrangement.spacedBy(KairoSpacing.large),
        ) {
            item { ScreenHeading("Listen", "Your music, thoughtfully gathered.") }
            if (isLoading) {
                item { LoadingState() }
            } else if (errorMessage != null) {
                item { ErrorState(errorMessage) }
            } else {
            item { SectionHeader("Recently Played") }
            if (recentlyPlayed.isEmpty()) item { SectionPlaceholder("Your listening history will appear here.") }
            else items(recentlyPlayed.take(4), key = { "recent-played-${it.sourceId}-${it.id}" }) {
                TrackRow(it, artworkIndex = it.id.hashCode(), onClick = onTrackClick?.let { callback -> { callback(it) } })
            }
            item { SectionHeader("Recently Added") }
            if (recentlyAdded.isEmpty()) item { SectionPlaceholder("New music from your library will appear here.") }
            else items(recentlyAdded.take(4), key = { "recent-added-${it.sourceId}-${it.id}" }) {
                TrackRow(it, artworkIndex = it.id.hashCode(), onClick = onTrackClick?.let { callback -> { callback(it) } })
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(KairoSpacing.small)) {
                    SectionHeader("Albums")
                    if (albums.isEmpty()) SectionPlaceholder("Albums you add will appear here.")
                    else LazyRow(horizontalArrangement = Arrangement.spacedBy(KairoSpacing.medium)) {
                        items(albums, key = { "${it.sourceId}-${it.id}" }) { album -> AlbumCard(album, artworkIndex = album.id.hashCode(), onClick = onAlbumClick?.let { callback -> { callback(album) } }) }
                    }
                }
            }
            item { SectionHeader("Artists") }
            if (artists.isEmpty()) item { SectionPlaceholder("Artists in your library will appear here.") }
            else items(artists.take(5), key = { "home-artist-${it.id}" }) {
                ArtistRow(it, artworkIndex = it.id.hashCode(), onClick = onArtistClick?.let { callback -> { callback(it) } })
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(KairoSpacing.small)) {
                    SectionHeader("Playlists")
                    if (playlists.isEmpty()) SectionPlaceholder("Your playlists will appear here.")
                    else LazyRow(horizontalArrangement = Arrangement.spacedBy(KairoSpacing.medium)) {
                        items(playlists, key = { it.id }) { playlist -> PlaylistCard(playlist, artworkIndex = playlist.id.hashCode(), onClick = onPlaylistClick?.let { callback -> { callback(playlist.id) } }) }
                    }
                }
            }
            }
    }
}

enum class SearchContentState { IDLE, LOADING, RESULTS, EMPTY, ERROR }

@Composable
fun SearchScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    state: SearchContentState = SearchContentState.IDLE,
    suggestions: List<String> = emptyList(),
    tracks: List<Track> = emptyList(),
    albums: List<Album> = emptyList(),
    artists: List<Artist> = emptyList(),
    playlists: List<PlaylistVisual> = emptyList(),
    errorMessage: String? = null,
    onSearch: (() -> Unit)? = null,
    onTrackClick: ((Track) -> Unit)? = null,
    onAlbumClick: ((Album) -> Unit)? = null,
    onArtistClick: ((Artist) -> Unit)? = null,
    onPlaylistClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val motionEnabled = LocalKairoMotionEnabled.current
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = KairoSpacing.large, vertical = KairoSpacing.large),
        verticalArrangement = Arrangement.spacedBy(KairoSpacing.large),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(KairoSpacing.large)) {
                ScreenHeading("Search", "Find a song, album, artist, or playlist.")
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = onSearch ?: {}, enabled = onSearch != null) {
                            Icon(Icons.Outlined.Search, contentDescription = "Search music")
                        }
                    },
                    placeholder = { Text("Songs, albums, artists") },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(KairoCorners.medium),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke() }),
                )
            }
        }
        when (state) {
            SearchContentState.IDLE -> {
                if (suggestions.isEmpty()) {
                    item(key = "search-idle") {
                        Box(modifier = lazyItemMotion(motionEnabled)) {
                            EmptyState(
                                title = "Start with a search",
                                message = "Search across the music in your library.",
                                icon = Icons.Outlined.Search,
                            )
                        }
                    }
                } else {
                    item { SectionHeader("Suggestions") }
                    items(suggestions, key = { it }) { suggestion ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .height(KairoSizes.touchTarget)
                                .then(lazyItemMotion(motionEnabled)),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(KairoSpacing.medium),
                        ) {
                            Icon(Icons.Outlined.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(suggestion, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
            SearchContentState.LOADING -> item(key = "search-loading") {
                Box(modifier = lazyItemMotion(motionEnabled)) { LoadingState("Searching your music") }
            }
            SearchContentState.ERROR -> item(key = "search-error") {
                Box(modifier = lazyItemMotion(motionEnabled)) {
                    ErrorState(errorMessage ?: "Search is temporarily unavailable.", onRetry = onSearch)
                }
            }
            SearchContentState.EMPTY -> item(key = "search-empty") {
                Box(modifier = lazyItemMotion(motionEnabled)) {
                    EmptyState("No matches", "Try a different title, artist, or album.", icon = Icons.Outlined.Search)
                }
            }
            SearchContentState.RESULTS -> {
                if (tracks.isNotEmpty()) {
                    item { SectionHeader("Songs") }
                    items(tracks, key = { "track-${it.sourceId}-${it.id}" }) { track ->
                        TrackRow(
                            track,
                            modifier = lazyItemMotion(motionEnabled),
                            artworkIndex = track.id.hashCode(),
                            onClick = onTrackClick?.let { callback -> { callback(track) } },
                        )
                    }
                }
                if (albums.isNotEmpty()) {
                    item { SectionHeader("Albums") }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(KairoSpacing.medium)) {
                            items(albums, key = { "album-${it.sourceId}-${it.id}" }) { album ->
                                AlbumCard(
                                    album,
                                    modifier = lazyItemMotion(motionEnabled),
                                    artworkIndex = album.id.hashCode(),
                                    onClick = onAlbumClick?.let { callback -> { callback(album) } },
                                )
                            }
                        }
                    }
                }
                if (artists.isNotEmpty()) {
                    item { SectionHeader("Artists") }
                    items(artists, key = { "artist-${it.sourceId}-${it.id}" }) { artist ->
                        ArtistRow(
                            artist,
                            modifier = lazyItemMotion(motionEnabled),
                            artworkIndex = artist.id.hashCode(),
                            onClick = onArtistClick?.let { callback -> { callback(artist) } },
                        )
                    }
                }
                if (playlists.isNotEmpty()) {
                    item { SectionHeader("Playlists") }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(KairoSpacing.medium)) {
                            items(playlists, key = { "playlist-${it.id}" }) { playlist ->
                                PlaylistCard(
                                    playlist,
                                    modifier = lazyItemMotion(motionEnabled),
                                    artworkIndex = playlist.id.hashCode(),
                                    onClick = onPlaylistClick?.let { callback -> { callback(playlist.id) } },
                                )
                            }
                        }
                    }
                }
                if (tracks.isEmpty() && albums.isEmpty() && artists.isEmpty() && playlists.isEmpty()) {
                    item(key = "search-results-empty") {
                        Box(modifier = lazyItemMotion(motionEnabled)) {
                            EmptyState("No matches", "Try a different title, artist, or album.", icon = Icons.Outlined.Search)
                        }
                    }
                }
            }
        }
    }
}

data class AudioQualityDetails(
    val lossless: Boolean? = null,
    val bitDepth: Int? = null,
    val sampleRateHz: Int? = null,
    val codec: String? = null,
    val channels: String? = null,
)

@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
fun NowPlayingScreen(
    track: TrackPresentation,
    isPlaying: Boolean,
    positionMs: Long,
    isBuffering: Boolean = false,
    durationMs: Long? = track.durationMs,
    quality: AudioQualityDetails? = null,
    shuffleEnabled: Boolean = false,
    repeatMode: Int = androidx.media3.common.Player.REPEAT_MODE_OFF,
    deviceVolume: Int? = null,
    maxDeviceVolume: Int? = null,
    deviceMuted: Boolean? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    sharedElementVisible: Boolean = true,
    artworkIndex: Int = track.id.hashCode(),
    onSeek: ((Long) -> Unit)? = null,
    onPrevious: (() -> Unit)? = null,
    onPlayPause: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
    onRepeat: (() -> Unit)? = null,
    onQueue: (() -> Unit)? = null,
    onShuffle: ((Boolean) -> Unit)? = null,
    onDeviceVolume: ((Int) -> Unit)? = null,
    onDeviceMuted: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val wideLayout = maxWidth >= 600.dp && maxWidth > maxHeight
        if (wideLayout) {
            Row(
                modifier = Modifier.fillMaxSize().padding(KairoSpacing.xLarge),
                horizontalArrangement = Arrangement.spacedBy(KairoSpacing.xLarge),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                KairoArtwork(
                    artworkIndex,
                    Modifier.weight(1f).aspectRatio(1f).kairoSharedElement(
                        sharedTransitionScope,
                        "kairo-artwork-${track.id}",
                        sharedElementVisible,
                    ),
                )
                NowPlayingControls(
                    track, isPlaying, isBuffering, positionMs, durationMs, quality, shuffleEnabled, repeatMode,
                    deviceVolume, maxDeviceVolume, deviceMuted, onSeek, sharedTransitionScope, sharedElementVisible,
                    onPrevious, onPlayPause, onNext, onShuffle, onRepeat, onQueue,
                    onDeviceVolume, onDeviceMuted,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = KairoSpacing.large, vertical = KairoSpacing.large),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(KairoSpacing.large),
            ) {
                item {
                    KairoArtwork(
                        artworkIndex,
                        Modifier.fillMaxWidth().widthIn(max = 420.dp).aspectRatio(1f).kairoSharedElement(
                            sharedTransitionScope,
                            "kairo-artwork-${track.id}",
                            sharedElementVisible,
                        ),
                    )
                }
                item {
                    NowPlayingControls(
                        track, isPlaying, isBuffering, positionMs, durationMs, quality, shuffleEnabled, repeatMode,
                        deviceVolume, maxDeviceVolume, deviceMuted, onSeek, sharedTransitionScope, sharedElementVisible,
                        onPrevious, onPlayPause, onNext, onShuffle, onRepeat, onQueue,
                        onDeviceVolume, onDeviceMuted,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun NowPlayingControls(
    track: TrackPresentation,
    isPlaying: Boolean,
    isBuffering: Boolean,
    positionMs: Long,
    durationMs: Long?,
    quality: AudioQualityDetails?,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    deviceVolume: Int?,
    maxDeviceVolume: Int?,
    deviceMuted: Boolean?,
    onSeek: ((Long) -> Unit)?,
    sharedTransitionScope: SharedTransitionScope?,
    sharedElementVisible: Boolean,
    onPrevious: (() -> Unit)?,
    onPlayPause: (() -> Unit)?,
    onNext: (() -> Unit)?,
    onShuffle: ((Boolean) -> Unit)?,
    onRepeat: (() -> Unit)?,
    onQueue: (() -> Unit)?,
    onDeviceVolume: ((Int) -> Unit)?,
    onDeviceMuted: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val motionEnabled = LocalKairoMotionEnabled.current
    val duration = durationMs?.coerceAtLeast(0L)
    val position = positionMs.coerceAtLeast(0L).let { if (duration != null) it.coerceAtMost(duration) else it }
    val progress = if (duration != null && duration > 0) position.toFloat() / duration else 0f
    var showDeviceControls by remember(track.id) { mutableStateOf(false) }
    var isSeeking by remember(track.id) { mutableStateOf(false) }
    var sliderValue by remember(track.id) { mutableFloatStateOf(progress) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = if (motionEnabled) tween(250, easing = LinearEasing) else snap(),
        label = "now-playing-progress",
    )
    val playInteractionSource = remember(track.id) { MutableInteractionSource() }
    val playPressed by playInteractionSource.collectIsPressedAsState()
    val playPressScale by animateFloatAsState(
        targetValue = if (motionEnabled && playPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "play-button-press",
    )
    val repeatLabel = when (repeatMode) {
        androidx.media3.common.Player.REPEAT_MODE_ONE -> "Repeat one"
        androidx.media3.common.Player.REPEAT_MODE_ALL -> "Repeat all"
        else -> "Repeat off"
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(KairoSpacing.large)) {
        Column(verticalArrangement = Arrangement.spacedBy(KairoSpacing.small)) {
            AnimatedContent(
                targetState = TrackTitleContent(track.id, track.title, track.artist),
                transitionSpec = {
                    if (motionEnabled) {
                        (fadeIn(tween(150)) + slideInVertically(tween(150)) { it / 10 }) togetherWith fadeOut(tween(100))
                    } else EnterTransition.None togetherWith ExitTransition.None
                },
                label = "now-playing-track-title",
            ) { content ->
                Column(verticalArrangement = Arrangement.spacedBy(KairoSpacing.small)) {
                    Text(
                        content.title,
                        modifier = Modifier.kairoSharedElement(sharedTransitionScope, "kairo-title-${content.trackId}", sharedElementVisible),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        content.artist,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            AnimatedVisibility(
                visible = isBuffering,
                enter = if (motionEnabled) fadeIn(tween(120)) else EnterTransition.None,
                exit = if (motionEnabled) fadeOut(tween(90)) else ExitTransition.None,
            ) {
                Text("Buffering", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(KairoSpacing.small)) {
            Slider(
                value = if (isSeeking) sliderValue else animatedProgress,
                onValueChange = { value ->
                    sliderValue = value
                    isSeeking = true
                },
                onValueChangeFinished = {
                    if (isSeeking && duration != null) onSeek?.invoke((sliderValue * duration).toLong())
                    isSeeking = false
                },
                enabled = duration != null && onSeek != null,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(position), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(duration?.let(::formatTime) ?: "--:--", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ActionIcon(
                Icons.Outlined.Shuffle,
                if (shuffleEnabled) "Turn shuffle off" else "Turn shuffle on",
                onClick = onShuffle?.let { callback -> { callback(!shuffleEnabled) } },
                active = shuffleEnabled,
            )
            ActionIcon(Icons.Outlined.SkipPrevious, "Previous track", onPrevious, large = true)
            Surface(color = MaterialTheme.colorScheme.primary, shape = androidx.compose.foundation.shape.CircleShape) {
                IconButton(
                    onClick = onPlayPause ?: {},
                    enabled = onPlayPause != null,
                    interactionSource = playInteractionSource,
                    modifier = Modifier
                        .size(64.dp)
                        .graphicsLayer { scaleX = playPressScale; scaleY = playPressScale }
                        .kairoSharedElement(sharedTransitionScope, "kairo-play-${track.id}", sharedElementVisible),
                ) {
                    AnimatedContent(
                        targetState = isPlaying,
                        transitionSpec = {
                            if (motionEnabled) fadeIn(tween(100)) togetherWith fadeOut(tween(80))
                            else EnterTransition.None togetherWith ExitTransition.None
                        },
                        label = "playback-icon",
                    ) { playing ->
                        Icon(
                            if (playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                            contentDescription = if (playing) "Pause playback" else "Start playback",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(KairoSizes.iconLarge),
                        )
                    }
                }
            }
            ActionIcon(Icons.Outlined.SkipNext, "Next track", onNext, large = true)
            ActionIcon(Icons.Outlined.Repeat, repeatLabel, onRepeat, active = repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onQueue ?: {}, enabled = onQueue != null, modifier = Modifier.size(KairoSizes.touchTarget)) {
                Icon(Icons.AutoMirrored.Outlined.QueueMusic, contentDescription = "Open queue")
            }
            quality?.let {
                QualityBadge(it.lossless, it.bitDepth, it.sampleRateHz, it.codec, it.channels)
            }
            IconButton(
                onClick = { showDeviceControls = !showDeviceControls },
                enabled = deviceVolume != null && maxDeviceVolume != null && maxDeviceVolume > 0,
                modifier = Modifier.size(KairoSizes.touchTarget),
            ) {
                Icon(Icons.AutoMirrored.Outlined.VolumeUp, contentDescription = "Device volume controls")
            }
        }
        if (showDeviceControls && deviceVolume != null && maxDeviceVolume != null && maxDeviceVolume > 0 && onDeviceVolume != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(KairoSpacing.small)) {
                IconButton(
                    onClick = { onDeviceMuted?.invoke(deviceMuted != true) },
                    enabled = onDeviceMuted != null,
                    modifier = Modifier.size(KairoSizes.touchTarget),
                ) {
                    Icon(
                        if (deviceMuted == true) Icons.Outlined.VolumeOff else Icons.Outlined.VolumeDown,
                        contentDescription = if (deviceMuted == true) "Unmute device" else "Mute device",
                    )
                }
                Slider(
                    value = (deviceVolume.toFloat() / maxDeviceVolume).coerceIn(0f, 1f),
                    onValueChange = { onDeviceVolume((it * maxDeviceVolume).toInt()) },
                    modifier = Modifier.weight(1f),
                    enabled = deviceMuted != true,
                )
                Icon(Icons.AutoMirrored.Outlined.VolumeUp, contentDescription = "Device volume", modifier = Modifier.size(KairoSizes.iconMedium))
            }
        }
    }
}

@Composable
private fun ActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: (() -> Unit)?,
    large: Boolean = false,
    active: Boolean = false,
) {
    val motionEnabled = LocalKairoMotionEnabled.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (motionEnabled && pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "playback-control-press",
    )
    IconButton(
        onClick = onClick ?: {},
        enabled = onClick != null,
        interactionSource = interactionSource,
        modifier = Modifier.size(if (large) 56.dp else KairoSizes.touchTarget)
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale },
    ) {
        Icon(
            icon,
            contentDescription = label,
            modifier = Modifier.size(if (large) KairoSizes.iconLarge else KairoSizes.iconMedium),
            tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

private data class TrackTitleContent(val trackId: String, val title: String, val artist: String)

private fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

@Composable
fun QueueScreen(
    tracks: List<TrackPresentation>,
    currentIndex: Int,
    onClear: (() -> Unit)? = null,
    onSelect: ((Int) -> Unit)? = null,
    onRemove: ((Int) -> Unit)? = null,
    onMove: ((Int, Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val motionEnabled = LocalKairoMotionEnabled.current
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = KairoSpacing.large, vertical = KairoSpacing.large),
        verticalArrangement = Arrangement.spacedBy(KairoSpacing.medium),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(KairoSpacing.small)) {
                ScreenHeading("Queue", "Now playing and coming up")
                SectionHeader("Playback queue", actionLabel = "Clear", onAction = onClear)
            }
        }
        if (tracks.isEmpty()) {
            item { EmptyState("Queue is clear", "Tracks added to play next will appear here.", icon = Icons.AutoMirrored.Outlined.QueueMusic) }
        } else {
            if (currentIndex > 0) {
                item { SectionHeader("Earlier in queue") }
                items((0 until currentIndex).toList(), key = { index -> "earlier-${tracks[index].id}" }) { index ->
                    QueueTrackRow(
                        track = tracks[index],
                        modifier = lazyItemMotion(motionEnabled),
                        artworkIndex = tracks[index].id.hashCode(), queueIndex = index,
                        queueSize = tracks.size, onClick = onSelect?.let { callback -> { callback(index) } },
                        onRemove = onRemove?.let { callback -> { callback(index) } },
                        onMoveToIndex = onMove,
                    )
                }
            }
            if (currentIndex in tracks.indices) {
                item { SectionHeader("Now Playing") }
                item {
                    QueueTrackRow(
                        track = tracks[currentIndex],
                        modifier = lazyItemMotion(motionEnabled),
                        artworkIndex = tracks[currentIndex].id.hashCode(), isCurrent = true,
                        queueIndex = currentIndex, queueSize = tracks.size,
                        onRemove = onRemove?.let { callback -> { callback(currentIndex) } },
                        onMoveToIndex = onMove,
                    )
                }
            }
            val nextStart = (currentIndex + 1).coerceAtLeast(0).coerceAtMost(tracks.size)
            item { SectionHeader("Up Next") }
            if (nextStart == tracks.size) item { SectionPlaceholder("No upcoming tracks.") }
            else items((nextStart until tracks.size).toList(), key = { index -> "up-next-${tracks[index].id}" }) { index ->
                QueueTrackRow(
                    track = tracks[index],
                    modifier = lazyItemMotion(motionEnabled),
                    artworkIndex = tracks[index].id.hashCode(), queueIndex = index,
                    queueSize = tracks.size, onClick = onSelect?.let { callback -> { callback(index) } },
                    onRemove = onRemove?.let { callback -> { callback(index) } },
                    onMoveToIndex = onMove,
                )
            }
        }
    }
}

private fun LazyItemScope.lazyItemMotion(motionEnabled: Boolean): Modifier = if (motionEnabled) {
    Modifier.animateItem(
        fadeInSpec = tween(130),
        placementSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow,
        ),
        fadeOutSpec = tween(100),
    )
} else {
    Modifier
}

@Composable
fun LibraryScreen(
    tracks: List<Track> = emptyList(),
    albums: List<Album> = emptyList(),
    artists: List<Artist> = emptyList(),
    playlists: List<PlaylistVisual> = emptyList(),
    recentlyPlayed: List<Track> = emptyList(),
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onTrackClick: ((Track) -> Unit)? = null,
    onAlbumClick: ((Album) -> Unit)? = null,
    onArtistClick: ((Artist) -> Unit)? = null,
    onPlaylistClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = KairoSpacing.large, vertical = KairoSpacing.large),
        verticalArrangement = Arrangement.spacedBy(KairoSpacing.large),
    ) {
        item { ScreenHeading("Library", "Everything in your collection.") }
        if (isLoading) {
            item { LoadingState() }
        } else if (errorMessage != null) {
            item { ErrorState(errorMessage) }
        } else {
        item { SectionHeader("Songs") }
        if (tracks.isEmpty()) item { EmptyState("No songs yet", "Music added to your library will appear here.", icon = Icons.Outlined.MusicNote) }
        else items(tracks, key = { "song-${it.sourceId}-${it.id}" }) { track ->
            TrackRow(track, artworkIndex = track.id.hashCode(), onClick = onTrackClick?.let { callback -> { callback(track) } })
        }

        item { SectionHeader("Albums") }
        if (albums.isEmpty()) item { EmptyState("No albums yet", "Albums in your library will appear here.") }
        else item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(KairoSpacing.medium)) {
                items(albums, key = { "album-${it.sourceId}-${it.id}" }) { album ->
                    AlbumCard(album, artworkIndex = album.id.hashCode(), onClick = onAlbumClick?.let { callback -> { callback(album) } })
                }
            }
        }

        item { SectionHeader("Artists") }
        if (artists.isEmpty()) item { EmptyState("No artists yet", "Artists in your library will appear here.") }
        else items(artists, key = { "artist-${it.sourceId}-${it.id}" }) { artist ->
            ArtistRow(artist, artworkIndex = artist.id.hashCode(), onClick = onArtistClick?.let { callback -> { callback(artist) } })
        }

        item { SectionHeader("Playlists") }
        if (playlists.isEmpty()) item { EmptyState("No playlists yet", "Playlists you create will appear here.") }
        else item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(KairoSpacing.medium)) {
                items(playlists, key = { it.id }) { playlist ->
                    PlaylistCard(playlist, artworkIndex = playlist.id.hashCode(), onClick = onPlaylistClick?.let { callback -> { callback(playlist.id) } })
                }
            }
        }

        item { SectionHeader("Recently Played") }
        if (recentlyPlayed.isEmpty()) item { EmptyState("Nothing played yet", "Recently played music will appear here.", icon = Icons.Outlined.History) }
        else items(recentlyPlayed, key = { "history-${it.sourceId}-${it.id}" }) { track ->
            TrackRow(track, artworkIndex = track.id.hashCode(), onClick = onTrackClick?.let { callback -> { callback(track) } })
        }
        }
    }
}

@Composable
fun DiagnosticsScreen(
    quality: AudioQualityDetails? = null,
    outputDevice: String? = null,
    isBitPerfect: Boolean? = null,
    playbackStatus: String,
    currentTrackTitle: String? = null,
    playbackPositionMs: Long,
    bufferedPositionMs: Long,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = KairoSpacing.large, vertical = KairoSpacing.large),
        verticalArrangement = Arrangement.spacedBy(KairoSpacing.large),
    ) {
        item { ScreenHeading("Audio Diagnostics", "A clear view of the active audio path.") }
        item { SectionHeader("Playback") }
        item { DiagnosticValue("State", playbackStatus) }
        currentTrackTitle?.let { item { DiagnosticValue("Track", it) } }
        item { DiagnosticValue("Position", formatTime(playbackPositionMs)) }
        item { DiagnosticValue("Buffered", formatTime(bufferedPositionMs)) }
        val hasQuality = quality?.let {
            it.lossless != null || it.bitDepth != null || it.sampleRateHz != null || it.codec != null || it.channels != null
        } == true
        if (!hasQuality && outputDevice == null && isBitPerfect == null) {
            item {
                EmptyState(
                    title = "No format reported",
                    message = "Stream details appear when the active decoder reports them.",
                    icon = Icons.Outlined.GraphicEq,
                )
            }
        } else {
            item { SectionHeader("Stream") }
            quality?.takeIf { hasQuality }?.let { actual ->
                if (actual.lossless != null) item { DiagnosticValue("Source quality", if (actual.lossless) "Lossless" else "Lossy") }
                if (actual.bitDepth != null) item { DiagnosticValue("Bit depth", "${actual.bitDepth}-bit") }
                if (actual.sampleRateHz != null) item { DiagnosticValue("Sample rate", "${actual.sampleRateHz} Hz") }
                if (actual.codec != null) item { DiagnosticValue("Codec", actual.codec.uppercase()) }
                if (actual.channels != null) item { DiagnosticValue("Channels", actual.channels) }
            }
            outputDevice?.let { item { DiagnosticValue("Output device", it) } }
            isBitPerfect?.let { item { DiagnosticValue("Bit-perfect path", if (it) "Active" else "Not active") } }
        }
    }
}

@Composable
private fun DiagnosticValue(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = KairoSpacing.small),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SettingsScreen(
    versionName: String,
    onAddMusicFolder: (() -> Unit)? = null,
    onOpenDiagnostics: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = KairoSpacing.large, vertical = KairoSpacing.large),
        verticalArrangement = Arrangement.spacedBy(KairoSpacing.large),
    ) {
        item { ScreenHeading("Settings") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(KairoSpacing.small)) {
                SectionHeader("Playback")
                Text("Playback controls are available from the player.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(KairoSpacing.small)) {
                SectionHeader("Audio Quality")
                Text("Inspect the active stream and output details.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onOpenDiagnostics ?: {}, enabled = onOpenDiagnostics != null) { Text("Open diagnostics") }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(KairoSpacing.small)) {
                SectionHeader("Library")
                Text("Choose local music folders for Kairo to index.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onAddMusicFolder ?: {}, enabled = onAddMusicFolder != null) {
                    Icon(Icons.Outlined.FolderOpen, contentDescription = null)
                    Spacer(Modifier.width(KairoSpacing.small))
                    Text("Add music folder")
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(KairoSpacing.small)) {
                SectionHeader("Appearance")
                Text("Kairo follows your device appearance setting.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(KairoSpacing.small)) {
                SectionHeader("About")
                Text("Kairo · $versionName", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
