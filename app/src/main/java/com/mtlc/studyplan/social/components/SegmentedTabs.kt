package com.mtlc.studyplan.social.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtlc.studyplan.social.SOCIAL_TABS
import com.mtlc.studyplan.social.SocialTab
import com.mtlc.studyplan.ui.responsive.responsiveHeights

@Composable
fun SocialSegmentedTabs(
    selected: SocialTab,
    onTabSelected: (SocialTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val heights = responsiveHeights()
    val tabs = SOCIAL_TABS
    val selectedIndex = tabs.indexOf(selected).let { if (it >= 0) it else 0 }
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val tabFontSize = when {
        screenWidthDp < 340 -> 11.sp
        screenWidthDp < 380 -> 12.sp
        screenWidthDp < 420 -> 13.sp
        else -> 14.sp
    }

    // Match the Tasks page pill style: soft surface with inner capsule indicator
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        tonalElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        BoxWithConstraints(modifier = Modifier.padding(6.dp)) {
            val count = tabs.size.coerceAtLeast(1)
            val segmentWidth = maxWidth / count
            val animatedOffset by animateDpAsState(
                targetValue = segmentWidth * selectedIndex,
                animationSpec = tween(durationMillis = 250),
                label = "social_tab_offset"
            )

            // Active indicator
            Surface(
                modifier = Modifier
                    .offset(x = animatedOffset)
                    .width(segmentWidth)
                    .height(heights.button)
                    .clip(RoundedCornerShape(20.dp)),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                tonalElevation = 0.dp
            ) {}

            Row(
                modifier = Modifier.height(heights.button),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = index == selectedIndex
                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(200),
                        label = "social_tab_text_$index"
                    )

                    Box(
                        modifier = Modifier
                            .width(segmentWidth)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(20.dp))
                            .selectable(
                                selected = isSelected,
                                onClick = { onTabSelected(tab) },
                                role = Role.Tab
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(id = tab.labelRes),
                            color = contentColor,
                            fontSize = tabFontSize,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
