package com.aura.ai.core.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Direct Compose counterparts of the @keyframes used across the .dc.html screens
 * (auraPulse / auraSpin / auraSpinRev / auraScan / auraFloat / auraBoot / auraFlicker / auraBar).
 * Durations are kept in milliseconds to mirror the CSS second values exactly.
 */
object AuraMotion {
    val CssEase = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
}

/** auraPulse: opacity .55<->1, scale 1<->1.06, ease-in-out, alternating. */
@Composable
fun rememberPulse(durationMillis: Int = 2400): Pair<State<Float>, State<Float>> {
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale =
        transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.06f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis / 2, easing = EaseInOut),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "pulseScale",
        )
    val alpha =
        transition.animateFloat(
            initialValue = 0.55f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis / 2, easing = EaseInOut),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "pulseAlpha",
        )
    return scale to alpha
}

/** auraSpin / auraSpinRev: continuous linear rotation. */
@Composable
fun rememberRotation(
    durationMillis: Int,
    reverse: Boolean = false,
): State<Float> {
    val transition = rememberInfiniteTransition(label = "spin")
    return transition.animateFloat(
        initialValue = if (reverse) 360f else 0f,
        targetValue = if (reverse) 0f else 360f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis, easing = LinearEasing),
            ),
        label = "rotation",
    )
}

/** auraScan: a highlight band translating from -100% to 100% of its bounds, looping. */
@Composable
fun rememberScanProgress(durationMillis: Int): State<Float> {
    val transition = rememberInfiniteTransition(label = "scan")
    return transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis, easing = LinearEasing)),
        label = "scanProgress",
    )
}

/** auraFloat: gentle vertical bob, 0 to -6dp and back, over 5s. */
@Composable
fun rememberFloatBob(durationMillis: Int = 5000): State<Float> {
    val transition = rememberInfiniteTransition(label = "float")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis / 2, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "floatBob",
    )
}

/** auraFlicker: opacity holds at 1, dips to .4 briefly near the end of each 2.6s cycle. */
@Composable
fun rememberFlicker(durationMillis: Int = 2600): State<Float> {
    val transition = rememberInfiniteTransition(label = "flicker")
    return transition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    keyframes {
                        this.durationMillis = durationMillis
                        1f at 0
                        1f at (durationMillis * 0.92f).toInt()
                        0.4f at (durationMillis * 0.94f).toInt()
                        1f at (durationMillis * 0.96f).toInt()
                        1f at durationMillis
                    },
            ),
        label = "flickerAlpha",
    )
}

/** auraBoot: a progress fill that plays 0->1 exactly once, CSS `ease` timing. */
@Composable
fun rememberBootProgress(durationMillis: Int = 2400): State<Float> {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    return animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(durationMillis, easing = AuraMotion.CssEase),
        label = "bootProgress",
    )
}

/** auraBar: per-bar equalizer scaleY, staggered per index — quiet & static when [active] is false. */
@Composable
fun rememberEqualizerBarScale(
    index: Int,
    active: Boolean,
): State<Float> {
    val transition = rememberInfiniteTransition(label = "bar$index")
    val durationMillis = ((0.5f + (index % 4) * 0.12f) * 1000).toInt()
    val animated =
        transition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis, easing = EaseInOut),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "barScale$index",
        )
    return if (active) animated else remember { mutableFloatStateOf(0.3f) }
}
