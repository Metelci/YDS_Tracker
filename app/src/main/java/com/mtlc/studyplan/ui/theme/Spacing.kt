package com.mtlc.studyplan.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class Spacing(
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val smPlus: Dp = 14.dp,
    val smAlt: Dp = 15.dp,
    val md: Dp = 16.dp,
    val mdPlus: Dp = 18.dp,
    val lgMinus: Dp = 20.dp,
    val lgAlt: Dp = 22.dp,
    val lg: Dp = 24.dp,
    val xlMinus: Dp = 28.dp,
    val xl: Dp = 32.dp
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }
