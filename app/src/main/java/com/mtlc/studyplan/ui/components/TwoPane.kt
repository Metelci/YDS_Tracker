package com.mtlc.studyplan.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mtlc.studyplan.ui.theme.LocalSpacing
import androidx.compose.foundation.layout.BoxWithConstraints

@Composable
fun TwoPaneScaffold(
    modifier: Modifier = Modifier,
    list: @Composable ColumnScope.() -> Unit,
    detail: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val isLarge = maxWidth >= 600.dp
        if (isLarge) {
            val s = LocalSpacing.current
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.weight(0.42f).fillMaxHeight().padding(s.xs), content = list)
                Column(Modifier.weight(0.58f).fillMaxHeight().padding(s.xs), content = detail)
            }
        } else {
            val s = LocalSpacing.current
            Column(Modifier.fillMaxSize().padding(s.xs)) {
                list()
                Spacer(Modifier.height(s.xs))
                detail()
            }
        }
    }
}
