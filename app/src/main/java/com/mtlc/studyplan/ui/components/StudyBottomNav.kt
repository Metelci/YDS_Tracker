package com.mtlc.studyplan.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
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
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE3F2FD), // Pastel light blue (top)
                        Color(0xFFEDE7F6)  // Light pastel lavender (bottom)
                    )
                )
            )
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { (route, icon, label) ->
                val isSelected = when {
                    route == "home" -> currentRoute == "home" || currentRoute.isBlank()
                    route == "tasks" -> currentRoute.startsWith("tasks")
                    route == "settings" -> currentRoute.startsWith("settings")
                    else -> currentRoute.startsWith(route)
                }
                val background by animateColorAsState(
                    targetValue = if (isSelected) Color(0xFFE3F2FD) else Color.Transparent, // Pastel light blue
                    animationSpec = tween(durationMillis = 250),
                    label = "bottom_nav_background"
                )
                val iconTint by animateColorAsState(
                    targetValue = if (isSelected) Color(0xFF1976D2) else DesignTokens.MutedForeground, // Blue for selected
                    animationSpec = tween(durationMillis = 300),
                    label = "bottom_nav_icon"
                )
                val labelTint by animateColorAsState(
                    targetValue = if (isSelected) Color(0xFF1976D2) else DesignTokens.MutedForeground, // Blue for selected
                    animationSpec = tween(durationMillis = 300),
                    label = "bottom_nav_label"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(background)
                        .selectable(
                            selected = isSelected,
                            onClick = { onTabSelected(route) },
                            role = Role.Tab
                        )
                        .animateContentSize()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Icon with highlight bubble when selected
                    Box(contentAlignment = Alignment.Center) {
                        if (isSelected) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFBBDEFB), // Lighter pastel blue for icon background
                                tonalElevation = 0.dp,
                                modifier = Modifier.size(28.dp)
                            ) {}
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = iconTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = label,
                        color = labelTint,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        fontFamily = StudyPlanFontFamily
                    )
                }
            }
        }
    }
}
