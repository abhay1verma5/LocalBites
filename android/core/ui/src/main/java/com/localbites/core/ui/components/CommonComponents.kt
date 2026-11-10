package com.localbites.core.ui.components

import androidx.annotation.RawRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.TwoWheeler
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.localbites.core.ui.R
import com.localbites.core.ui.theme.*

// Veg/NonVeg indicator dot
@Composable
fun FoodTypeIndicator(
    foodType: String,
    modifier: Modifier = Modifier
) {
    val color = when (foodType) {
        "VEG" -> VegGreen
        "NON_VEG" -> NonVegRed
        "EGG" -> EggYellow
        else -> Color.Gray
    }
    Box(
        modifier = modifier
            .size(16.dp)
            .background(Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
                .align(Alignment.Center)
        )
    }
}

// Rating chip with Swiggy-style green
@Composable
fun RatingChip(
    rating: Float,
    totalRatings: Int? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = if (rating >= 4.0f) RatingGreen
        else if (rating >= 3.0f) Orange60
        else Color(0xFFE53935),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = String.format("%.1f", rating),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = Color.White,
            )
            if (totalRatings != null) {
                Text(
                    text = "(${totalRatings})",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f),
                )
            }
        }
    }
}

// Price text
@Composable
fun PriceText(
    price: Double,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium,
) {
    Text(
        text = "\u20B9${String.format("%.0f", price)}",
        style = style,
        fontWeight = FontWeight.Bold,
        modifier = modifier,
    )
}

// Sliding gradient shimmer (Swiggy-style left-to-right sweep)
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp)
) {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_translate",
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 300f, 0f),
        end = Offset(translateAnim, 0f),
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(brush)
    )
}

// Loading indicator
@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

// Lottie-powered empty state
@Composable
fun LottieEmptyState(
    @RawRes animationRes: Int,
    title: String,
    subtitle: String = "",
    modifier: Modifier = Modifier,
    animationSize: Int = 200,
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(animationRes))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.size(animationSize.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = CharcoalText,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        if (subtitle.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryText,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// Plain text empty state (fallback)
@Composable
fun EmptyState(
    title: String,
    subtitle: String = "",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (subtitle.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// Order status stepper with icons and animated progress
@Composable
fun OrderStatusStepper(
    currentStatus: String,
    modifier: Modifier = Modifier
) {
    val statuses = listOf("PLACED", "CONFIRMED", "PREPARING", "READY", "PICKED_UP", "ON_THE_WAY", "DELIVERED")
    val labels = listOf(
        stringResource(R.string.status_placed),
        stringResource(R.string.status_confirmed),
        stringResource(R.string.status_preparing),
        stringResource(R.string.status_ready),
        stringResource(R.string.status_picked_up),
        stringResource(R.string.status_on_the_way),
        stringResource(R.string.status_delivered),
    )
    val stepIcons = listOf(
        Icons.Outlined.Receipt,       // Placed
        Icons.Outlined.CheckCircle,   // Confirmed
        Icons.Outlined.Restaurant,    // Preparing
        Icons.Outlined.CheckCircle,   // Ready
        Icons.Outlined.TwoWheeler,    // Picked up
        Icons.Outlined.TwoWheeler,    // On the way
        Icons.Outlined.Home,          // Delivered
    )
    val currentIndex = statuses.indexOf(currentStatus)

    // Pulse animation for active step
    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_alpha",
    )

    Column(modifier = modifier.padding(16.dp)) {
        statuses.forEachIndexed { index, _ ->
            val isCompleted = index < currentIndex
            val isActive = index == currentIndex
            val isPending = index > currentIndex

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                // Step circle with icon
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isCompleted -> RatingGreen
                                isActive -> MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Outlined.CheckCircle else stepIcons[index],
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (isPending) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        else Color.White,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = labels[index],
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isActive) FontWeight.Bold else if (isCompleted) FontWeight.Medium else FontWeight.Normal,
                    color = when {
                        isActive -> MaterialTheme.colorScheme.primary
                        isCompleted -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    },
                )
            }

            // Animated connecting line
            if (index < statuses.lastIndex) {
                val lineProgress by animateFloatAsState(
                    targetValue = when {
                        index < currentIndex -> 1f
                        index == currentIndex -> 0.5f
                        else -> 0f
                    },
                    animationSpec = tween(600),
                    label = "line_progress",
                )

                Box(modifier = Modifier.padding(start = 15.dp)) {
                    // Background line
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(20.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    // Progress fill
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height((20 * lineProgress).dp)
                            .background(
                                if (index < currentIndex) RatingGreen
                                else MaterialTheme.colorScheme.primary
                            )
                    )
                }
            }
        }
    }
}
