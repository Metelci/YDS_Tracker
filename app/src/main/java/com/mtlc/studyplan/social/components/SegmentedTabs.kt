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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mtlc.studyplan.social.SOCIAL_TABS
import com.mtlc.studyplan.social.SocialTab
import com.mtlc.studyplan.ui.theme.DesignTokens

@Composable
fun SocialSegmentedTabs(
    selected: SocialTab,
    onTabSelected: (SocialTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = DesignTokens.Surface,
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
                    targetValue = if (isSelected) DesignTokens.PrimaryContainer else Color.Transparent,
                    animationSpec = tween(200),
                    label = "segmented_background"
                )
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) DesignTokens.PrimaryContainerForeground else MaterialTheme.colorScheme.onSurface,
                    animationSpec = tween(200),
                    label = "segmented_content"
                )

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
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
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(id = tab.labelRes),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
