package com.example.imposterparty.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.imposterparty.theme.*

@Composable
fun HomeScreen(
    onNewGame: () -> Unit,
    onWordPacks: () -> Unit,
    onScoreboard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val animOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bgShift",
    )

    Box(
        modifier = modifier
            .background(
                Brush.linearGradient(
                    colors = listOf(GradientStart, GradientMid, GradientEnd, DarkBackground),
                    start = Offset(animOffset, 0f),
                    end = Offset(animOffset + 500f, 1500f),
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            // Emoji icon
            Text(
                text = "🕵️",
                fontSize = 72.sp,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                text = "IMPOSTER",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp,
                ),
                color = ImposterPrimary,
            )
            Text(
                text = "PARTY",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Light,
                    letterSpacing = 8.sp,
                ),
                color = ImposterSecondary,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Find the imposter among your friends!",
                style = MaterialTheme.typography.bodyMedium,
                color = TextOnDarkSecondary,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(48.dp))

            // New Game Button
            HomeButton(
                text = "New Game",
                icon = Icons.Default.PlayArrow,
                colors = listOf(ImposterPrimary, ImposterPrimaryDark),
                onClick = onNewGame,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Word Packs Button
            HomeButton(
                text = "Word Packs",
                icon = Icons.AutoMirrored.Filled.LibraryBooks,
                colors = listOf(ImposterSecondaryDark, ImposterSecondary),
                onClick = onWordPacks,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Scoreboard Button
            HomeButton(
                text = "Scoreboard",
                icon = Icons.Default.EmojiEvents,
                colors = listOf(ImposterTertiaryDark, ImposterTertiary),
                onClick = onScoreboard,
            )
        }
    }
}

@Composable
private fun HomeButton(
    text: String,
    icon: ImageVector,
    colors: List<Color>,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(colors),
                    shape = RoundedCornerShape(16.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
