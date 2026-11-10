package com.localbites.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.SignalCellularAlt1Bar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val OfflineRed = Color(0xFFD32F2F)
private val OfflineRedBg = Color(0xFFFFEBEE)
private val SlowAmber = Color(0xFFF57F17)
private val SlowAmberBg = Color(0xFFFFF8E1)

@Composable
fun OfflineBanner(
    isOnline: Boolean,
    isSlowNetwork: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = !isOnline || isSlowNetwork,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier,
    ) {
        if (!isOnline) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(OfflineRedBg)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.CloudOff,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = OfflineRed,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "You're offline. Showing cached data.",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = OfflineRed,
                )
            }
        } else if (isSlowNetwork) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SlowAmberBg)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.SignalCellularAlt1Bar,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = SlowAmber,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Slow connection detected",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = SlowAmber,
                )
            }
        }
    }
}
