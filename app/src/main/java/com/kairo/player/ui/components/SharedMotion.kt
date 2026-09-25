package com.kairo.player.ui.components

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.Modifier

@OptIn(ExperimentalSharedTransitionApi::class)
@androidx.compose.runtime.Composable
fun Modifier.kairoSharedElement(
    sharedTransitionScope: SharedTransitionScope?,
    key: String,
    enabled: Boolean,
): Modifier {
    if (sharedTransitionScope == null || !enabled) return this
    return with(sharedTransitionScope) {
        sharedElementWithCallerManagedVisibility(
            sharedContentState = rememberSharedContentState(key),
            visible = enabled,
            boundsTransform = { _, _ ->
                spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium,
                )
            },
        )
    }
}
