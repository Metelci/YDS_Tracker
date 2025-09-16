package com.mtlc.studyplan.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtlc.studyplan.ui.theme.DesignTokens

// Typography following Google Sans Display pattern - fallback to system fonts
val StudyPlanFontFamily = FontFamily.Default

@Composable
fun StudyBottomNav(
    currentRoute: String,
    tabs: List<Triple<String, ImageVector, String>>,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = DesignTokens.Surface,
        shadowElevation = 8.dp
    ) {
        NavigationBar(
            containerColor = DesignTokens.Surface,
            contentColor = DesignTokens.Foreground,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEach { (route, icon, label) ->
                val isSelected = currentRoute.startsWith(route)

                // Enhanced selection animation
                val selectionScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.05f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "navigation_selection_scale"
                )

                // Color transition for icon
                val iconColor by animateColorAsState(
                    targetValue = if (isSelected) DesignTokens.Primary else DesignTokens.MutedForeground,
                    animationSpec = tween(durationMillis = 300),
                    label = "navigation_icon_color"
                )

                // Label color transition
                val labelColor by animateColorAsState(
                    targetValue = if (isSelected) DesignTokens.Primary else DesignTokens.MutedForeground,
                    animationSpec = tween(durationMillis = 300),
                    label = "navigation_label_color"
                )

                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onTabSelected(route) },
                    icon = {
                        Box {
                            // Active indicator background
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(DesignTokens.PrimaryContainer.copy(alpha = 0.3f))
                                        .scale(selectionScale)
                                )
                            }

                            // Icon with enhanced animations
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = iconColor,
                                modifier = Modifier
                                    .size(24.dp)
                                    .align(Alignment.Center)
                                    .scale(selectionScale)
                            )
                        }
                    },
                    label = {
                        Text(
                            text = label,
                            color = labelColor,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            fontFamily = StudyPlanFontFamily,
                            modifier = Modifier.scale(if (isSelected) 1.05f else 1f)
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DesignTokens.Primary,
                        unselectedIconColor = DesignTokens.MutedForeground,
                        selectedTextColor = DesignTokens.Primary,
                        unselectedTextColor = DesignTokens.MutedForeground,
                        indicatorColor = DesignTokens.PrimaryContainer.copy(alpha = 0.12f)
                    )
                )
            }
        }
    }
}