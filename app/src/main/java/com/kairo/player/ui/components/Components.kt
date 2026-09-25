package com.kairo.player.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import com.kairo.player.domain.model.Album
import com.kairo.player.domain.model.Artist
import com.kairo.player.domain.model.Track
import com.kairo.player.ui.TrackPresentation
import com.kairo.player.ui.LocalKairoMotionEnabled
import com.kairo.player.ui.theme.KairoCorners
import com.kairo.player.ui.theme.KairoElevation
import com.kairo.player.ui.theme.KairoSizes
import com.kairo.player.ui.theme.KairoSpacing

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction, modifier = Modifier.height(KairoSizes.touchTarget)) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun QualityBadge(
    lossless: Boolean? = null,
    bitDepth: Int? = null,
    sampleRateHz: Int? = null,
    codec: String? = null,
    channels: String? = null,
    modifier: Modifier = Modifier,
) {
    val resolution = buildList {
        if (bitDepth != null) add("$bitDepth-bit")
        if (sampleRateHz != null) add("${sampleRateHz / 1000.0} kHz".trimEnd('0').trimEnd('.'))
    }
    val encoding = buildList {
        if (codec != null) add(codec.uppercase())
        if (channels != null) add(channels)
    }
    if (lossless == null && resolution.isEmpty() && encoding.isEmpty()) return
    val badgeContent = QualityBadgeContent(
        lossLabel = lossless?.let { if (it) "LOSSLESS" else "LOSSY" },
        resolution = resolution.joinToString(" · "),
        encoding = encoding.joinToString(" · "),
    )
    val motionEnabled = LocalKairoMotionEnabled.current

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(KairoCorners.small),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = KairoSpacing.small, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(KairoSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.GraphicEq, contentDescription = null, modifier = Modifier.size(KairoSizes.iconSmall))
            AnimatedContent(
                targetState = badgeContent,
                transitionSpec = {
                    if (motionEnabled) fadeIn(tween(130)) togetherWith fadeOut(tween(100))
                    else EnterTransition.None togetherWith ExitTransition.None
                },
                label = "quality-badge",
            ) { content ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    content.lossLabel?.let { Text(it, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                    if (content.resolution.isNotEmpty()) Text(content.resolution, style = MaterialTheme.typography.labelSmall)
                    if (content.encoding.isNotEmpty()) Text(content.encoding, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

private data class QualityBadgeContent(
    val lossLabel: String?,
    val resolution: String,
    val encoding: String,
)

private val ArtworkPalettes = listOf(
    listOf(Color(0xFF1F6355), Color(0xFFB2C9A4)),
    listOf(Color(0xFFB75C3E), Color(0xFFF0B676)),
    listOf(Color(0xFF344C7A), Color(0xFF8EABC8)),
    listOf(Color(0xFF6D4C71), Color(0xFFD3A6B7)),
    listOf(Color(0xFF8A6A2B), Color(0xFFE0C978)),
    listOf(Color(0xFF366B77), Color(0xFFA6D0C9)),
)

@Composable
fun KairoArtwork(
    index: Int,
    modifier: Modifier = Modifier,
    circular: Boolean = false,
) {
    val motionEnabled = LocalKairoMotionEnabled.current
    AnimatedContent(
        targetState = index,
        modifier = modifier,
        contentAlignment = Alignment.Center,
        transitionSpec = {
            if (motionEnabled) fadeIn(tween(150)) togetherWith fadeOut(tween(120))
            else EnterTransition.None togetherWith ExitTransition.None
        },
        label = "artwork-crossfade",
    ) { artworkIndex ->
        ArtworkCanvas(artworkIndex, Modifier.fillMaxSize(), circular)
    }
}

@Composable
private fun ArtworkCanvas(index: Int, modifier: Modifier, circular: Boolean) {
    val colors = ArtworkPalettes[index.mod(ArtworkPalettes.size)]
    val shape = if (circular) CircleShape else RoundedCornerShape(KairoCorners.small)

    Canvas(
        modifier = modifier
            .clip(shape)
            .background(Brush.linearGradient(colors))
            .semantics { contentDescription = "Artwork placeholder" },
    ) {
        val minSide = size.minDimension
        drawCircle(
            color = Color.White.copy(alpha = 0.18f),
            radius = minSide * 0.34f,
            center = Offset(size.width * 0.72f, size.height * 0.28f),
        )
        drawCircle(
            color = colors.first().copy(alpha = 0.7f),
            radius = minSide * 0.48f,
            center = Offset(size.width * 0.28f, size.height * 0.78f),
            style = Stroke(width = minSide * 0.13f),
        )
        for (line in 0..4) {
            val x = size.width * (0.43f + line * 0.075f)
            drawLine(
                color = Color.White.copy(alpha = 0.7f - line * 0.08f),
                start = Offset(x, size.height * 0.42f),
                end = Offset(x, size.height * (0.57f + (line % 2) * 0.13f)),
                strokeWidth = minSide * 0.018f,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
fun TrackRow(
    track: Track,
    modifier: Modifier = Modifier,
    artworkIndex: Int = 0,
    subtitle: String = track.artists.joinToString { it.name }.ifBlank { "Unknown artist" },
    trailingIcon: ImageVector? = null,
    trailingLabel: String? = null,
    onTrailingClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = KairoSizes.touchTarget)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(vertical = KairoSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KairoSpacing.medium),
    ) {
        KairoArtwork(artworkIndex, Modifier.size(KairoSizes.trackArtwork))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(track.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (trailingIcon != null && trailingLabel != null) {
            IconButton(onClick = onTrailingClick ?: {}, modifier = Modifier.size(KairoSizes.touchTarget)) {
                Icon(trailingIcon, contentDescription = trailingLabel)
            }
        }
    }
}

@Composable
fun AlbumCard(album: Album, modifier: Modifier = Modifier, artworkIndex: Int = 0, onClick: (() -> Unit)? = null) {
    Column(
        modifier = modifier
            .width(156.dp)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
        verticalArrangement = Arrangement.spacedBy(KairoSpacing.small),
    ) {
        KairoArtwork(artworkIndex, Modifier.fillMaxWidth().aspectRatio(1f))
        Text(album.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            album.artists.joinToString { it.name }.ifBlank { "Album" },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun ArtistRow(artist: Artist, modifier: Modifier = Modifier, artworkIndex: Int = 0, onClick: (() -> Unit)? = null) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(KairoSizes.touchTarget + KairoSpacing.medium)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
        horizontalArrangement = Arrangement.spacedBy(KairoSpacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KairoArtwork(artworkIndex, Modifier.size(KairoSizes.trackArtwork), circular = true)
        Text(artist.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

data class PlaylistVisual(
    val id: String,
    val name: String,
    val trackCount: Int? = null,
)

@Composable
fun PlaylistCard(
    playlist: PlaylistVisual,
    modifier: Modifier = Modifier,
    artworkIndex: Int = 0,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .width(156.dp)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
        verticalArrangement = Arrangement.spacedBy(KairoSpacing.small),
    ) {
        KairoArtwork(artworkIndex, Modifier.fillMaxWidth().aspectRatio(1f))
        Text(playlist.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        playlist.trackCount?.let { count ->
            Text("$count songs", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MiniPlayer(
    track: TrackPresentation?,
    isPlaying: Boolean,
    progress: Float? = null,
    modifier: Modifier = Modifier,
    artworkIndex: Int = 0,
    onPlayPause: (() -> Unit)? = null,
    onOpenNowPlaying: (() -> Unit)? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    sharedElementVisible: Boolean = true,
) {
    if (track == null) return
    val motionEnabled = LocalKairoMotionEnabled.current
    val animatedProgress by animateFloatAsState(
        targetValue = progress?.coerceIn(0f, 1f) ?: 0f,
        animationSpec = if (motionEnabled) tween(260, easing = LinearEasing) else snap(),
        label = "mini-player-progress",
    )
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(KairoElevation.low, RoundedCornerShape(KairoCorners.medium)),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(KairoCorners.medium),
        onClick = onOpenNowPlaying ?: {},
        enabled = onOpenNowPlaying != null,
    ) {
        Box {
            Row(
                modifier = Modifier.padding(horizontal = KairoSpacing.medium, vertical = KairoSpacing.small),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(KairoSpacing.medium),
            ) {
                KairoArtwork(
                    artworkIndex,
                    Modifier.size(KairoSizes.miniPlayerArtwork).kairoSharedElement(
                        sharedTransitionScope,
                        "kairo-artwork-${track.id}",
                        sharedElementVisible,
                    ),
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        track.title,
                        modifier = Modifier.kairoSharedElement(sharedTransitionScope, "kairo-title-${track.id}", sharedElementVisible),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(track.artist, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                val interactionSource = remember { MutableInteractionSource() }
                val pressed by interactionSource.collectIsPressedAsState()
                val pressScale by animateFloatAsState(
                    targetValue = if (motionEnabled && pressed) 0.96f else 1f,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMedium,
                    ),
                    label = "mini-play-button-press",
                )
                IconButton(
                    onClick = onPlayPause ?: {},
                    enabled = onPlayPause != null,
                    interactionSource = interactionSource,
                    modifier = Modifier
                        .size(KairoSizes.touchTarget)
                        .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
                        .kairoSharedElement(sharedTransitionScope, "kairo-play-${track.id}", sharedElementVisible),
                ) {
                    AnimatedContent(
                        targetState = isPlaying,
                        transitionSpec = {
                            if (motionEnabled) fadeIn(tween(100)) togetherWith fadeOut(tween(80))
                            else EnterTransition.None togetherWith ExitTransition.None
                        },
                        label = "mini-playback-icon",
                    ) { playing ->
                        Icon(
                            if (playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                            contentDescription = if (playing) "Pause playback" else "Start playback",
                        )
                    }
                }
            }
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { if (motionEnabled) animatedProgress else progress.coerceIn(0f, 1f) },
                    modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().height(2.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent,
                )
            }
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.LibraryMusic,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = KairoSpacing.xxLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(KairoSpacing.medium),
    ) {
        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = CircleShape) {
            Icon(icon, contentDescription = null, modifier = Modifier.padding(KairoSpacing.medium).size(KairoSizes.iconLarge), tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (actionLabel != null && onAction != null) {
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun LoadingState(message: String = "Loading your music", modifier: Modifier = Modifier) {
    val motionEnabled = LocalKairoMotionEnabled.current
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = KairoSpacing.xxLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(KairoSpacing.medium),
    ) {
        AnimatedVisibility(
            visible = true,
            enter = if (motionEnabled) fadeIn(tween(140)) else EnterTransition.None,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(KairoSpacing.small)) {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = CircleShape) {
                    Icon(
                        Icons.Outlined.GraphicEq,
                        contentDescription = null,
                        modifier = Modifier.padding(KairoSpacing.small).size(KairoSizes.iconMedium),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun ErrorState(message: String, modifier: Modifier = Modifier, onRetry: (() -> Unit)? = null) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = KairoSpacing.xxLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(KairoSpacing.medium),
    ) {
        Icon(Icons.Outlined.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(KairoSizes.iconLarge))
        Text("Something went wrong", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (onRetry != null) Button(onClick = onRetry) { Text("Try again") }
    }
}

@Composable
fun QueueTrackRow(
    track: TrackPresentation,
    modifier: Modifier = Modifier,
    artworkIndex: Int = 0,
    isCurrent: Boolean = false,
    onRemove: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    queueIndex: Int = -1,
    queueSize: Int = 0,
    onMoveToIndex: ((Int, Int) -> Unit)? = null,
) {
    val latestIndex by rememberUpdatedState(queueIndex)
    val latestMove by rememberUpdatedState(onMoveToIndex)
    val density = LocalDensity.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(if (isCurrent) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.58f) else Color.Transparent, RoundedCornerShape(KairoCorners.medium))
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(start = KairoSpacing.small, end = KairoSpacing.xSmall, top = KairoSpacing.small, bottom = KairoSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KairoSpacing.medium),
    ) {
        KairoArtwork(artworkIndex, Modifier.size(KairoSizes.trackArtwork))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(track.title, style = MaterialTheme.typography.bodyLarge, fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onRemove ?: {}, enabled = onRemove != null, modifier = Modifier.size(KairoSizes.touchTarget)) {
            Icon(Icons.Outlined.Close, contentDescription = "Remove ${track.title} from queue")
        }
        Box(
            modifier = Modifier
                .size(KairoSizes.touchTarget)
                .semantics {
                    contentDescription = "Reorder ${track.title}"
                    customActions = listOf(
                        CustomAccessibilityAction("Move up") {
                            val target = latestIndex - 1
                            if (latestIndex < 0 || target < 0 || latestMove == null) false
                            else { latestMove?.invoke(latestIndex, target); true }
                        },
                        CustomAccessibilityAction("Move down") {
                            val target = latestIndex + 1
                            if (latestIndex < 0 || target >= queueSize || latestMove == null) false
                            else { latestMove?.invoke(latestIndex, target); true }
                        },
                    )
                }
                .then(
                    if (onMoveToIndex != null) Modifier.pointerInput(track.id) {
                        var dragDistance = 0f
                        var movingIndex = latestIndex
                        detectDragGesturesAfterLongPress(
                            onDrag = { change, amount ->
                                change.consume()
                                dragDistance += amount.y
                                val step = with(density) { 56.dp.toPx() }
                                while (dragDistance >= step && movingIndex + 1 < queueSize) {
                                    latestMove?.invoke(movingIndex, movingIndex + 1)
                                    movingIndex += 1
                                    dragDistance -= step
                                }
                                while (dragDistance <= -step && movingIndex - 1 >= 0) {
                                    latestMove?.invoke(movingIndex, movingIndex - 1)
                                    movingIndex -= 1
                                    dragDistance += step
                                }
                            },
                            onDragEnd = { dragDistance = 0f },
                            onDragCancel = { dragDistance = 0f },
                        )
                    } else Modifier,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.DragHandle, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ResponsiveContent(
    modifier: Modifier = Modifier,
    content: @Composable (wide: Boolean) -> Unit,
) {
    BoxWithConstraints(modifier = modifier) {
        content(maxWidth >= KairoSizes.contentMaxWidth)
    }
}

@Composable
fun IconAction(
    icon: ImageVector,
    label: String,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick ?: {}, modifier = modifier.size(KairoSizes.touchTarget).semantics { contentDescription = label }) {
        Icon(icon, contentDescription = null)
    }
}

@Composable
fun ArtworkBorder(modifier: Modifier = Modifier) {
    Spacer(
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(KairoCorners.small))
            .clip(RoundedCornerShape(KairoCorners.small)),
    )
}

