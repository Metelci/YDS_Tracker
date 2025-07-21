// app/src/main/java/com/mtlc/studyplan/ui/theme/Shape.kt

package com.mtlc.studyplan.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp), // Card'lar için bu boyutu kullanabilirsiniz
    extraLarge = RoundedCornerShape(24.dp)
)