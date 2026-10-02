package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.Screen
import com.example.ui.theme.PaletteCornflower
import com.example.ui.theme.PaletteIceCyan
import com.example.ui.theme.PaletteMintFrost
import com.example.ui.theme.PaletteSoftSky

/**
 * Modern Glassmorphic Floating Bottom Navigation Dock for Secondary Brain 2.0.
 * Designed with tactile pill active states, frosted translucent wash, and smooth spring animations.
 */
@Composable
fun ModernBottomNavigationDock(
    screens: List<Screen>,
    currentRoute: String,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val dockBorderBrush = remember {
        Brush.horizontalGradient(
            listOf(
                PaletteCornflower.copy(alpha = 0.60f),
                PaletteSoftSky.copy(alpha = 0.80f),
                PaletteIceCyan.copy(alpha = 0.60f)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color.White.copy(alpha = 0.92f),
            shadowElevation = 8.dp,
            modifier = Modifier
                .border(
                    BorderStroke(1.2.dp, dockBorderBrush),
                    RoundedCornerShape(26.dp)
                )
                .shadow(
                    elevation = 10.dp,
                    shape = RoundedCornerShape(26.dp),
                    spotColor = PaletteCornflower.copy(alpha = 0.25f)
                )
                .testTag("modern_bottom_navigation_dock")
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 6.dp, vertical = 6.dp)
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                screens.forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    ModernDockItem(
                        screen = screen,
                        isSelected = isSelected,
                        onClick = { onNavigate(screen) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernDockItem(
    screen: Screen,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) PaletteCornflower else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "dock_container_color"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else PaletteCornflower,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "dock_content_color"
    )

    val itemPaddingHorizontal by animateDpAsState(
        targetValue = if (isSelected) 14.dp else 10.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "dock_padding_horizontal"
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .testTag("dock_item_${screen.route}")
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = itemPaddingHorizontal,
                vertical = 8.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = screen.icon,
                contentDescription = screen.title,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )

            if (isSelected) {
                Text(
                    text = when (screen) {
                        is Screen.Dashboard -> "Overview"
                        is Screen.ChatAi -> "Brain AI"
                        is Screen.AgentStudio -> "Agent Hub"
                        is Screen.Tasks -> "Tasks"
                        is Screen.Alarm -> "Alarms"
                        is Screen.Recorder -> "Record"
                        else -> screen.title
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = contentColor,
                    letterSpacing = 0.2.sp
                )
            }
        }
    }
}
