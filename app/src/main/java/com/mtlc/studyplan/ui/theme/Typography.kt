package com.mtlc.studyplan.ui.theme // Paket adını kendi projenize göre güncelleyin

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Material 3 için standart tipografi setini tanımlıyoruz.
// Bu yapı, 'Typography' isminin çakışmasını engeller.
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
    /* Diğer metin stillerini (titleLarge, bodyMedium vb.)
       burada özelleştirebilirsiniz. */
)
