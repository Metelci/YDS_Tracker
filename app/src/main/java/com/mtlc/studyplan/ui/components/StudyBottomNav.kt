package com.mtlc.studyplan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    val navShape = RoundedCornerShape(32.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = navShape,
            color = Color.Transparent,
            tonalElevation = 8.dp,
            shadowElevation = 20.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(navShape)
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)), navShape)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFE3F2FD).copy(alpha = 0.9f),
                                Color(0xFFEDE7F6).copy(alpha = 0.9f)
                            )
                        )
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
            .navigationBarsPadding()
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

                        val background = if (isSelected) Color(0xFFE3F2FD) else Color.Transparent
                        val iconTint = if (isSelected) Color(0xFF1976D2) else DesignTokens.MutedForeground
                        val labelTint = if (isSelected) Color(0xFF1976D2) else DesignTokens.MutedForeground

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
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isSelected) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFBBDEFB),
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
    }
}












