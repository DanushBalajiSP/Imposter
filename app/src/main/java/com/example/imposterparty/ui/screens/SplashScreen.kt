package com.example.imposterparty.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.imposterparty.R
import com.example.imposterparty.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    var startAnimation by remember { mutableStateOf(false) }

    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.6f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "scale",
    )

    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = EaseOutCubic),
        label = "alpha",
    )

    val titleAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800, delayMillis = 350, easing = EaseOutCubic),
        label = "titleAlpha",
    )

    val subtitleAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800, delayMillis = 650, easing = EaseOutCubic),
        label = "subtitleAlpha",
    )

    // Infinite breathing glow for the eyes and aura
    val infiniteTransition = rememberInfiniteTransition(label = "glowPulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseGlow",
    )

    LaunchedEffect(Unit) {
        delay(100)
        startAnimation = true
        delay(350)
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        delay(1800)
        onSplashFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF3B060F),
                        Color(0xFF1F0308),
                        DarkBackground,
                        Color(0xFF070002),
                    ),
                    center = Offset.Unspecified,
                    radius = 1200f,
                )
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                onSplashFinished()
            },
        contentAlignment = Alignment.Center,
    ) {
        // Animated Glowing Background Aura
        Canvas(modifier = Modifier.size(320.dp)) {
            val radius = size.minDimension / 2 * pulseGlow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        DangerRed.copy(alpha = 0.35f * alphaAnim * pulseGlow),
                        Color(0xFF880E4F).copy(alpha = 0.15f * alphaAnim),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp),
        ) {
            // Icon Container with glowing background & shadow
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(160.dp)
                    .scale(scaleAnim)
                    .alpha(alphaAnim)
                    .graphicsLayer {
                        shadowElevation = 24.dp.toPx()
                    }
            ) {
                // Outer Radial Ring
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0xFF4A0813), Color(0xFF140104))
                            )
                        )
                )

                // Foreground Vector Icon
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "Imposter Icon",
                    modifier = Modifier.size(150.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Title "IMPOSTER"
            Text(
                text = "IMPOSTER",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 6.sp,
                ),
                color = DangerRed,
                modifier = Modifier
                    .alpha(titleAlpha)
                    .scale(if (startAnimation) 1f else 0.8f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle "PARTY"
            Text(
                text = "P A R T Y",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Light,
                    letterSpacing = 10.sp,
                ),
                color = ImposterSecondary,
                modifier = Modifier.alpha(subtitleAlpha)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Tagline
            Text(
                text = "Trust No One",
                style = MaterialTheme.typography.bodySmall,
                color = TextOnDarkSecondary.copy(alpha = 0.7f),
                modifier = Modifier.alpha(subtitleAlpha)
            )
        }
    }
}
