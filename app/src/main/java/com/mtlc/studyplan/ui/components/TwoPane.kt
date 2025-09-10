package com.mtlc.studyplan.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.weight(0.42f).fillMaxHeight().padding(8.dp), content = list)
                Column(Modifier.weight(0.58f).fillMaxHeight().padding(8.dp), content = detail)
            }
        } else {
            Column(Modifier.fillMaxSize().padding(8.dp)) {
                list()
                Spacer(Modifier.height(8.dp))
                detail()
            }
        }
    }
}

