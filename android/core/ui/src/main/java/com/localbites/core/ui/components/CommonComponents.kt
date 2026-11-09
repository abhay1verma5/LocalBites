package com.localbites.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
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

// Rating chip
@Composable
fun RatingChip(
    rating: Float,
    totalRatings: Int? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = if (rating >= 4.0f) Green60 else if (rating >= 3.0f) Orange60 else Color(0xFFE53935),
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
        text = "₹${String.format("%.0f", price)}",
        style = style,
        fontWeight = FontWeight.Bold,
        modifier = modifier,
    )
}

// Shimmer loading placeholder
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp)
) {
    var shimmerAlpha by remember { mutableFloatStateOf(0.3f) }
    val alpha by animateFloatAsState(
        targetValue = shimmerAlpha,
        animationSpec = tween(700),
        finishedListener = {
            shimmerAlpha = if (shimmerAlpha == 0.3f) 0.7f else 0.3f
        },
        label = "shimmer"
    )

    LaunchedEffect(Unit) {
        shimmerAlpha = 0.7f
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
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

// Empty state
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

// Order status stepper
@Composable
fun OrderStatusStepper(
    currentStatus: String,
    modifier: Modifier = Modifier
) {
    val statuses = listOf("PLACED", "CONFIRMED", "PREPARING", "READY", "PICKED_UP", "ON_THE_WAY", "DELIVERED")
    val labels = listOf("Placed", "Confirmed", "Preparing", "Ready", "Picked Up", "On the Way", "Delivered")
    val currentIndex = statuses.indexOf(currentStatus)

    Column(modifier = modifier.padding(16.dp)) {
        statuses.forEachIndexed { index, status ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            if (index <= currentIndex) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (index < currentIndex) {
                        Text("✓", color = Color.White, style = MaterialTheme.typography.labelSmall)
                    } else if (index == currentIndex) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = labels[index],
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (index == currentIndex) FontWeight.Bold else FontWeight.Normal,
                    color = if (index <= currentIndex) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            }
            if (index < statuses.lastIndex) {
                Box(
                    modifier = Modifier
                        .padding(start = 11.dp)
                        .width(2.dp)
                        .height(16.dp)
                        .background(
                            if (index < currentIndex) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                )
            }
        }
    }
}
