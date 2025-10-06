package com.mtlc.studyplan.social.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@Composable
fun SocialSegmentedTabs(
    selected: SocialTab,
    onTabSelected: (SocialTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val tabFontSize = when {
        screenWidthDp < 340 -> 11.sp
        screenWidthDp < 380 -> 12.sp
        screenWidthDp < 420 -> 13.sp
        else -> 14.sp
    }
    val tabHorizontalPadding = if (screenWidthDp < 360) 4.dp else 8.dp
    val tabVerticalPadding = if (screenWidthDp < 360) 8.dp else 10.dp

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SOCIAL_TABS.forEach { tab ->
                val isSelected = tab == selected
                val background by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    animationSpec = tween(200),
                    label = "segmented_background"
                )
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    animationSpec = tween(200),
                    label = "segmented_content"
                )

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .selectable(
                            selected = isSelected,
                            onClick = { onTabSelected(tab) },
                            role = Role.Tab
                        ),
                    shape = RoundedCornerShape(22.dp),
                    color = background,
                    contentColor = contentColor
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = tabHorizontalPadding, vertical = tabVerticalPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(id = tab.labelRes),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = tabFontSize),
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
