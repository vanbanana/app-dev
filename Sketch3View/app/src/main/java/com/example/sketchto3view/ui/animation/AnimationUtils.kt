package com.example.sketchto3view.ui.animation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

// ============ Floating Card Shadow Modifier ============

/**
 * Applies a visible "floating" neumorphic shadow effect to cards.
 * Creates the clearly visible soft shadow underneath that makes cards
 * appear to float above the background surface.
 */
fun Modifier.floatingCard(
    elevation: Dp = 12.dp,
    shape: Shape = RoundedCornerShape(16.dp)
): Modifier = this.shadow(
    elevation = elevation,
    shape = shape,
    ambientColor = Color(0x1A000000),
    spotColor = Color(0x33000000)
)

/**
 * iOS-like spring animation spec - smooth and bouncy like Apple's default spring.
 */
val iosSpring: AnimationSpec<Float> = spring(
    dampingRatio = 0.8f,
    stiffness = 300f
)

/**
 * iOS-like spring for scale press effects - snappy response.
 */
val iosPressSpring: AnimationSpec<Float> = spring(
    dampingRatio = 0.7f,
    stiffness = 800f
)

/**
 * iOS-like spring for entrance animations - gentle and smooth.
 */
val iosEntranceSpring: AnimationSpec<Float> = spring(
    dampingRatio = 0.85f,
    stiffness = 200f
)

// ============ Page Transition Specs ============

private const val TRANSITION_DURATION = 300

fun fadeSlideInFromRight(): EnterTransition {
    return slideInHorizontally(
        initialOffsetX = { fullWidth -> (fullWidth * 0.25f).toInt() },
        animationSpec = tween(TRANSITION_DURATION, easing = androidx.compose.animation.core.EaseOutCubic)
    ) + fadeIn(
        animationSpec = tween(TRANSITION_DURATION, easing = androidx.compose.animation.core.EaseOutCubic)
    )
}

fun fadeSlideOutToLeft(): ExitTransition {
    return slideOutHorizontally(
        targetOffsetX = { fullWidth -> -(fullWidth * 0.25f).toInt() },
        animationSpec = tween(TRANSITION_DURATION, easing = androidx.compose.animation.core.EaseInCubic)
    ) + fadeOut(
        animationSpec = tween(TRANSITION_DURATION, easing = androidx.compose.animation.core.EaseInCubic)
    )
}

fun fadeSlideInFromLeft(): EnterTransition {
    return slideInHorizontally(
        initialOffsetX = { fullWidth -> -(fullWidth * 0.25f).toInt() },
        animationSpec = tween(TRANSITION_DURATION, easing = androidx.compose.animation.core.EaseOutCubic)
    ) + fadeIn(
        animationSpec = tween(TRANSITION_DURATION, easing = androidx.compose.animation.core.EaseOutCubic)
    )
}

fun fadeSlideOutToRight(): ExitTransition {
    return slideOutHorizontally(
        targetOffsetX = { fullWidth -> (fullWidth * 0.25f).toInt() },
        animationSpec = tween(TRANSITION_DURATION, easing = androidx.compose.animation.core.EaseInCubic)
    ) + fadeOut(
        animationSpec = tween(TRANSITION_DURATION, easing = androidx.compose.animation.core.EaseInCubic)
    )
}

// ============ Visibility Animations ============

fun slideUpFadeIn(): EnterTransition {
    return slideInVertically(
        initialOffsetY = { it / 4 },
        animationSpec = tween(350, easing = androidx.compose.animation.core.EaseOutCubic)
    ) + fadeIn(
        animationSpec = tween(350, easing = androidx.compose.animation.core.EaseOutCubic)
    )
}

fun slideDownFadeOut(): ExitTransition {
    return slideOutVertically(
        targetOffsetY = { it / 4 },
        animationSpec = tween(250, easing = androidx.compose.animation.core.EaseInCubic)
    ) + fadeOut(
        animationSpec = tween(250, easing = androidx.compose.animation.core.EaseInCubic)
    )
}

// ============ Press Effect Modifier ============

/**
 * iOS-like press effect that scales down the element on press.
 * Provides tactile feedback similar to iOS button presses.
 */
fun Modifier.pressClickEffect(
    targetScale: Float = 0.96f
): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) targetScale else 1f,
        animationSpec = iosPressSpring,
        label = "pressScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                isPressed = true
                val up = waitForUpOrCancellation()
                isPressed = false
            }
        }
}

// ============ Staggered Animation Helpers ============

/**
 * Data class to hold staggered animation state for a single item.
 */
data class StaggeredItemState(
    val alpha: Float = 0f,
    val offsetY: Float = 30f,
    val scale: Float = 0.95f
)

/**
 * Composable modifier that applies a staggered fade-in + slide-up animation.
 * Use with LaunchedEffect to trigger the animation with a delay based on index.
 */
@Composable
fun Modifier.staggeredFadeIn(
    index: Int,
    baseDelay: Long = 50L,
    duration: Int = 400
): Modifier {
    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(30f) }

    LaunchedEffect(Unit) {
        delay(index * baseDelay)
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(duration, easing = androidx.compose.animation.core.EaseOutCubic)
        )
    }

    LaunchedEffect(Unit) {
        delay(index * baseDelay)
        offsetY.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = 0.85f,
                stiffness = 200f
            )
        )
    }

    return this.graphicsLayer {
        this.alpha = alpha.value
        translationY = offsetY.value
    }
}

/**
 * Composable modifier for scale + fade entrance animation (good for headers/hero elements).
 */
@Composable
fun Modifier.scaleFadeIn(
    delay: Long = 0L,
    duration: Int = 500
): Modifier {
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.92f) }

    LaunchedEffect(Unit) {
        delay(delay)
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(duration, easing = androidx.compose.animation.core.EaseOutCubic)
        )
    }

    LaunchedEffect(Unit) {
        delay(delay)
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.8f,
                stiffness = 250f
            )
        )
    }

    return this.graphicsLayer {
        this.alpha = alpha.value
        scaleX = scale.value
        scaleY = scale.value
    }
}

/**
 * Composable modifier for slide-up entrance from bottom.
 */
@Composable
fun Modifier.slideUpEntrance(
    delay: Long = 0L,
    initialOffset: Float = 60f
): Modifier {
    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(initialOffset) }

    LaunchedEffect(Unit) {
        delay(delay)
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(350, easing = androidx.compose.animation.core.EaseOutCubic)
        )
    }

    LaunchedEffect(Unit) {
        delay(delay)
        offsetY.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = 0.85f,
                stiffness = 250f
            )
        )
    }

    return this.graphicsLayer {
        this.alpha = alpha.value
        translationY = offsetY.value
    }
}
